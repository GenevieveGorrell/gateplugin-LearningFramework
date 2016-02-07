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
import gate.learningframework.classification.Operation;
import java.io.File;
import java.net.URL;
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
import gate.Resource;
import gate.creole.ResourceInstantiationException;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.learningframework.corpora.CorpusWriter;
import gate.plugin.learningframework.corpora.CorpusWriterArff;
import gate.plugin.learningframework.corpora.CorpusWriterArffNumericClass;
import gate.plugin.learningframework.corpora.CorpusWriterMallet;
import gate.plugin.learningframework.corpora.CorpusWriterMalletSeq;
import gate.plugin.learningframework.corpora.FeatureSpecification;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import libsvm.svm_problem;

/**
 * Evaluate a machine learning algorithm on some corpus.
 */
@CreoleResource(name = "LearningFrameworkEvaluateTODO", comment = "Evaluate a machine learning approach")
public class LearningFrameworkEvaluate extends LearningFrameworkPRBase {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  static final Logger logger = Logger.getLogger(LearningFrameworkEvaluate.class.getCanonicalName());

  private java.net.URL featureSpecURL;

  /**
   * The name of the output annotation set.
   *
   */
  private String outputASName;

  /**
   * The annotation type to be treated as instance. Leave blank to use the document as instance.
   *
   */
  private String instanceName;

  /**
   * The annotation type defining the unit for sequence tagging.
   *
   */
  private String sequenceSpan;

  /**
   * The operation to be done; data prep, training, evaluation or application.
   *
   */
  private Operation operation;

  /**
   * The number of folds for cross-validation.
   *
   */
  private int foldsForXVal;

  /**
   * The proportion of training data for holdout evaluation.
   *
   */
  private float trainingproportion;

  /**
   * The implementation to be used, such as Mallet.
   *
   */
  private Algorithm trainingAlgo;

  /**
   * Whether to do classification or named entity recognition.
   *
   */
  private Mode mode;

  /**
   * Annotation type containing/indicating the class.
   *
   */
  private String classType;

  /**
   * Annotation feature containing the class. Ignored for NER.
   *
   */
  private String classFeature;

  /**
   * The feature of the instance that can be used as an identifier for that instance.
   *
   */
  private String identifierFeature;

  /**
   * The confidence threshold for applying an annotation. In the case of NER, the confidence
   * threshold is applied to the average for the entire entity.
   *
   */
  private Double confidenceThreshold;

  /**
   * Some of the learners take parameters. Parameters can be entered here. For example, the LibSVM
   * supports parameters.
   */
  private String learnerParams;

  /**
   * A flag that indicates that the PR has just been started. Used in execute() to run code that
   * needs to run once before any documents are processed.
   */
  protected boolean justStarted = false;

  /**
   * A flag that indicates that at least one document was processed.
   */
  protected boolean haveSomeDocuments = false;

  @RunTime
  @CreoleParameter(comment = "The feature specification file.")
  public void setFeatureSpecURL(URL featureSpecURL) {
    if (!featureSpecURL.equals(this.featureSpecURL)) {
      this.featureSpecURL = featureSpecURL;
      this.conf = new FeatureSpecification(featureSpecURL);
    }
  }

  public URL getFeatureSpecURL() {
    return featureSpecURL;
  }

  @RunTime
  @CreoleParameter(comment = "The directory to which data will be saved, including models and corpora.")
  public void setDataDirectory(URL output) {
    //System.out.println("LF DEBUG: setting dataDirectory to "+output);
    this.dataDirectory = output;
  }

  public URL getDataDirectory() {
    return this.dataDirectory;
  }

  @RunTime
  @Optional
  @CreoleParameter
  public void setInputASName(String iasn) {
    this.inputASName = iasn;
  }

  public String getInputASName() {
    return this.inputASName;
  }

