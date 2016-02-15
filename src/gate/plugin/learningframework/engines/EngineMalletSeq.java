/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.classify.Classifier;
import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFOptimizableByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByValueGradients;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.optimize.Optimizable;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import gate.Annotation;
import gate.AnnotationSet;
import gate.plugin.learningframework.GateClassification;
import gate.plugin.learningframework.Mode;
import gate.plugin.learningframework.corpora.CorpusWriterMalletSeq;
import gate.plugin.learningframework.data.CorpusRepresentationMalletClass;
import gate.plugin.learningframework.data.CorpusRepresentationMalletSeq;
import static gate.plugin.learningframework.engines.Engine.FILENAME_MODEL;
import gate.plugin.learningframework.features.TargetType;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Johann Petrak
 */
public class EngineMalletSeq extends EngineMallet {

  private static Logger logger = Logger.getLogger(EngineMalletSeq.class);
  
  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    // DOES NOTHINIG?
  }

  public AlgorithmKind getAlgorithmKind() { 
    return AlgorithmKind.SEQUENCE_TAGGER; 
  }
 

  @Override
  public void trainModel(String parms) {
    
    // TODO: maybe we should allow more flexibility here based on the parms specified!?!?!
    InstanceList trainingData = corpusRepresentationMallet.getRepresentationMallet();
    //Sanity check--how does the data look?
    //logger.info("LearningFramework: Instances: " + trainingData.size());
    //logger.info("LearningFramework: Data labels: " + trainingData.getDataAlphabet().size());
    //logger.info("LearningFramework: Target labels: " + trainingData.getTargetAlphabet().size());
    //Including the pipe at this stage means we have it available to
    //put data through at apply time.
    CRF crf = new CRF(trainingData.getPipe(), null);
    model = crf;

    // construct the finite state machine
    crf.addFullyConnectedStatesForLabels();
    // initialize model's weights
    crf.setWeightsDimensionAsIn(trainingData, false);

    //  CRFOptimizableBy* objects (terms in the objective function)
    // objective 1: label likelihood objective
    CRFOptimizableByLabelLikelihood optLabel
            = new CRFOptimizableByLabelLikelihood(crf, trainingData);

    // CRF trainer
    Optimizable.ByGradientValue[] opts
            = new Optimizable.ByGradientValue[]{optLabel};
    // by default, use L-BFGS as the optimizer
    CRFTrainerByValueGradients crfTrainer = new CRFTrainerByValueGradients(crf, opts);

    // all setup done, train until convergence
    crfTrainer.setMaxResets(0);
    crfTrainer.train(trainingData, Integer.MAX_VALUE);
    
    updateInfo();
    
  }

  @Override
  public List<GateClassification> classify(
          AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    // stop growth
    CorpusRepresentationMalletSeq data = (CorpusRepresentationMalletSeq)corpusRepresentationMallet;
    data.stopGrowth();
    
    List<GateClassification> gcs = new ArrayList<GateClassification>();

    CRF crf = (CRF)model;
    
    for(Annotation sequenceAnn : sequenceAS) {
      int sequenceSpanId = sequenceAnn.getId();
      Instance inst = data.getInstanceForSequence( 
              instanceAS, sequenceAnn, inputAS, null, null, TargetType.NONE, null);

      //Always put the instance through the same pipe used for training.
      inst = crf.getInputPipe().instanceFrom(inst);

      SumLatticeDefault sl = new SumLatticeDefault(crf,
              (FeatureVectorSequence) inst.getData());

      List<Annotation> instanceAnnotations = gate.Utils.getContainedAnnotations(
              instanceAS, sequenceAnn).inDocumentOrder();

      //Sanity check that we're mapping the probs back onto the right anns.
      //This being wrong might follow from errors reading in the data to mallet inst.
      if (instanceAnnotations.size() != ((FeatureVectorSequence) inst.getData()).size()) {
        logger.warn("LearningFramework: CRF output length: "
                + ((FeatureVectorSequence) inst.getData()).size()
                + ", GATE instances: " + instanceAnnotations.size()
                + ". Can't assign.");
      } else {
        int i = 0;
        for (Annotation instanceAnn : instanceAnnotations) {
            i++;

          String bestLabel = null;
          double bestProb = 0.0;

          //For each label option ..
          for (int j = 0; j < crf.getOutputAlphabet().size(); j++) {
            String label = crf.getOutputAlphabet().lookupObject(j).toString();

            //Get the probability of being in state j at position i+1
            //Note that the plus one is because the labels are on the
            //transitions. Positions are between transitions.
            double marg = sl.getGammaProbability(i, crf.getState(j));
            if (marg > bestProb) {
              bestLabel = label;
              bestProb = marg;
            }
          }
          GateClassification gc = new GateClassification(
                  instanceAnn, bestLabel, bestProb, sequenceSpanId);

          gcs.add(gc);
        }
      }
    }
    data.startGrowth();
    return gcs;
  }

  @Override
  public Object evaluateHoldout(InstanceList instances, double portion, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Object evaluateXVal(InstanceList instances, int k, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void loadMalletCorpusRepresentation(File directory) {
    corpusRepresentationMallet = CorpusRepresentationMalletSeq.load(directory);
  }
  
  @Override
  protected void loadModel(File directory, String parms) {
    File modelFile = new File(directory, FILENAME_MODEL);
    if (!modelFile.exists()) {
      throw new GateRuntimeException("Cannot load model file, does not exist: " + modelFile);
    }
    CRF classifier;
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new FileInputStream(modelFile));
      classifier = (CRF) ois.readObject();
      model=classifier;
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not load Mallet model", ex);
    } finally {
      if (ois != null) {
        try {
          ois.close();
        } catch (IOException ex) {
          logger.error("Could not close object input stream after loading model", ex);
        }
      }
    }
  }
  

}
