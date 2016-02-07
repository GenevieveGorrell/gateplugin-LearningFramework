/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.learningframework.engines;

import gate.learningframework.classification.EngineWeka;

/**
 *
 * @author johann
 */
public enum AlgorithmSequenceTagging implements Algorithm {
  MALLET_SEQ_CRF(EngineMalletSeq.class,null),
  MALLET_SEQ_SPECIFY_CLASS(EngineMallet.class,null);
  private AlgorithmSequenceTagging() {
    
  }
  private AlgorithmSequenceTagging(Class engineClass, Class algorithmClass) {
    this.engineClass = engineClass;
    this.trainerClass = algorithmClass;
  }
  private Class engineClass;
  private Class trainerClass;
  public Class getEngineClass() { return engineClass; }
  public Class getAlgorithmClass() { return trainerClass; }

  @Override
  public Class getTrainerClass() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setTrainerClass(Class trainerClass) {
    this.trainerClass = trainerClass;
  }
}