  @RunTime
  @Optional
  @CreoleParameter(defaultValue = "LearningFramework")
  public void setOutputASName(String oasn) {
    this.outputASName = oasn;
  }

  public String getOutputASName() {
    return this.outputASName;
  }

  @RunTime
  @Optional
  @CreoleParameter(defaultValue = "3", comment = "The number of folds for "
          + "cross-validation.")
  public void setFoldsForXVal(Integer folds) {
    this.foldsForXVal = folds.intValue();
  }

  public Integer getFoldsForXVal() {
    return new Integer(this.foldsForXVal);
  }

  @RunTime
  @Optional
  @CreoleParameter(defaultValue = "0.5", comment = "The proportion of the "
          + "data to use for training in holdout evaluation.")
  public void setTrainingProportion(Float trainingproportion) {
    this.trainingproportion = trainingproportion.floatValue();
  }

  public Float getTrainingProportion() {
    return new Float(this.trainingproportion);
  }

  @RunTime
  @CreoleParameter(defaultValue = "Token", comment = "The annotation type to "
          + "be treated as instance.")
  public void setInstanceType(String inst) {
    this.instanceName = inst;
  }

  public String getInstanceType() {
    return this.instanceName;
  }

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
  @CreoleParameter(defaultValue = "TRAIN", comment = "The operation to be "
          + "done; training, evaluation or application.")
  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public Operation getOperation() {
    return this.operation;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "The algorithm to be used for training. Ignored at "
          + "application time.")
  public void setTrainingAlgo(Algorithm algo) {
    this.trainingAlgo = algo;
  }

  public Algorithm getTrainingAlgo() {
    return this.trainingAlgo;
  }

  @RunTime
  @CreoleParameter(defaultValue = "CLASSIFICATION", comment = "Whether to do "
          + "classification or named entity recognition.")
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public Mode getMode() {
    return this.mode;
  }

  @RunTime
  @CreoleParameter(comment = "Annotation type containing/indicating the class.")
  public void setClassType(String classType) {
    this.classType = classType;
  }

  public String getClassType() {
    return this.classType;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "For classification, the feature "
          + "containing the class. Ignored for NER, where type only is used.")
  public void setTargetFeature(String classFeature) {
    this.classFeature = classFeature;
  }

  public String getTargetFeature() {
    return this.classFeature;
  }

  @RunTime
  @Optional
  @CreoleParameter(comment = "The feature of the instance that "
          + "can be used as an identifier for that instance.")
  public void setIdentifierFeature(String identifierFeature) {
    this.identifierFeature = identifierFeature;
  }

  public String getIdentifierFeature() {
    return this.identifierFeature;
  }

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

  @RunTime
  @Optional
  @CreoleParameter(comment = "Some of the learners take parameters. Parameters "
          + "can be entered here. For example, the LibSVM supports parameters.")
  public void setAlgorithmParameters(String learnerParams) {
    this.learnerParams = learnerParams;
  }

  public String getAlgorithmParameters() {
    return this.learnerParams;
  }

  @RunTime
  @CreoleParameter(defaultValue = "NONE", comment = "If and how to scale features. ")
  public void setScaleFeatures(ScalingMethod sf) {
    scaleFeatures = sf;
  }

  public ScalingMethod getScaleFeatures() {
    return scaleFeatures;
  }
  protected ScalingMethod scaleFeatures = ScalingMethod.NONE;

  // TODO: eventually, maybe LF_class should be the default for this,
  // but for now we make empty the default so that the existing 
  // pipelines work unchanged. Since it is optional, the parameter
  // does not need to exist in the pipeline either.
  private String outClassFeature;

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

  //Loaded from save directory, replaced with training learner 
  //after training completes.
  private Engine applicationLearner;

  //Used at training time.
  private Engine trainingLearner;

  //Separate learner for evaluation, not to be mixed up with the others
  private Engine evaluationLearner;

