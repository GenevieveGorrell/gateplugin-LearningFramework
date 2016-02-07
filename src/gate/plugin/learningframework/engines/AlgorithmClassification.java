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
public enum AlgorithmClassification implements Algorithm {
  LIBSVM_CL_XXX(EngineLibSVM.class,null),
  MALLET_CL_C45(EngineMallet.class,cc.mallet.classify.C45Trainer.class),
  MALLET_CL_DECISION_TREE(EngineMallet.class,null),
  MALLET_CL_MAX_ENT,
  MALLET_CL_NAIVE_BAYES_EM,
  MALLET_CL_NAIVE_BAYES,
  MALLET_CL_WINNOW,
  MALLET_SEQ_CRF(EngineMalletSeq.class,null),
  MALLET_CL_SPECIFY_CLASS(EngineMallet.class,null),
  WEKA_CL_NAIVE_BAYES(EngineWeka.class,weka.classifiers.bayes.NaiveBayes.class),
  WEKA_CL_JRIP,
  WEKA_CL_J48,
  WEKA_CL_MULTILAYER_PERCEPTRON,
  WEKA_CL_RANDOM_TREE,
  WEKA_CL_IBK,
  WEKA_CL_LOGISTIC_REGRESSION(EngineWeka.class,weka.classifiers.functions.Logistic.class),
  WEKA_CL_RANDOM_FOREST(EngineWeka.class,weka.classifiers.trees.RandomForest.class),
  WEKA_CL_SPECIFY_CLASS(EngineWeka.class,null);
  private AlgorithmClassification() {
    
  }
  private AlgorithmClassification(Class engineClass, Class algorithmClass) {
    this.engineClass = engineClass;
    this.trainerClass = algorithmClass;
  }
  private Class engineClass;
  private Class trainerClass;
  public Class getEngineClass() { return engineClass; }
  public Class getTrainerClass() { return trainerClass; }

  @Override
  public void setTrainerClass(Class trainerClass) {
    this.trainerClass = trainerClass;
  }
}
