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
public enum AlgorithmRegression {
  LIBSVM_RG_XXX(EngineLibSVM.class,null),
  MALLET_RG_SPECIFY_CLASS(EngineMallet.class,null),
  WEKA_RG_ADDITIVE_REGRESSION(EngineWeka.class,null),
  WEKA_RG_SPECIFY_CLASS(EngineWeka.class,null);
  private AlgorithmRegression() {
    
  }
  private AlgorithmRegression(Class engineClass, Class algorithmClass) {
    this.engineClass = engineClass;
    this.algorithmClass = algorithmClass;
  }
  private Class engineClass;
  private Class algorithmClass;
  public Class getEngineClass() { return engineClass; }
  public Class getAlgorithmClass() { return algorithmClass; }
}
