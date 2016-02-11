/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFOptimizableByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByValueGradients;
import cc.mallet.optimize.Optimizable;
import cc.mallet.types.InstanceList;
import gate.AnnotationSet;
import gate.learningframework.classification.GateClassification;
import gate.plugin.learningframework.data.CorpusRepresentationMallet;
import gate.plugin.learningframework.data.CorpusRepresentationMalletClass;
import gate.plugin.learningframework.data.CorpusRepresentationMalletSeq;
import gate.util.GateRuntimeException;
import java.io.File;
import java.util.List;

/**
 *
 * @author Johann Petrak
 */
public class EngineMalletSeq extends EngineMallet {


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
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
  

}
