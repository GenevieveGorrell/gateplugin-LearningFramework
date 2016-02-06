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

import gate.learningframework.classification.EngineLibSVM;
import gate.learningframework.classification.EngineMalletSeq;
import gate.learningframework.classification.EngineWeka;
import gate.learningframework.classification.Engine;
import gate.learningframework.classification.EngineMallet;
import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;

import gate.Controller;
import gate.Document;
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

/**
 *
 */
@CreoleResource(
        name = "LF_TrainSequenceTagging",
        helpURL = "",
        comment = "Train a machine learning model for sequence tagging")
public class LF_TrainSequenceTagging extends LF_TrainBase {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(LF_TrainSequenceTagging.class.getCanonicalName());

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

  /**
   * The implementation to be used, such as Mallet.
   *
   */
  private Algorithm trainingAlgo;

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

  protected ScalingMethod scaleFeatures = ScalingMethod.NONE;

  @RunTime
  @CreoleParameter(defaultValue = "NONE", comment = "If and how to scale features. ")
  public void setScaleFeatures(ScalingMethod sf) {
    scaleFeatures = sf;
  }

  public ScalingMethod getScaleFeatures() {
    return scaleFeatures;
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
  
  protected String classType;

  @RunTime
  @CreoleParameter(comment = "Annotation type containing/indicating the class.")
  public void setClassType(String classType) {
    this.classType = classType;
  }

  public String getClassType() {
    return this.classType;
  }

  

  private Mode mode = Mode.NAMED_ENTITY_RECOGNITION;

  //These corpora will be added to on each document so they need to be globals
  private CorpusWriter trainingCorpus = null;

  private FeatureSpecification conf = null;

  private Engine trainingLearner = null;

  private File savedModelDirectoryFile;

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
  public void execute(Document doc) {
    trainingCorpus.add(doc);
  }

  @Override
  public void afterLastDocument(Controller arg0, Throwable t) {
    if (t != null) {
      // Something went wrong during execution, so we better do not train a model... 
      logger.error("Error during the processing of documents, no training is done");
    } else if (trainingLearner != null) {
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
      logger.info("LearningFramework: Training complete!");
    }
  }

  @Override
  protected void finishedNoDocument(Controller c, Throwable t) {
    logger.error("Processing finished, but no documents seen, cannot train!");
  }

  @Override
  protected void beforeFirstDocument(Controller controller) {
    conf = new FeatureSpecification(featureSpecURL);
    savedModelDirectoryFile = new File(
            gate.util.Files.fileFromURL(dataDirectory), Globals.savedModelDirectory);

    if (trainingAlgo == null) {
      throw new GateRuntimeException("LearningFramework: no training algorithm specified");
    } else {
      trainingLearner = this.createLearner(trainingAlgo, savedModelDirectoryFile);

      // TODO: not sure how this should work: what is targetFeature if we want to do sequence
      // tagging without an actual sequence learner? 
      // CHECK: for now we always use null as the targetFeature in this PR to make it compile!
      String targetFeature = null;
      switch (this.getTrainingAlgo()) {
        case LIBSVM: //Yes we are making a mallet corpus writer for use with libsvm ..
        case MALLET_CL_C45:
        case MALLET_CL_DECISION_TREE:
        case MALLET_CL_MAX_ENT:
        case MALLET_CL_NAIVE_BAYES_EM:
        case MALLET_CL_NAIVE_BAYES:
        case MALLET_CL_WINNOW:
          File trainfilemallet = new File(
                  gate.util.Files.fileFromURL(dataDirectory), Globals.trainFilename);
          trainingCorpus = new CorpusWriterMallet(this.conf, this.instanceType,
                  this.inputASName, trainfilemallet, mode, classType,
                  targetFeature, identifierFeature, scaleFeatures);
          break;
        case MALLET_SEQ_CRF:
          File trainfilemalletseq = new File(
                  gate.util.Files.fileFromURL(dataDirectory), Globals.trainFilename);
          trainingCorpus = new CorpusWriterMalletSeq(this.conf, this.instanceType,
                  this.inputASName, trainfilemalletseq, this.sequenceSpan,
                  mode, classType, targetFeature, identifierFeature, scaleFeatures);
          break;
        case WEKA_CL_NUM_ADDITIVE_REGRESSION:
          File trainfileweka = new File(
                  gate.util.Files.fileFromURL(dataDirectory), Globals.trainFilename);
          trainingCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceType,
                  this.inputASName, trainfileweka,
                  mode, classType, targetFeature, identifierFeature, null, scaleFeatures);
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
                  gate.util.Files.fileFromURL(dataDirectory), Globals.trainFilename);
          trainingCorpus = new CorpusWriterArff(this.conf, this.instanceType,
                  this.inputASName, trainfileweka,
                  mode, classType, targetFeature, identifierFeature,
                  null, scaleFeatures);
          break;
      }

      logger.info("LearningFramework: Preparing training data ...");
    }
  }

}
