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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import gate.AnnotationSet;
import gate.Annotation;
import gate.Controller;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.learningframework.engines.AlgorithmKind;
import gate.plugin.learningframework.engines.Engine;
import gate.util.GateRuntimeException;

/**
 * <p>
 * Training, evaluation and application of ML in GATE.</p>
 */
@CreoleResource(name = "LF_ApplyClassification",
        helpURL = "",
        comment = "Apply a trained machine learning model for classification")
public class LF_ApplyClassification extends LearningFrameworkPRBase {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  static final Logger logger = Logger.getLogger(LF_ApplyClassification.class.getCanonicalName());

  protected String outputASName;

  @RunTime
  @Optional
  @CreoleParameter(defaultValue = "LearningFramework")
  public void setOutputASName(String oasn) {
    this.outputASName = oasn;
  }

  public String getOutputASName() {
    return this.outputASName;
  }

  /**
   * The confidence threshold for applying an annotation. In the case of NER, the confidence
   * threshold is applied to the average for the entire entity.
   *
   */
  private Double confidenceThreshold;

  @RunTime
  @CreoleParameter(defaultValue = "0.0", comment = "The minimum "
          + "confidence/probability for including "
          + "an annotation at application time. In the case of NER, the confidence "
          + "threshold is applied to the average for the entire entity.")
  public void setConfidenceThreshold(Double confidenceThreshold) {
    this.confidenceThreshold = confidenceThreshold;
  }

  public Double getConfidenceThreshold() {
    return this.confidenceThreshold;
  }

  protected String targetFeature;

  // TODO: we want to get rid of this and read this name from the info file!!
  @RunTime
  @Optional
  @CreoleParameter(comment = "Name of class feature to add to the original "
          + "instance annotations",
          defaultValue = "")
  public void setTargetFeature(String name) {
    targetFeature = name;
  }

  public String getTargetFeature() {
    return targetFeature;
  }
  
  String sequenceSpan;
  
  @RunTime
  @Optional
  @CreoleParameter(comment = "For sequence learners, an annotation type "
          + "defining a meaningful sequence span. Ignored by non-sequence "
          + "learners. Needs to be in the input AS.")
  public void setSequenceSpan(String seq) {
    sequenceSpan = seq;
  }

  public String getSequenceSpan() {
    return sequenceSpan;
  }
  
  

////////////////////////////////////////////////////////////////////////////

  private Engine engine;

  private File savedModelDirectoryFile;


  @Override
  public void execute(Document doc) {
    // extract the required annotation sets,
    AnnotationSet inputAS = doc.getAnnotations(getInputASName());
    AnnotationSet instanceAS = inputAS.get(getInstanceType());
    // the classAS 
    // the sequenceAS must be specified for a sequence tagging algorithm and most not be specified
    // for a non-sequence tagging algorithm!
    AnnotationSet sequenceAS = null;
    if(engine.getAlgorithmKind() == AlgorithmKind.SEQUENCE_TAGGER) {
      // NOTE: we already have checked earlier, that in that case, the sequenceSpan parameter is 
      // given!
      sequenceAS = doc.getAnnotations(getSequenceSpan());
    }

    List<GateClassification> gcs = engine.classify(
          instanceAS, inputAS,
          sequenceAS, getAlgorithmParameters());

    GateClassification.applyClassification(doc, gcs, getTargetFeature(), null);
  }


  @Override
  public void afterLastDocument(Controller arg0, Throwable throwable) {
    // No need to do anything, empty implementation!
  }

  public void finishedNoDocument(Controller arg0, Throwable throwable) {
    // no need to do anything
  }

  @Override
  protected void beforeFirstDocument(Controller controller) {

    // JP: this was moved from the dataDirectory setter to avoid problems
    // but we should really make sure that the learning is reloaded only 
    // if the URL has changed since the last time (if ever) it was loaded.
    savedModelDirectoryFile = new File(
            gate.util.Files.fileFromURL(dataDirectory), Globals.savedModelDirectory);

    // Restore the Engine
    engine = Engine.loadEngine(savedModelDirectoryFile, getAlgorithmParameters());
    System.out.println("LF-Info: model loaded is now "+engine);

    if (engine.getModel() == null) {
      throw new GateRuntimeException("Do not have a model, something went wrong.");
    } else {
      System.out.println("LearningFramework: Applying model "
              + engine.getModel().getClass() + " ...");
    }
    // TODO: IMPORTANT: the restored engine should be able to tell 
    // what is the target feature name?
    // is it indeed a classification model?
  }

}
