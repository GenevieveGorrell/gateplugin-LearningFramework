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

import gate.learningframework.classification.GateClassification;
import gate.learningframework.classification.EngineLibSVM;
import gate.learningframework.classification.EngineMalletSeq;
import gate.learningframework.classification.EngineWeka;
import gate.learningframework.classification.Engine;
import gate.learningframework.classification.EngineMallet;
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

  protected String outClassFeature;

  // TODO: we want to get rid of this and read this name from the info file!!
  @RunTime
  @Optional
  @CreoleParameter(comment = "Name of class feature to add to the original "
          + "instance annotations, if empty new annotation is created.",
          defaultValue = "")
  public void setOutClassFeature(String name) {
    outClassFeature = name;
  }

  public String getOutClassFeature() {
    return outClassFeature;
  }

////////////////////////////////////////////////////////////////////////////
  private final String sequenceSpan = null;

  private Engine applicationLearner;

  private File savedModelDirectoryFile;

  private final Mode mode = Mode.CLASSIFICATION;

  @Override
  public void execute(Document doc) {

    if (applicationLearner != null) {
      List<GateClassification> gcs = null;

      // TODO: (JP) this should really check the actual type of the learner,
      // rather than what kind of learning is currently set as a parameter,
      // because the learner we read from the savedModel directory could
      // be entirely different. Also, we could then inform about the actual
      // learning class used.
      // At the moment, if an unknown model was loaded from a directory,
      // the whatIsIt will return null, so we handle this separately.
      if (applicationLearner.whatIsIt() == null) {
        if (applicationLearner instanceof EngineWeka) {
          gcs = ((EngineWeka) applicationLearner).classify(this.instanceType, this.inputASName, doc);
        } else if (applicationLearner instanceof EngineMallet
                && ((EngineMallet) applicationLearner).getMode() == Mode.CLASSIFICATION) {
          gcs = ((EngineMallet) applicationLearner).classify(this.instanceType, this.inputASName, doc);
        } else if (applicationLearner instanceof EngineMallet
                && ((EngineMallet) applicationLearner).getMode() == Mode.NAMED_ENTITY_RECOGNITION) {
          gcs = ((EngineMalletSeq) applicationLearner).classify(this.instanceType, this.inputASName, doc, this.sequenceSpan);
        } else {
          throw new GateRuntimeException("Found a strange instance of an engine");
        }
      } else {
        switch (applicationLearner.whatIsIt()) {
          case LIBSVM:
            gcs = ((EngineLibSVM) applicationLearner).classify(this.instanceType, this.inputASName, doc);
            break;
          case MALLET_CL_C45:
          case MALLET_CL_DECISION_TREE:
          case MALLET_CL_MAX_ENT:
          case MALLET_CL_NAIVE_BAYES_EM:
          case MALLET_CL_NAIVE_BAYES:
          case MALLET_CL_WINNOW:
            gcs = ((EngineMallet) applicationLearner).classify(this.instanceType, this.inputASName, doc);
            break;
          case MALLET_SEQ_CRF:
            gcs = ((EngineMalletSeq) applicationLearner).classify(this.instanceType, this.inputASName, doc, this.sequenceSpan);
            break;
          case WEKA_CL_NUM_ADDITIVE_REGRESSION:
          case WEKA_CL_NAIVE_BAYES:
          case WEKA_CL_J48:
          case WEKA_CL_JRIP:
          case WEKA_CL_RANDOM_TREE:
          case WEKA_CL_MULTILAYER_PERCEPTRON:
          case WEKA_CL_IBK:
          case WEKA_CL_LOGISTIC_REGRESSION:
          case WEKA_CL_RANDOM_FOREST:
            gcs = ((EngineWeka) applicationLearner).classify(this.instanceType, this.inputASName, doc);
            break;
        }
      }

      addClassificationAnnotations(doc, gcs);
    }
  }

  /*
	 * Having received a list of GateClassifications from the learner, we
	 * then write them onto the document if they pass the confidence threshold.
	 * If we are doing NER, we don't apply the confidence threshold.
   */
  private void addClassificationAnnotations(Document doc, List<GateClassification> gcs) {

    Iterator<GateClassification> gcit = gcs.iterator();

    AnnotationSet outputAnnSet = doc.getAnnotations(this.outputASName);

    while (gcit.hasNext()) {
      GateClassification gc = gcit.next();

      //We have a valid classification. Now write it onto the document.
      // If this is classification and the add feature value is set,
      // do not create a new annotation and instead just add features
      // to the instance annotation
      // TODO: this can be refactored to be more concise!
      if (mode == Mode.CLASSIFICATION && getOutClassFeature() != null
              && !getOutClassFeature().isEmpty()) {
        Annotation instance = gc.getInstance();
        FeatureMap fm = instance.getFeatures();
        // Instead of the predefined output class feature name use the one specified
        // as a PR parameter
        //
        // fm.put(outputClassFeature, gc.getClassAssigned());
        fm.put(getOutClassFeature(), gc.getClassAssigned());
        fm.put(Globals.outputProbFeature, gc.getConfidenceScore());
        if (gc.getClassList() != null && gc.getConfidenceList() != null) {
          fm.put(Globals.outputClassFeature + "_list", gc.getClassList());
          fm.put(Globals.outputProbFeature + "_list", gc.getConfidenceList());
        }
      } else {
        FeatureMap fm = Factory.newFeatureMap();
        fm.putAll(gc.getInstance().getFeatures());
        fm.put(Globals.outputClassFeature, gc.getClassAssigned());
        fm.put(Globals.outputProbFeature, gc.getConfidenceScore());
        if (gc.getClassList() != null && gc.getConfidenceList() != null) {
          fm.put(Globals.outputClassFeature + "_list", gc.getClassList());
          fm.put(Globals.outputProbFeature + "_list", gc.getConfidenceList());
        }
        //fm.put(this.conf.getIdentifier(), identifier);
        if (gc.getSeqSpanID() != null) {
          System.err.println("Refactoring error: why do we have a SeqSpanID when doing classification?");
        }
        outputAnnSet.add(gc.getInstance().getStartNode(),
                gc.getInstance().getEndNode(),
                gc.getInstance().getType(), fm);
      } // else if CLASSIFICATION and have out class feature

    }
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

    applicationLearner = Engine.restoreLearner(savedModelDirectoryFile);
    //System.out.println("LF-Info: model loaded is now "+applicationLearner);

    if (this.applicationLearner == null) {
      throw new GateRuntimeException("Do not have a model, something went wrong.");
    } else {
      System.out.println("LearningFramework: Applying model "
              + applicationLearner.whatIsItString() + " ...");
      if (applicationLearner.getPipe() == null) {
        System.out.println("Model classes: UNKNOWN, no pipe");
      } else {
        System.out.println("Model classes: "
                + applicationLearner.getPipe().getTargetAlphabet().toString().replaceAll("\\n", " "));
      }

      if (applicationLearner.getMode() != mode) {
        logger.warn("LearningFramework: Warning! Applying "
                + "model trained in " + applicationLearner.getMode()
                + " mode in " + mode + " mode!");
      }
    }
  }

}
