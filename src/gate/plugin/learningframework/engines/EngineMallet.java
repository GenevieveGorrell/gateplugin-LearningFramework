/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.classify.C45Trainer;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.types.InstanceList;
import gate.AnnotationSet;
import gate.learningframework.classification.GateClassification;
import gate.util.GateRuntimeException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johann Petrak
 */
public class EngineMallet extends Engine {

  protected ClassifierTrainer trainer;
  protected Classifier model;

  @Override
  public Object evaluateHoldout(InstanceList instances, double portion, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Object evaluateXVal(InstanceList instances, int k, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void loadModel(File directory, Info info, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trainModel(InstanceList instances, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<GateClassification> classify(AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    // if this is one of the algorithms were we need to deal with parameters in some way,
    // use the non-empty constructor, otherwise just instanciate the trainer class.
    if(algorithm.equals(AlgorithmClassification.MALLET_CL_C45)) {
      trainer = new C45Trainer();
      // we could also do
      Class trainerClass = algorithm.getTrainerClass();
      try {
        trainer = (ClassifierTrainer)trainerClass.newInstance();
      } catch (Exception ex) {
        throw new GateRuntimeException("Could not create trainer instance for "+trainerClass);
      }
    }
  }

}
