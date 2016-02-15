/*
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright 2015 South London and Maudsley NHS Trust and King's College London
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 */
package gate.plugin.learningframework;

import gate.AnnotationSet;
import java.net.URL;

import org.apache.log4j.Logger;

import gate.Controller;
import gate.Document;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.learningframework.data.CorpusRepresentation;
import gate.plugin.learningframework.data.CorpusRepresentationMalletClass;
import gate.plugin.learningframework.engines.AlgorithmClassification;
import gate.plugin.learningframework.engines.Engine;
import gate.plugin.learningframework.features.FeatureSpecification;
import gate.plugin.learningframework.features.TargetType;
import gate.util.Files;
import gate.util.GateRuntimeException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 *
 */
@CreoleResource(
        name = "LF_TrainClassification",
        helpURL = "",
        comment = "Train a machine learning model for classification")
public class LF_TrainClassification extends LF_TrainBase {
  private static final long serialVersionUID = -420477191226830002L;
  

  private final Logger logger = Logger.getLogger(LF_TrainClassification.class.getCanonicalName());

  /**
   * The configuration file.
   *
   */
  private java.net.URL featureSpecURL;

  @RunTime
  @CreoleParameter(comment = "The feature specification file.")
  public void setFeatureSpecURL(URL featureSpecURL) {
    this.featureSpecURL = featureSpecURL;
  }

  public URL getFeatureSpecURL() {
    return featureSpecURL;
  }

  private AlgorithmClassification trainingAlgorithm;

  @RunTime
  @Optional
  @CreoleParameter(comment = "The algorithm to be used for training. Ignored at "
          + "application time.")
  public void setTrainingAlgorithm(AlgorithmClassification algo) {
    this.trainingAlgorithm = algo;
  }

  public AlgorithmClassification getTrainingAlgorithm() {
    return this.trainingAlgorithm;
  }
  
  protected String algorithmClass;
  @RunTime
  @Optional
  @CreoleParameter(comment = "The class of the training algorithm to use, only used if SPECIFY_CLASS is selected")
  public void setAlgorithmClass(String className) { 
    algorithmClass = className;
  }
  
  public String getAlgorithmClass() { return algorithmClass; }

  protected ScalingMethod scaleFeatures = ScalingMethod.NONE;

  @RunTime
  @CreoleParameter(defaultValue = "NONE", comment = "If and how to scale features. ")
  public void setScaleFeatures(ScalingMethod sf) {
    scaleFeatures = sf;
  }

  public ScalingMethod getScaleFeatures() {
    return scaleFeatures;
  }

  protected String targetFeature;

  @RunTime
  @Optional
  @CreoleParameter(comment = "For classification, the feature "
          + "containing the class. Ignored for NER, where type only is used.")
  public void setTargetFeature(String classFeature) {
    this.targetFeature = classFeature;
  }

  public String getTargetFeature() {
    return this.targetFeature;
  }

  private CorpusRepresentationMalletClass corpusRepresentation = null;
  private FeatureSpecification featureSpec = null;

  private Engine engine = null;

  protected String sequenceSpan;

  @RunTime
  @Optional
  @CreoleParameter(comment = "For sequence learners, an annotation type "
          + "defining a meaningful sequence span. Ignored by non-sequence "
          + "learners. Needs to be in the input AS.")
  public void setSequenceSpan(String seq) {
    this.sequenceSpan = seq;
  }

  public String getSequenceSpan() {
    return this.sequenceSpan;
  }
  
  
  @RunTime
  @CreoleParameter(comment = "If we want to train or just export the instances.")
  public void setTrainOrExportAction(TrainOrExportAction value) {
    this.trainOrExportAction = value;
  }

  public TrainOrExportAction getTrainOrExportAction() {
    return trainOrExportAction == null ? TrainOrExportAction.TRAIN : trainOrExportAction;
  }

  private TrainOrExportAction trainOrExportAction;
  
  

  @Override
  public void execute(Document doc) {
    // extract the required annotation sets,
    AnnotationSet inputAS = doc.getAnnotations(getInputASName());
    AnnotationSet instanceAS = inputAS.get(getInstanceType());
    // the classAS 
    // the sequenceAS must be specified for a sequence tagging algorithm and most not be specified
    // for a non-sequence tagging algorithm!
    AnnotationSet sequenceAS = null;
    if(getTrainingAlgorithm() == AlgorithmClassification.MALLET_SEQ_CRF) {
      // NOTE: we already have checked earlier, that in that case, the sequenceSpan parameter is 
      // given!
      sequenceAS = doc.getAnnotations(getSequenceSpan());
    }
    // the classAS is always null for the classification task!
    // the nameFeatureName is always null for now!
    String nameFeatureName = null;
    corpusRepresentation.add(instanceAS, sequenceAS, inputAS, null, getTargetFeature(), TargetType.NOMINAL, nameFeatureName);
  }

