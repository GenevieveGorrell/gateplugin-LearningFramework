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
public enum AlgorithmRegression implements Algorithm {
  LIBSVM_RG_XXX(EngineLibSVM.class,null),
  MALLET_RG_SPECIFY_CLASS(EngineMallet.class,null),
  WEKA_RG_ADDITIVE_REGRESSION(EngineWeka.class,null),
  WEKA_RG_SPECIFY_CLASS(EngineWeka.class,null);
  private AlgorithmRegression() {
    
  }
  private AlgorithmRegression(Class engineClass, Class algorithmClass) {
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
