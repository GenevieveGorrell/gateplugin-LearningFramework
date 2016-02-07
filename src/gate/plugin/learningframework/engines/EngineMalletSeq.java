/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.types.InstanceList;
import gate.AnnotationSet;
import gate.learningframework.classification.GateClassification;
import java.io.File;
import java.util.List;

/**
 *
 * @author Johann Petrak
 */
public class EngineMalletSeq extends Engine {


  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void loadModel(File directory, String parms) {
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
  public Object evaluateHoldout(InstanceList instances, double portion, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Object evaluateXVal(InstanceList instances, int k, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void saveModel(File directory) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
