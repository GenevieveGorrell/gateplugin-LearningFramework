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

package gate.plugin.learningframework.corpora;

import gate.Annotation;
import gate.AnnotationSet;
import java.util.List;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.mallet.LFPipe;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * This represents a corpus in Mallet format where we have a single feature vector and single target.
 * This corpus may be created for classification, regression or sequence tagging, but the 
 * representation is always a pair (featurevector,target). In the case of sequence tagging, the
 * corpus is created for the use of normal classification algorithms that will be used to 
 * predict the begin/inside/outside classifications. 
 * 
 * @author johann
 */
public class CorpusRepresentationMallet extends CorpusRepresentation {

  static final Logger logger = Logger.getLogger("CorpusRepresentationMallet");
  
      protected LFPipe pipe;
      protected InstanceList instances;
      
	public CorpusRepresentationMallet(FeatureInfo fi, ScalingMethod sm){
          featureInfo = fi;
          scalingMethod = sm;
          
          Pipe innerPipe = new Noop(new Alphabet(), new LabelAlphabet());
          List<Pipe> pipes = new ArrayList<Pipe>();
          pipes.add(innerPipe);
          pipe = new LFPipe(pipes);
          pipe.setFeatureInfo(fi);
          instances = new InstanceList(pipe);
	}

        /**
         * Extract 
         * @param doc
         * @param featureInfo
         * @param pipe
         * @return 
         */
  public static Instance extractInstance(Annotation instanceAnnotation, String targetFeatureName,
          FeatureInfo featureInfo, Pipe pipe, 
          AnnotationSet inputAS, String nameFeature) {
    Instance inst = new Instance(null,null,null,null);
    return inst;
  }
    
  /**
   * Add instances for regression or classification tasks.
   * @param instancesAS
   * @param inputAS
   * @param nameFeatureName 
   */
  public void add(AnnotationSet instancesAS, String targetFeatureName, AnnotationSet inputAS, String nameFeatureName) {
    List<Annotation> instanceAnnotations = instancesAS.inDocumentOrder();
    for(Annotation instanceAnnotation : instanceAnnotations) {
      Instance inst = extractInstance(instanceAnnotation, targetFeatureName, featureInfo, pipe, inputAS, nameFeatureName);
      instances.addThruPipe(inst);
    }
  }
  
  /** 
   * Add instances for sequence tagging task.
   */
  public void addSequences(AnnotationSet instancesAS, AnnotationSet inputAS, AnnotationSet classAnnotations,
          String nameFeatureName) {
    
  }
	
}
