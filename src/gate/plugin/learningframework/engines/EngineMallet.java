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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Johann Petrak
 */
public class EngineMallet extends Engine {

  Logger logger = Logger.getLogger(EngineMallet.class);

  @Override
  public void loadModel(File directory, String parms) {
    File modelFile = new File(directory, FILENAME_MODEL);
    if (!modelFile.exists()) {
      throw new GateRuntimeException("Cannot load model file, does not exist: " + modelFile);
    }
    Classifier classifier;
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new FileInputStream(modelFile));
      classifier = (Classifier) ois.readObject();
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not load Mallet model", ex);
    } finally {
      if (ois != null) {
        try {
          ois.close();
        } catch (IOException ex) {
          logger.error("Could not close object input stream after loading model", ex);
        }
      }
    }
  }

  @Override
  public void trainModel(InstanceList instances, String parms) {
    ((ClassifierTrainer) trainer).train(instances);
  }

  @Override
  public List<GateClassification> classify(AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    // if this is one of the algorithms were we need to deal with parameters in some way,
    // use the non-empty constructor, otherwise just instanciate the trainer class.
    // But only bother if we have a parameter at all
    if (parms == null || parms.trim().isEmpty()) {
      // no parameters, just instantiate the class
      Class trainerClass = algorithm.getTrainerClass();
      try {
        trainer = (ClassifierTrainer) trainerClass.newInstance();
      } catch (Exception ex) {
        throw new GateRuntimeException("Could not create trainer instance for " + trainerClass);
      }
    } else {      
      // there are parameters, so if it is one of the algorithms were we support setting
      // a parameter do this      
      if (algorithm.equals(AlgorithmClassification.MALLET_CL_C45)) {      
        Parms ps = new Parms(parms, "m:maxDepth:i", "p:prune:b");
        int maxDepth = (int)ps.getValueOrElse("maxDepth", -1);
        boolean prune = (boolean)ps.getValueOrElse("prune",false);  
        trainer = new C45Trainer(maxDepth,prune);
      } else {
        // all other algorithms are still just instantiated from the class name, we ignore
        // the parameters
        logger.warn("Parameters ignored when creating Mallet trainer " + algorithm.getTrainerClass());
        Class trainerClass = algorithm.getTrainerClass();
        try {
          trainer = (ClassifierTrainer) trainerClass.newInstance();
        } catch (Exception ex) {
          throw new GateRuntimeException("Could not create trainer instance for " + trainerClass);
        }
      }
    }
  }

  @Override
  public void saveModel(File directory) {
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(new File(directory, FILENAME_MODEL)));
      oos.writeObject(model);
    } catch (Exception e) {
      throw new GateRuntimeException("Could not store Mallet model", e);
    } finally {
      if (oos != null) {
        try {
          oos.close();
        } catch (IOException ex) {
          logger.error("Could not close object output stream", ex);
        }
      }
    }
  }

  @Override
  public Object evaluateHoldout(InstanceList instances, double portion, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Object evaluateXVal(InstanceList instances, int k, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