  //These corpora will be added to on each document so they need to be globals
  private CorpusWriter trainingCorpus = null;
  private CorpusWriter testCorpus = null;
  private CorpusWriter exportCorpus = null;

  private FeatureSpecification conf = null;

  //Some file names, mostly not used at the mo since the corpora don't need
  //to be written out. The arff one gets used.
  private static String trainfilenamemallet = "train.mallet";
  private static String testfilenamemallet = "test.mallet";
  private static String trainfilenamemalletseq = "train.seq.mallet";
  private static String testfilenamemalletseq = "test.seq.mallet";
  private static String trainfilenamearff = "train.arff";
  private static String testfilenamearff = "test.arff";
  private static String corpusoutputdirectory = "exportedCorpora";

  //Some directory names. The evaluation one doesn't get used at the mo.
  private static String savedModelDirectory = "savedModel";
  private static String evaluationModelDirectory = "evaluationModel";

  private File savedModelDirectoryFile;
  private File evaluationModelDirectoryFile;

  private static String outputClassFeature = "LF_class";
  private static String outputProbFeature = "LF_confidence";
  private static String outputSequenceSpanIDFeature = "LF_seq_span_id";

  //In the case of NER, output instance annotations to temporary
  //AS, to keep them separate.
  private static String tempOutputASName = "tmp_ouputas_for_ner12345";

  @Override
  public Resource init() throws ResourceInstantiationException {
    //Load the configuration file for training from the location given.
    //If the user changes the file location at runtime, no prob, but if
    //they change the file contents, they need to reinitialize to load it.
    if (featureSpecURL != null) {
      this.conf = new FeatureSpecification(featureSpecURL);
    }

    return this;
  }

  private Engine createLearner(Algorithm algo, File savedModelFile) {
    if (algo != null) {
      String spec = algo.toString();
      switch (algo) {
        case MALLET_CL_C45:
        case MALLET_CL_DECISION_TREE:
        case MALLET_CL_MAX_ENT:
        case MALLET_CL_NAIVE_BAYES_EM:
        case MALLET_CL_NAIVE_BAYES:
        case MALLET_CL_WINNOW:
          return new EngineMallet(savedModelFile, mode, learnerParams, spec, false);
        case MALLET_SEQ_CRF:
          return new EngineMalletSeq(savedModelFile, mode, spec, false);
        case LIBSVM:
          return new EngineLibSVM(
                  savedModelFile, mode, learnerParams, spec, false);
        case WEKA_CL_NUM_ADDITIVE_REGRESSION:
        case WEKA_CL_NAIVE_BAYES:
        case WEKA_CL_J48:
        case WEKA_CL_JRIP:
        case WEKA_CL_RANDOM_TREE:
        case WEKA_CL_IBK:
        case WEKA_CL_LOGISTIC_REGRESSION:
        case WEKA_CL_MULTILAYER_PERCEPTRON:
        case WEKA_CL_RANDOM_FOREST:
          return new EngineWeka(
                  savedModelFile, mode, learnerParams, spec, false);
      }
    }
    return null;
  }

