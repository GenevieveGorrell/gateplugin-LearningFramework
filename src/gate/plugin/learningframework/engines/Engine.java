/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.types.InstanceList;
import gate.AnnotationSet;
import gate.learningframework.classification.GateClassification;
import gate.util.GateRuntimeException;
import java.io.File;
import java.util.List;

/**
 * Base class for all engines.
 * This is the base class for all engines. It also provides the static factory method 
 * loadEngine(directory) which will return a subclass of the appropriate type, if possible.
 * @author Johann Petrak
 */
public abstract class Engine {
  /**
   * A factory method to return the engine which is stored in the given directory.
   * All the filenames are fixed so only the directory name is needed.
   * This will first read the info file which contains information about the Engine class,
   * then construct the Engine instance and initialize it.
   * If there are parameters that will influence the initialization of the algorithm,
   * they will be used.
   * @param directory
   * @return 
   */
  public static Engine loadEngine(File directory, String parms) {
    // 1) read the infor file
    // extract the Engine class from the file and create an instance of the engine
    Engine eng = new EngineWeka();
    // now use the specific engine's loadModel method to complete the loading: each engine
    // knows best how to load its own kinds of models.
    // eng.loadModel(directory,info,parms);
    return eng;
  }
  
  /** 
   * A factory method to create a new instance of an engine with the given backend algorithm.
   * This works in two steps: first the instance of the engine is created, then that instance's
   * method for initializing the algorithm is called (initializeAlgorithm) with the given parameters.
   * @param engineClass
   * @param algorithmClass
   * @return 
   */
  public static Engine createEngine(Algorithm algorithm, String parms) {
    Engine eng;
    try {
      eng = (Engine)algorithm.getEngineClass().newInstance();
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not create the Engine "+algorithm.getEngineClass(),ex);
    }
    eng.initializeAlgorithm(algorithm,parms);
    return eng;
  }
  
  
  /**
   * This prepares the output directory for storing a trained model. This should only be 
   * done once the new model has been successfully be trained, so any old model is kept in
   * case of a problem.
   * @param directory 
   */
  public static void prepareOutputDirectory(File directory) {
    // TODO: delete the known files for saving a model. In most cases this is a model file,
    // an info file and a pipe file.
  }
  
  
  /**
   * Load a stored model into the engine.
   * @param directory
   * @param info 
   */
  public abstract void loadModel(File directory, Info info, String parms);
  
  /**
   * Train a model from the instances.
   * This always takes our own representation of instances (which is a Mallet InstanceList ATM).
   * The Engine instance should know best how to use or convert that representation to its own
   * format, using one of the CorpusRepresentationXXX classes.
   */
  public abstract void trainModel(InstanceList instances, String parms);
  
  /**
   * Classify all instance annotations.
   * If the algorithm is a sequence tagger, the sequence annotations must be given, otherwise
   * they must not be given.
   * @return 
   */
  public abstract List<GateClassification> classify(AnnotationSet instanceAS, AnnotationSet inputAS,
          AnnotationSet sequenceAS, String parms);
  
  /**
   * Perform the native Holdout evaluation using the given portion size.
   * Implementations may return some object that represents the evaluation result or just
   * log the results.
   * @param instances
   * @param portion
   * @param parms
   * @return 
   */
  public abstract Object evaluateHoldout(InstanceList instances, double portion, String parms);
  
  public abstract Object evaluateXVal(InstanceList instances, int k, String parms);
  
  public abstract void initializeAlgorithm(Algorithm algorithm, String parms);
  
  // fields shared by all subclasses of Engine:
  // The following fields are present in each subclass, but not inherited from this class 
  // but defined with the Engine-specific types.
  
  // TODO: not sure if this will work for all situations, we may have to distinguish between
  // trainer, applier, and model
  
  /**
   * The model is the result of the training step and is what is stored/loaded.
   * This will only have a value if an engine was loaded successfully or if training was 
   * completed successfully.
   */
  protected Object model;
  
  /**
   * The trainer is the instance of something that can be used to create a trained model.
   * This should be set right after the EngineXXX instance is created.
   */
  protected Object trainer;
  
  
}
