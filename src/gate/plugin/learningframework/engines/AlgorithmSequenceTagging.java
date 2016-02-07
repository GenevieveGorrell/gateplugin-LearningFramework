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
  MALLET_SEQ_CRF_XXX(EngineMalletSeq.class,null); // too complex to specify the trainer class here
  // MALLET_SEQ_SPECIFY_CLASS(EngineMallet.class,null); // it is not really possible to specify a class for this
  private AlgorithmSequenceTagging() {
    
  }
  private AlgorithmSequenceTagging(Class engineClass, Class algorithmClass) {
    this.engineClass = engineClass;
    this.trainerClass = algorithmClass;
  }
  private Class engineClass;
  private Class trainerClass;
  public Class getEngineClass() { return engineClass; }

  @Override
  public Class getTrainerClass() {
    return trainerClass;
  }

  @Override
  public void setTrainerClass(Class trainerClass) {
    this.trainerClass = trainerClass;
  }
}