  @Override
  public void execute() throws ExecutionException {

    if (justStarted) {
      justStarted = false;
      runAfterJustStarted();
    }
    haveSomeDocuments = true;

    Document doc = getDocument();

    switch (this.getOperation()) {
      case TRAIN:
        if (trainingLearner != null) {
          this.trainingCorpus.add(doc);
        }
        break;
      case APPLY_CURRENT_MODEL:
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
              gcs = ((EngineWeka) applicationLearner).classify(
                      this.instanceName, this.inputASName, doc);
            } else if (applicationLearner instanceof EngineMallet
                    && ((EngineMallet) applicationLearner).getMode() == Mode.CLASSIFICATION) {
              gcs = ((EngineMallet) applicationLearner).classify(
                      this.instanceName, this.inputASName, doc);
            } else if (applicationLearner instanceof EngineMallet
                    && ((EngineMallet) applicationLearner).getMode() == Mode.NAMED_ENTITY_RECOGNITION) {
              gcs = ((EngineMalletSeq) applicationLearner).classify(
                      this.instanceName, this.inputASName, doc, this.sequenceSpan);
            } else {
              throw new GateRuntimeException("Found a strange instance of an engine");
            }
          } else {
            switch (applicationLearner.whatIsIt()) {
              case LIBSVM:
                gcs = ((EngineLibSVM) applicationLearner).classify(
                        this.instanceName, this.inputASName, doc);
                break;
              case MALLET_CL_C45:
              case MALLET_CL_DECISION_TREE:
              case MALLET_CL_MAX_ENT:
              case MALLET_CL_NAIVE_BAYES_EM:
              case MALLET_CL_NAIVE_BAYES:
              case MALLET_CL_WINNOW:
                gcs = ((EngineMallet) applicationLearner).classify(
                        this.instanceName, this.inputASName, doc);
                break;
              case MALLET_SEQ_CRF:
                gcs = ((EngineMalletSeq) applicationLearner).classify(
                        this.instanceName, this.inputASName, doc, this.sequenceSpan);
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
                gcs = ((EngineWeka) applicationLearner).classify(
                        this.instanceName, this.inputASName, doc);
                break;
            }
          }

