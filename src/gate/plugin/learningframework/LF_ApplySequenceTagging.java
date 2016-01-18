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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import gate.util.InvalidOffsetException;

/**
 * <p>
 * Training, evaluation and application of ML in GATE.</p>
 */
@CreoleResource(name = "LF_ApplyClassification", 
        helpURL = "",
        comment = "Apply a trained machine learning model for classification")
public class LF_ApplySequenceTagging extends LearningFrameworkPRBase  {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  static final Logger logger = Logger.getLogger(LF_ApplySequenceTagging.class.getCanonicalName());

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
  
  private Mode mode = Mode.NAMED_ENTITY_RECOGNITION;
  
  
////////////////////////////////////////////////////////////////////////////

  
  private Engine applicationLearner;

  private File savedModelDirectoryFile;
  

  //In the case of NER, output instance annotations to temporary
  //AS, to keep them separate.
  private static final String tempOutputASName = "tmp_ouputas_for_ner12345";

  private String outClassFeature = null;

  @Override
  public void execute(Document doc)  {


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
          if (mode == Mode.NAMED_ENTITY_RECOGNITION) {
            //We need to make the surrounding annotations
            addSurroundingAnnotations(doc);
          }
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
    //Unless we are doing NER, in which case we want to use the temp
    if (mode == Mode.NAMED_ENTITY_RECOGNITION) {
      outputAnnSet = doc.getAnnotations(tempOutputASName);
    }

    while (gcit.hasNext()) {
      GateClassification gc = gcit.next();

      // JP: TODO: need to check if we always get the correct confidence
      // score here and if the default makes this do what is expected!
      if (mode == Mode.CLASSIFICATION
              && gc.getConfidenceScore() < this.getConfidenceThreshold()) {
        //Skip it
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
          fm.put(Globals.outputSequenceSpanIDFeature, gc.getSeqSpanID());
        }
        outputAnnSet.add(gc.getInstance().getStartNode(),
                gc.getInstance().getEndNode(),
                gc.getInstance().getType(), fm);
      } 
    }
  }

  /*
	 * In the case of NER, we replace the instance annotations with
	 * spanning annotations for the desired entity type. We apply a confidence
	 * threshold to the average for the whole entity. We write the average
	 * confidence for the entity onto the entity.
   */
  private void addSurroundingAnnotations(Document doc) {
    AnnotationSet fromset = doc.getAnnotations(tempOutputASName);
    AnnotationSet toset = doc.getAnnotations(this.outputASName);
    List<Annotation> insts = fromset.get(this.instanceType).inDocumentOrder();

    class AnnToAdd {

      long thisStart = -1;
      long thisEnd = -1;
      int len = 0;
      double conf = 0.0;
    }

    Map<Integer, AnnToAdd> annsToAdd = new HashMap<Integer, AnnToAdd>();

    Iterator<Annotation> it = insts.iterator();
    while (it.hasNext()) {
      Annotation inst = it.next();

      //Do we have an annotation in progress for this sequence span ID?
      //If we didn't use sequence learning, just use the same ID repeatedly.
      Integer sequenceSpanID = (Integer) inst.getFeatures().get(Globals.outputSequenceSpanIDFeature);
      if (sequenceSpanID == null) {
        sequenceSpanID = 0;
      }
      AnnToAdd thisAnnToAdd = annsToAdd.get(sequenceSpanID);

      //B, I or O??
      String status = (String) inst.getFeatures().get(Globals.outputClassFeature);
      if (status == null) {
        status = "outside";
      }

      if (thisAnnToAdd != null && (status.equals("beginning") || status.equals("outside"))) {
        //If we've found a beginning or an end, this indicates that a current
        //incomplete annotation is now completed. We should write it on and
        //remove it from the map.
        double entityconf = thisAnnToAdd.conf / thisAnnToAdd.len;

        if (thisAnnToAdd.thisStart != -1 && thisAnnToAdd.thisEnd != -1
                && entityconf >= this.getConfidenceThreshold()) {
          FeatureMap fm = Factory.newFeatureMap();
          fm.put(Globals.outputProbFeature, entityconf);
          if (sequenceSpanID != null) {
            fm.put(Globals.outputSequenceSpanIDFeature, sequenceSpanID);
          }
          try {
            toset.add(
                    thisAnnToAdd.thisStart, thisAnnToAdd.thisEnd,
                    this.getClassType(), fm);
          } catch (InvalidOffsetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        annsToAdd.remove(sequenceSpanID);
      }

      if (status.equals("beginning")) {
        AnnToAdd ata = new AnnToAdd();
        ata.thisStart = inst.getStartNode().getOffset();
        //Update the end on the offchance that this is it
        ata.thisEnd = inst.getEndNode().getOffset();
        ata.conf = (Double) inst.getFeatures().get(Globals.outputProbFeature);
        ata.len++;
        annsToAdd.put(sequenceSpanID, ata);
      }

      if (status.equals("inside") && thisAnnToAdd != null) {
        thisAnnToAdd.conf += (Double) inst.getFeatures().get(Globals.outputProbFeature);
        thisAnnToAdd.len++;
        //Update the end on the offchance that this is it
        thisAnnToAdd.thisEnd = inst.getEndNode().getOffset();
      }

      //Remove each inst ann as we consume it
      fromset.remove(inst);
    }

    //Add any hanging entities at the end.
    Iterator<Integer> atait = annsToAdd.keySet().iterator();
    while (atait.hasNext()) {
      Integer sequenceSpanID = (Integer) atait.next();
      AnnToAdd thisAnnToAdd = annsToAdd.get(sequenceSpanID);
      double entityconf = thisAnnToAdd.conf / thisAnnToAdd.len;

      if (thisAnnToAdd.thisStart != -1 && thisAnnToAdd.thisEnd != -1
              && entityconf >= this.getConfidenceThreshold()) {
        FeatureMap fm = Factory.newFeatureMap();
        fm.put(Globals.outputProbFeature, entityconf);
        if (sequenceSpanID != null) {
          fm.put(Globals.outputSequenceSpanIDFeature, sequenceSpanID);
        }
        try {
          toset.add(
                  thisAnnToAdd.thisStart, thisAnnToAdd.thisEnd,
                  this.getClassType(), fm);
        } catch (InvalidOffsetException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    doc.removeAnnotationSet(tempOutputASName);
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
