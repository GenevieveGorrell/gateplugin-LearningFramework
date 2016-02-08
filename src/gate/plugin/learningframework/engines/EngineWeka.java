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
import gate.plugin.learningframework.data.CorpusRepresentationWeka;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import weka.classifiers.Classifier;

/**
 *
 * @author Johann Petrak
 */
public class EngineWeka extends Engine {
  
  
  @Override
  public Object evaluateHoldout(InstanceList instances, double portion, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Object evaluateXVal(InstanceList instances, int k, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void loadModel(File directory, String parms) {
    // when this is called, info should already be set
    // we create the instance of the training algorithm from the infor and the instance of
    // the actual trained classifier from de-serialization of the file
    File modelFile = new File(directory, "model.model");
    try {
        ObjectInputStream ois
                = new ObjectInputStream(new FileInputStream(modelFile));
        model = (Classifier) ois.readObject();
        System.out.println("Loaded Weka model " + model.getClass());
        ois.close();
    } catch (Exception e) {
        throw new GateRuntimeException("Could not load Weka model file "+modelFile,e);
    }    
    try {
      trainer = Class.forName(info.algorithmClass).newInstance();
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not create Weka trainer instance for "+info.algorithmClass,ex);
    }
  }

  @Override
  public void trainModel(CorpusRepresentationMallet data, String parms) {
    if(trainer == null) {
      throw new GateRuntimeException("Cannot train Weka model, not trainer initialized");
    }
    Classifier alg = (Classifier)trainer;
    // convert the Mallet representation to Weka instances
    CorpusRepresentationWeka crw = new CorpusRepresentationWeka(data);
    
    try {
      alg.buildClassifier(crw.getRepresentationWeka());
      // set the trained model to the algorithm, in Weka they are identical
      model = alg;
    } catch (Exception ex) {
      throw new GateRuntimeException("Error during training of Weka algorithm "+alg.getClass(),ex);
    }
  }

  @Override
  public List<GateClassification> classify(
          CorpusRepresentationMallet crm,
          AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    Class trainerClass = algorithm.getTrainerClass();
    System.err.println("LF DEBUG: trying to initialize trainer class "+trainerClass);
    try {
      trainer = trainerClass.newInstance();
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not create Weka trining algorithm for class "+trainerClass);
    }
  }

  @Override
  public void saveModel(File directory) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
