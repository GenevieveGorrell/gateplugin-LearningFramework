/*
 * CorpusWriterMallet.java
 *  
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright 2015 South London and Maudsley NHS Trust and King's College London
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 *
 * Genevieve Gorrell, 9 Jan 2015
 */
package gate.plugin.learningframework.data;

import gate.Annotation;
import gate.AnnotationSet;
import java.util.List;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.mallet.FeatureVector2NormalizedFeatureVector;
import gate.plugin.learningframework.features.Attribute;
import gate.plugin.learningframework.features.FeatureExtraction;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.TargetType;
import gate.plugin.learningframework.mallet.LFPipe;
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * This represents a corpus in Mallet format where we have a single feature vector and single
 * target. This corpus may be created for classification, regression or sequence tagging, but the
 * representation is always a pair (featurevector,target). In the case of sequence tagging, the
 * corpus is created for the use of normal classification algorithms that will be used to predict
 * the begin/inside/outside classifications.
 *
 * @author johann
 */
public class CorpusRepresentationMallet extends CorpusRepresentation {

  static final Logger logger = Logger.getLogger("CorpusRepresentationMallet");

  protected InstanceList instances;

  public InstanceList getInstances() { return instances; }
  

  /**
   * Constructor for creating a new CorpusRepresentation from a FeatureInfo. 
   * @param fi
   * @param sm 
   */
  public CorpusRepresentationMallet(FeatureInfo fi, ScalingMethod sm) {
    featureInfo = fi;
    scalingMethod = sm;

    Pipe innerPipe = new Noop(new Alphabet(), new LabelAlphabet());
    List<Pipe> pipes = new ArrayList<Pipe>();
    pipes.add(innerPipe);
    pipe = new LFPipe(pipes);
    pipe.setFeatureInfo(fi);
    instances = new InstanceList(pipe);
  }
  
  // NOTE: at application time we do not explicitly create a CorpusRepresentatioMallet object.
  // Instead, the pipe gets saved with the model and can get retrieved from the loaded model 
  // later. The method extractIndependentFeatures is also used at application time to 
  // extract the Instances, using the Pipe that was stored with the model.
  // For non-Mallet algorithms we store the pipe separately and load it separately when the model
  // is loaded for application. The Pipe is then again used with extractIndependentFeatures 
  // to get the instances.

  /**
   * Extract the independent features for a single instance annotation.
   * Extract the independent features for a single annotation according to the information
   * in the featureInfo object. The information in the featureInfo instance gets updated 
   * by this. 
   * @param instanceAnnotation
   * @param inputAS
   * @param targetFeatureName
   * @param featureInfo
   * @param pipe
   * @param nameFeature
   * @return 
   */
  public static Instance extractIndependentFeatures(
          Annotation instanceAnnotation,
          AnnotationSet inputAS,
          String targetFeatureName,
          FeatureInfo featureInfo,
          Pipe pipe) {
    
    AugmentableFeatureVector afv = new AugmentableFeatureVector(pipe.getDataAlphabet());
    Instance inst = new Instance(afv, null, null, null);
    for(Attribute attr : featureInfo.getAttributes()) {
      FeatureExtraction.extractFeature(inst, attr, inputAS, instanceAnnotation);
    }
    // TODO: we destructively replace the AugmentableFeatureVector by a FeatureVector here,
    // but it is not clear if this is beneficial - our assumption is that yes.
    inst.setData(((AugmentableFeatureVector)inst.getData()).toFeatureVector());
    return inst;
  }