  @Override
  public void afterLastDocument(Controller arg0, Throwable t) {
    if(trainOrExportAction == TrainOrExportAction.TRAIN) {
      System.out.println("LearningFramework: Starting training engine "+engine);
      System.out.println("Training set classes: "+
           corpusRepresentation.getRepresentationMallet().getPipe().getTargetAlphabet().toString().replaceAll("\\n", " "));
      System.out.println("Training set size: " + corpusRepresentation.getRepresentationMallet().size());
      if (corpusRepresentation.getRepresentationMallet().getDataAlphabet().size() > 20) {
        System.out.println("LearningFramework: Attributes " + corpusRepresentation.getRepresentationMallet().getDataAlphabet().size());
      } else {
        System.out.println("LearningFramework: Attributes " + corpusRepresentation.getRepresentationMallet().getDataAlphabet().toString().replaceAll("\\n", " "));
      }
      //System.out.println("DEBUG: instances are "+corpusRepresentation.getRepresentationMallet());
      
      corpusRepresentation.addScaling(getScaleFeatures());
      engine.trainModel(getAlgorithmParameters());
      logger.info("LearningFramework: Training complete!");
      engine.saveEngine(Files.fileFromURL(getDataDirectory()));      
    } else {
      corpusRepresentation.addScaling(getScaleFeatures());
      File outDir = Files.fileFromURL(getDataDirectory());
      CorpusRepresentation.export(corpusRepresentation, trainOrExportAction, outDir, getAlgorithmParameters());
    }
    }

  @Override
  protected void finishedNoDocument(Controller c, Throwable t) {
    logger.error("Processing finished, but no documents seen, cannot train!");
  }

  @Override
  protected void beforeFirstDocument(Controller controller) {
    
    if(getTrainingAlgorithm() == AlgorithmClassification.MALLET_SEQ_CRF) {
      if(getSequenceSpan() == null || getSequenceSpan().isEmpty()) {
        throw new GateRuntimeException("SequenceSpan parameter is required for MALLET_SEQ_CRF");
      } 
    } else {
      if(getSequenceSpan() != null && !getSequenceSpan().isEmpty()) {
        throw new GateRuntimeException("SequenceSpan parameter must not be specified with non-sequence tagging algorithm");
      }
    }
    
    AlgorithmClassification alg = getTrainingAlgorithm();
    // if an algorithm is specified where the name ends in "SPECIFY_CLASS" use the 
    // algorithmClass 
    if(getTrainingAlgorithm().toString().endsWith("SPECIFY_CLASS")) {
      if(getAlgorithmClass() == null || getAlgorithmClass().isEmpty()) {
        throw new GateRuntimeException("AlgorithmClass parameter must be specified when "+getTrainingAlgorithm()+" is chosen");
      }
      Class clazz = null;
      try {
        clazz = Class.forName(getAlgorithmClass());
      } catch (ClassNotFoundException ex) {
        throw new GateRuntimeException("Could not load algorithm class: "+getAlgorithmClass(),ex);
      }
      alg.setTrainerClass(clazz);
    }
    
    System.err.println("DEBUG: Before Document.");
    System.err.println("  Training algorithm engine class is "+alg.getEngineClass());
    System.err.println("  Training algorithm algor class is "+alg.getTrainerClass());
    
    // Read and parse the feature specification
    featureSpec = new FeatureSpecification(featureSpecURL);
    System.err.println("DEBUG Read the feature specification: "+featureSpec);

    // create the corpus representation for creating the training instances
    corpusRepresentation = new CorpusRepresentationMalletClass(featureSpec.getFeatureInfo(), scaleFeatures);
    System.err.println("DEBUG: created the corpusRepresentationMallet: "+corpusRepresentation);

    // Create the engine from the Algorithm parameter
    engine = Engine.createEngine(trainingAlgorithm, getAlgorithmParameters(),corpusRepresentation);
    System.err.println("DEBUG: created the engine: "+engine);
    
    
    System.err.println("DEBUG: setup of the training PR complete");
  }

}