          addClassificationAnnotations(doc, gcs);
          if (this.getMode() == Mode.NAMED_ENTITY_RECOGNITION) {
            //We need to make the surrounding annotations
            addSurroundingAnnotations(doc);
          }
        }
        break;
      case EVALUATE_X_FOLD:
      case EVALUATE_HOLDOUT:
        if (evaluationLearner != null) {
          this.testCorpus.add(doc);
        }
        break;
      case EXPORT_ARFF:
      case EXPORT_ARFF_THRU_CURRENT_PIPE:
      case EXPORT_ARFF_NUMERIC_CLASS:
      case EXPORT_ARFF_NUMERIC_CLASS_THRU_CURRENT_PIPE:
      case EXPORT_LIBSVM:
        exportCorpus.add(document);
        break;
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
    if (this.getMode() == Mode.NAMED_ENTITY_RECOGNITION) {
      outputAnnSet = doc.getAnnotations(tempOutputASName);
    }

    while (gcit.hasNext()) {
      GateClassification gc = gcit.next();

      // JP: TODO: need to check if we always get the correct confidence
      // score here and if the default makes this do what is expected!
      if (this.getMode() == Mode.CLASSIFICATION
              && gc.getConfidenceScore() < this.getConfidenceThreshold()) {
        //Skip it
      } else //We have a valid classification. Now write it onto the document.
      // If this is classification and the add feature value is set,
      // do not create a new annotation and instead just add features
      // to the instance annotation
      // TODO: this can be refactored to be more concise!
      {
        if (getMode() == Mode.CLASSIFICATION && getOutClassFeature() != null
                && !getOutClassFeature().isEmpty()) {
          Annotation instance = gc.getInstance();
          FeatureMap fm = instance.getFeatures();
          // Instead of the predefined output class feature name use the one specified
          // as a PR parameter
          //
          // fm.put(outputClassFeature, gc.getClassAssigned());
          fm.put(getOutClassFeature(), gc.getClassAssigned());
          fm.put(outputProbFeature, gc.getConfidenceScore());
          if (gc.getClassList() != null && gc.getConfidenceList() != null) {
            fm.put(outputClassFeature + "_list", gc.getClassList());
            fm.put(outputProbFeature + "_list", gc.getConfidenceList());
          }
        } else {
          FeatureMap fm = Factory.newFeatureMap();
          fm.putAll(gc.getInstance().getFeatures());
          fm.put(outputClassFeature, gc.getClassAssigned());
          fm.put(outputProbFeature, gc.getConfidenceScore());
          if (gc.getClassList() != null && gc.getConfidenceList() != null) {
            fm.put(outputClassFeature + "_list", gc.getClassList());
            fm.put(outputProbFeature + "_list", gc.getConfidenceList());
          }
          //fm.put(this.conf.getIdentifier(), identifier);
          if (gc.getSeqSpanID() != null) {
            fm.put(outputSequenceSpanIDFeature, gc.getSeqSpanID());
          }
          outputAnnSet.add(gc.getInstance().getStartNode(),
                  gc.getInstance().getEndNode(),
                  gc.getInstance().getType(), fm);
        } // else if CLASSIFICATION and have out class feature
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
    List<Annotation> insts = fromset.get(this.instanceName).inDocumentOrder();

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
      Integer sequenceSpanID = (Integer) inst.getFeatures().get(outputSequenceSpanIDFeature);
      if (sequenceSpanID == null) {
        sequenceSpanID = 0;
      }
      AnnToAdd thisAnnToAdd = annsToAdd.get(sequenceSpanID);

      //B, I or O??
      String status = (String) inst.getFeatures().get(outputClassFeature);
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
          fm.put(outputProbFeature, entityconf);
          if (sequenceSpanID != null) {
            fm.put(outputSequenceSpanIDFeature, sequenceSpanID);
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
        ata.conf = (Double) inst.getFeatures().get(outputProbFeature);
        ata.len++;
        annsToAdd.put(sequenceSpanID, ata);
      }

      if (status.equals("inside") && thisAnnToAdd != null) {
        thisAnnToAdd.conf += (Double) inst.getFeatures().get(outputProbFeature);
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
        fm.put(outputProbFeature, entityconf);
        if (sequenceSpanID != null) {
          fm.put(outputSequenceSpanIDFeature, sequenceSpanID);
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
  public synchronized void interrupt() {
    super.interrupt();
  }

  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1)
          throws ExecutionException {
    // reset the flags for the next time the controller is run
    justStarted = false;
    haveSomeDocuments = false;
  }

  @Override
  public void controllerExecutionFinished(Controller arg0)
          throws ExecutionException {
    // reset the flags for the next time the controller is run but remember if we had 
    // some documents for checking later!
    boolean hadSomeDocuments = haveSomeDocuments;
    justStarted = false;
    haveSomeDocuments = false;

    switch (this.getOperation()) {
      case TRAIN:
        // check if there were some documents: if not we have nothing to train our
        // model on!
        if (!hadSomeDocuments) {
          throw new GateRuntimeException("Cannot train, did not see any documents!");
        }
        if (trainingLearner != null) {
          //Ready to go
          // JP: Using the logger does not always make the info show, so using System.out here 
          // just to be safe.
          System.out.println("LearningFramework: Training "
                  + trainingLearner.whatIsItString() + " ...");
          System.out.println("Training set classes: "
                  + trainingCorpus.getPipe().getTargetAlphabet().toString().replaceAll("\\n", " "));
          System.out.println("Training set size: " + trainingCorpus.getInstances().size());
          System.out.println("LearningFramework: Instances: " + trainingCorpus.getInstances().size());
          if (trainingCorpus.getInstances().getDataAlphabet().size() > 20) {
            System.out.println("LearningFramework: Attributes " + trainingCorpus.getInstances().getDataAlphabet().size());
          } else {
            System.out.println("LearningFramework: Attributes " + trainingCorpus.getInstances().getDataAlphabet().toString().replaceAll("\\n", " "));
          }
          //System.out.println("DEBUG: instances are "+trainingCorpus.getInstances());
          System.out.println("DEBUG: trainingCorpus class is " + trainingCorpus.getClass());
          trainingCorpus.conclude();
          trainingLearner.train(conf, trainingCorpus);
          this.applicationLearner = trainingLearner;
          logger.info("LearningFramework: Training complete!");
        }
        break;
      case APPLY_CURRENT_MODEL:
        break;
      case EVALUATE_X_FOLD:
        if (evaluationLearner != null) {
          //Ready to evaluate
          logger.info("LearningFramework: Evaluating ..");
          testCorpus.conclude();
          evaluationLearner.evaluateXFold(testCorpus, this.foldsForXVal);
        }
        break;
      case EVALUATE_HOLDOUT:
        if (evaluationLearner != null) {
          //Ready to evaluate
          logger.info("LearningFramework: Evaluating ..");
          testCorpus.conclude();
          evaluationLearner.evaluateHoldout(
                  testCorpus, this.trainingproportion);
        }
        break;
      case EXPORT_ARFF:
      case EXPORT_ARFF_THRU_CURRENT_PIPE:
      case EXPORT_ARFF_NUMERIC_CLASS:
      case EXPORT_ARFF_NUMERIC_CLASS_THRU_CURRENT_PIPE:
        exportCorpus.conclude();
        ((CorpusWriterArff) exportCorpus).writeToFile();
        break;
      case EXPORT_LIBSVM:
        // JP: this should get moved to some better place
        // JP: I have no idea if conclude works properly here! CHECK!
        exportCorpus.conclude();
        svm_problem prob = ((CorpusWriterMallet) exportCorpus).getLibSVMProblem();
        PrintStream out = null;
        File savedir = gate.util.Files.fileFromURL(dataDirectory);
        File expdir = new File(savedir, "exportedLibSVM");
        expdir.mkdir();
        try {
          out = new PrintStream(new File(expdir, "data.libsvm"));
          for (int i = 0; i < prob.l; i++) {
            out.print(prob.y[i]);
            for (int j = 0; j < prob.x[i].length; j++) {
              out.print(" ");
              out.print(prob.x[i][j].index);
              out.print(":");
              out.print(prob.x[i][j].value);
            }
            out.println();
          }
          out.close();
        } catch (FileNotFoundException ex) {
          System.err.println("Could not write training instances to svm format file");
          ex.printStackTrace(System.out);
        }
        try {
          ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(expdir
                  + "/my.pipe"));
          oos.writeObject(exportCorpus.getInstances().getPipe());
          oos.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
        break;
    }
  }

  @Override
  public void controllerExecutionStarted(Controller arg0)
          throws ExecutionException {
    justStarted = true;
  }

  protected void runAfterJustStarted() {

    // JP: this was moved from the dataDirectory setter to avoid problems
    // but we should really make sure that the learning is reloaded only 
    // if the URL has changed since the last time (if ever) it was loaded.
    savedModelDirectoryFile = new File(
            gate.util.Files.fileFromURL(dataDirectory), savedModelDirectory);

    evaluationModelDirectoryFile = new File(
            gate.util.Files.fileFromURL(dataDirectory), evaluationModelDirectory);

    //System.out.println("LF-Info: loading model from "+savedModelDirectoryFile.getAbsolutePath()+" dataDirectory is "+dataDirectory);
    applicationLearner = Engine.restoreLearner(savedModelDirectoryFile);
    //System.out.println("LF-Info: model loaded is now "+applicationLearner);

    switch (this.getOperation()) {
      case TRAIN:
        if (trainingAlgo == null) {
          logger.warn("LearningFramework: Please select an algorithm!");
          trainingLearner = null;
          interrupt();
          break;
        } else {
          trainingLearner = this.createLearner(this.trainingAlgo, this.savedModelDirectoryFile);

          switch (this.getTrainingAlgo()) {
            case LIBSVM: //Yes we are making a mallet corpus writer for use with libsvm ..
            case MALLET_CL_C45:
            case MALLET_CL_DECISION_TREE:
            case MALLET_CL_MAX_ENT:
            case MALLET_CL_NAIVE_BAYES_EM:
            case MALLET_CL_NAIVE_BAYES:
            case MALLET_CL_WINNOW:
              File trainfilemallet = new File(
                      gate.util.Files.fileFromURL(dataDirectory), trainfilenamemallet);
              trainingCorpus = new CorpusWriterMallet(this.conf, this.instanceName,
                      this.inputASName, trainfilemallet, mode, classType,
                      classFeature, identifierFeature, scaleFeatures);
              break;
            case MALLET_SEQ_CRF:
              File trainfilemalletseq = new File(
                      gate.util.Files.fileFromURL(dataDirectory), trainfilenamemalletseq);
              trainingCorpus = new CorpusWriterMalletSeq(this.conf, this.instanceName,
                      this.inputASName, trainfilemalletseq, this.sequenceSpan,
                      mode, classType, classFeature, identifierFeature, scaleFeatures);
              break;
            case WEKA_CL_NUM_ADDITIVE_REGRESSION:
              File trainfileweka = new File(
                      gate.util.Files.fileFromURL(dataDirectory), trainfilenamearff);
              trainingCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceName,
                      this.inputASName, trainfileweka,
                      mode, classType, classFeature, identifierFeature, null, scaleFeatures);
              break;
            case WEKA_CL_NAIVE_BAYES:
            case WEKA_CL_J48:
            case WEKA_CL_JRIP:
            case WEKA_CL_RANDOM_TREE:
            case WEKA_CL_MULTILAYER_PERCEPTRON:
            case WEKA_CL_IBK:
            case WEKA_CL_LOGISTIC_REGRESSION:
            case WEKA_CL_RANDOM_FOREST:
              trainfileweka = new File(
                      gate.util.Files.fileFromURL(dataDirectory), trainfilenamearff);
              trainingCorpus = new CorpusWriterArff(this.conf, this.instanceName,
                      this.inputASName, trainfileweka,
                      mode, classType, classFeature, identifierFeature,
                      null, scaleFeatures);
              break;
          }

          logger.info("LearningFramework: Preparing training data ...");
        }
        break;
      case APPLY_CURRENT_MODEL:
        // TODO JP: OK I do not really understand if we need this here or not, but it seems not
        // applicationLearner = Engine.restoreLearner(savedModelDirectoryFile);
        if (this.applicationLearner == null) {
          logger.warn("LearningFramework: either train a new or load an existing model!");
          interrupt();
          break;
        } else {
          System.out.println("LearningFramework: Applying model "
                  + applicationLearner.whatIsItString() + " ...");
          if (applicationLearner.getPipe() == null) {
            System.out.println("Model classes: UNKNOWN, no pipe");
          } else {
            System.out.println("Model classes: "
                    + applicationLearner.getPipe().getTargetAlphabet().toString().replaceAll("\\n", " "));
          }

          if (applicationLearner.getMode() != this.getMode()) {
            logger.warn("LearningFramework: Warning! Applying "
                    + "model trained in " + applicationLearner.getMode()
                    + " mode in " + this.getMode() + " mode!");
          }
        }
        break;
      case EVALUATE_X_FOLD:
      case EVALUATE_HOLDOUT:
        if (trainingAlgo == null) {
          logger.warn("LearningFramework: Please select an algorithm!");
          evaluationLearner = null;
          interrupt();
          break;
        } else {
          evaluationLearner = this.createLearner(this.trainingAlgo, this.evaluationModelDirectoryFile);

          switch (this.getTrainingAlgo()) {
            case LIBSVM: //Yes we are making a mallet corpus writer for use with libsvm ..
            case MALLET_CL_C45:
            case MALLET_CL_DECISION_TREE:
            case MALLET_CL_MAX_ENT:
            case MALLET_CL_NAIVE_BAYES_EM:
            case MALLET_CL_NAIVE_BAYES:
            case MALLET_CL_WINNOW:
              File testfilemallet = new File(
                      gate.util.Files.fileFromURL(dataDirectory), testfilenamemallet);
              testCorpus = new CorpusWriterMallet(this.conf, this.instanceName,
                      this.inputASName, testfilemallet, mode, classType,
                      classFeature, identifierFeature, scaleFeatures);
              break;
            case MALLET_SEQ_CRF:
              File testfilemalletseq = new File(
                      gate.util.Files.fileFromURL(dataDirectory), testfilenamemalletseq);
              testCorpus = new CorpusWriterMalletSeq(this.conf, this.instanceName,
                      this.inputASName, testfilemalletseq, this.sequenceSpan,
                      mode, classType, classFeature, identifierFeature, scaleFeatures);
              break;
            case WEKA_CL_NUM_ADDITIVE_REGRESSION:
              File testfileweka = new File(
                      gate.util.Files.fileFromURL(dataDirectory), testfilenamearff);
              testCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceName,
                      this.inputASName, testfileweka, mode, classType, classFeature,
                      identifierFeature, null, scaleFeatures);
              break;
            case WEKA_CL_NAIVE_BAYES:
            case WEKA_CL_J48:
            case WEKA_CL_JRIP:
            case WEKA_CL_RANDOM_TREE:
            case WEKA_CL_MULTILAYER_PERCEPTRON:
            case WEKA_CL_IBK:
            case WEKA_CL_LOGISTIC_REGRESSION:
            case WEKA_CL_RANDOM_FOREST:
              testfileweka = new File(
                      gate.util.Files.fileFromURL(dataDirectory), testfilenamearff);
              testCorpus = new CorpusWriterArff(this.conf, this.instanceName,
                      this.inputASName, testfileweka, mode, classType, classFeature,
                      identifierFeature, null, scaleFeatures);
              break;
          }
        }
        break;
      case EXPORT_LIBSVM:
        File trainfilemallet = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);
        exportCorpus = new CorpusWriterMallet(conf, instanceName,
                inputASName, trainfilemallet, mode, classType,
                classFeature, identifierFeature, scaleFeatures);
        break;
      case EXPORT_ARFF:
        File outputfilearff = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);
        exportCorpus = new CorpusWriterArff(this.conf, this.instanceName, this.inputASName,
                outputfilearff, mode, classType, classFeature, identifierFeature, null,
                scaleFeatures);
        break;
      case EXPORT_ARFF_THRU_CURRENT_PIPE:
        File outputfilearff2 = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);

        if (CorpusWriterArff.getArffPipe(outputfilearff2) == null) {
          logger.warn("LearningFramework: No pipe found in corpus output directory! "
                  + "Begin by exporting arff training data without using pipe "
                  + "so as to create one which you can then use to export test "
                  + "data.");
          break;
        }

        exportCorpus = new CorpusWriterArff(this.conf, this.instanceName, this.inputASName,
                outputfilearff2, mode, classType, classFeature, identifierFeature,
                CorpusWriterArff.getArffPipe(outputfilearff2), scaleFeatures);
        break;
      case EXPORT_ARFF_NUMERIC_CLASS:
        File outputfilearff3 = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);
        exportCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceName, this.inputASName,
                outputfilearff3, mode, classType, classFeature, identifierFeature, null, scaleFeatures);
        break;
      case EXPORT_ARFF_NUMERIC_CLASS_THRU_CURRENT_PIPE:
        File outputfilearff4 = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);

        if (CorpusWriterArff.getArffPipe(outputfilearff4) == null) {
          logger.warn("LearningFramework: No pipe found in corpus output directory! "
                  + "Begin by exporting arff training data without using pipe "
                  + "so as to create one which you can then use to export test "
                  + "data.");
          break;
        }

        exportCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceName, this.inputASName,
                outputfilearff4, mode, classType, classFeature, identifierFeature,
                CorpusWriterArff.getArffPipe(outputfilearff4), scaleFeatures);
        break;
    }
  }

  public void finishedNoDocument(Controller arg0, Throwable throwable) {
    // no need to do anything
  }

  @Override
  protected void execute(Document document) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void beforeFirstDocument(Controller ctrl) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
