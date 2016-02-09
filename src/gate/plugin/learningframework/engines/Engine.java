/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.types.InstanceList;
import gate.AnnotationSet;
import gate.learningframework.classification.GateClassification;
import gate.plugin.learningframework.data.CorpusRepresentationMallet;
import gate.util.GateRuntimeException;
import java.io.File;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Base class for all engines.
 * This is the base class for all engines. It also provides the static factory method 
 * loadEngine(directory) which will return a subclass of the appropriate type, if possible.
 * @author Johann Petrak
 */
public abstract class Engine {
  
  public static final String FILENAME_MODEL = "lf.model";
  public static final String FILENAME_PIPE = "lf.pipe";
  
  Logger logger = Logger.getLogger(Engine.class);
  
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
    // 1) read the info file
    Info info = Info.load(directory);
    // extract the Engine class from the file and create an instance of the engine
    Engine eng;
    try {
      eng = (Engine)Class.forName(info.engineClass).newInstance();
    } catch (Exception ex) {
      throw new GateRuntimeException("Error creating engine class when loading: "+info.engineClass,ex);
    }
    // store the info we have just obtained in the new engine instance
    eng.info = info;
    // now use the specific engine's loadModel method to complete the loading: each engine
    // knows best how to load its own kinds of models.
    eng.loadModel(directory, parms);
    return eng;
  }
  
  /**
   * Save an engine to a directory.
   * This saves the information about the engine and the training algorithm together with
   * a trained model.
   * It does not make sense to save an engine before all that information is present, a
   * GateRuntimeException is thrown if the engine is not in a state where it can be reasonably 
   * saved.
   * @param directory 
   */
  public void saveEngine(File directory) {
    // First save the info. 
    info.save(directory);
    // Then delegate to the engine to save the model
    saveModel(directory);
  }
  
  
  /** 
   * A factory method to create a new instance of an engine with the given backend algorithm.
   * This works in two steps: first the instance of the engine is created, then that instance's
   * method for initializing the algorithm is called (initializeAlgorithm) with the given parameters.
   * However, some training algorithms cannot be instantiated until all the training data is
   * there (e.g. Mallet CRF) - for these, the initializeAlgorithm method does nothing and the
   * actual algorithm initialization happens when the train method is called. 
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
   * Load a stored model into the engine.
   * @param directory
   * @param info 
   */
  public abstract void loadModel(File directory, String parms);
  
  public abstract void saveModel(File directory);
  
  
  /**
   * Train a model from the instances.
   * This always takes our own representation of instances (which is a Mallet InstanceList ATM).
   * The Engine instance should know best how to use or convert that representation to its own
   * format, using one of the CorpusRepresentationXXX classes.
   */
  public abstract void trainModel(CorpusRepresentationMallet data, String parms);
  
  /**
   * Classify all instance annotations.
   * If the algorithm is a sequence tagger, the sequence annotations must be given, otherwise
   * they must not be given. 
   * @return 
   */
  public abstract List<GateClassification> classify(
          CorpusRepresentationMallet cr,
          AnnotationSet instanceAS, AnnotationSet inputAS,
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
  public Object getModel() { return model; }
  
  /**
   * The trainer is the instance of something that can be used to create a trained model.
   * This should be set right after the EngineXXX instance is created.
   */
  protected Object trainer;
  public Object getTrainer() { return trainer; }
  
  protected Info info;
  
  public Info getInfo() { return info; }
  
  CorpusRepresentationMallet crMallet;
  /**
   * Set the Mallet corpus representation to be used with this engine.
   * This must be set for some engines before classification so that their own corpus
   * representation can be created and cached. If this has not been called but is required,
   * the Engine method that depends on it throws an exception.
   * 
   * TODO: not sure if this is really necessary, probably not. For now only Weka really 
   * needs to know about the pipeline so the dataset can be created that is needed to 
   * convert individual mallet instances to weka instances at classification time.
   * But we could maybe just as well do this by caching the weka dataset once we have 
   * created it for the first call of classify.
   * In fact this is what we are trying at the moment ...
   * @param crm 
   */
  public void setCorpusRepresentation(CorpusRepresentationMallet crm, boolean includeTarget) { 
    crMallet = crm; 
  }
  
}