  /**
   * Add instances. The exact way of how the target is created to the instances depends on which
   * parameters are given and which are null. The parameter sequenceAS must always be null for this
   * corpus representation since this corpus representation is not usable for sequence tagging
   * algorithms If the parameter classAS is non-null then instances for a sequence tagging task are
   * created, in that case targetFeatureName must be null. If targetFeatureName is non-null then
   * instances for a regression or classification problem are created (depending on targetType) and
   * classAS must be null. if the parameter nameFeatureName is non-null, then a Mallet instance name
   * is added from the source document and annotation.
   *
   * @param instancesAS
   * @param inputAS
   * @param nameFeatureName
   */
  public void add(AnnotationSet instancesAS, AnnotationSet sequenceAS, AnnotationSet inputAS, AnnotationSet classAS, String targetFeatureName, TargetType targetType, String nameFeatureName) {
    if(sequenceAS != null) {
      throw new GateRuntimeException("LF invalid call to CorpusRepresentationMallet.add: sequenceAS must be null "+
              " for document "+inputAS.getDocument().getName());
    }
    List<Annotation> instanceAnnotations = instancesAS.inDocumentOrder();
    for (Annotation instanceAnnotation : instanceAnnotations) {
      Instance inst = extractIndependentFeatures(instanceAnnotation, inputAS, targetFeatureName, featureInfo, pipe);
      if (classAS != null) {
        // extract the target as required for sequence tagging
        FeatureExtraction.extractClassForSeqTagging(inst, pipe.getTargetAlphabet(), classAS, instanceAnnotation);
      } else {
        if(targetType == TargetType.NOMINAL) {
          FeatureExtraction.extractClassTarget(inst, pipe.getTargetAlphabet(), targetFeatureName, instanceAnnotation, inputAS);
        } else if(targetType == TargetType.NUMERIC) {
          FeatureExtraction.extractNumericTarget(inst, targetFeatureName, instanceAnnotation, inputAS);
        }
      }
      // if a nameFeature is specified, add the name informatin to the instance
      if(nameFeatureName != null) {
        FeatureExtraction.extractName(inst, instanceAnnotation, inputAS.getDocument());
      }
      if(!FeatureExtraction.ignoreInstanceWithMV(inst)) {
        instances.add(inst);
      }
    }
  }

  /**
   * Add scale features and add a pipe for that scaling to the end of the current SerialPipes.
   * If the ScalingMethod is NONE, this does nothing.
   * @param scaleFeatures 
   */
  public void addScaling(ScalingMethod scaleFeatures) {
    if(scaleFeatures == ScalingMethod.NONE) return;
    System.out.println("DEBUG normalize: getDataAlphabet=" + instances.getDataAlphabet());
    System.out.println("DEBUG normalize: size=" + instances.getDataAlphabet().size());
    double[] sums = new double[instances.getDataAlphabet().size()];
    double[] sumsofsquares = new double[instances.getDataAlphabet().size()];
    //double[] numvals = new double[instances.getDataAlphabet().size()];
    double[] means = new double[instances.getDataAlphabet().size()];
    double[] variances = new double[instances.getDataAlphabet().size()];

    for (int i = 0; i < instances.size(); i++) {
      FeatureVector data = (FeatureVector) instances.get(i).getData();
      int[] indices = data.getIndices();
      double[] values = data.getValues();
      for (int j = 0; j < indices.length; j++) {
        int index = indices[j];
        double value = values[j];
        sums[index] += value;
        sumsofsquares[index] += (value * value);
        //numvals[index]++;
      }
    }

    //Now use the accumulators to prepare means and variances
    //for each feature in the alphabet, to be used for scaling.
    for (int i = 0; i < sums.length; i++) {
      means[i] = sums[i] / instances.getDataAlphabet().size();
      variances[i] = sumsofsquares[i] / instances.getDataAlphabet().size();
    }

    //We make a new pipe and apply it to all the instances
    FeatureVector2NormalizedFeatureVector normalizer
            = new FeatureVector2NormalizedFeatureVector(means, variances, instances.getAlphabet());
    
    // Run all the instances through this pipe
    for(Instance inst : instances) {
      inst = normalizer.pipe(inst);
    }

    //Add the pipe to the pipes so application time data will go through it
    ArrayList<Pipe> pipeList = pipe.pipes();
    pipeList.add(normalizer);
    System.out.println("DEBUG normalize: added normalizer pipe " + normalizer);
    // pipe = new SerialPipes(pipeList);
    LFPipe pipe = (LFPipe)instances.getPipe();
    pipe.addPipe(normalizer);
    System.out.println("DEBUG pipes after normalization: " + pipe);
  }

}
