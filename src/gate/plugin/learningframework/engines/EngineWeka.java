/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import gate.Annotation;
import gate.AnnotationSet;
import gate.plugin.learningframework.GateClassification;
import gate.plugin.learningframework.data.CorpusRepresentationMalletClass;
import gate.plugin.learningframework.data.CorpusRepresentationWeka;
import gate.plugin.learningframework.mallet.LFPipe;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Classifier;
import weka.core.Instances;

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
  protected void loadModel(File directory, String parms) {
    // when this is called, info should already be set
    // we create the instance of the training algorithm from the infor and the instance of
    // the actual trained classifier from de-serialization of the file
    File modelFile = new File(directory, FILENAME_MODEL);
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
      trainer = Class.forName(info.trainerClass).newInstance();
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not create Weka trainer instance for "+info.trainerClass,ex);
    }
    // now load the Mallet corpus representation
    loadMalletCorpusRepresentation(directory);
  }

  @Override
  public void trainModel(String parms) {
    if(trainer == null) {
      throw new GateRuntimeException("Cannot train Weka model, not trainer initialized");
    }
    Classifier alg = (Classifier)trainer;
    // convert the Mallet representation to Weka instances
    CorpusRepresentationWeka crw = new CorpusRepresentationWeka(corpusRepresentationMallet);
    
    try {
      alg.buildClassifier(crw.getRepresentationWeka());
      // set the trained model to the algorithm, in Weka they are identical
      model = alg;
    } catch (Exception ex) {
      throw new GateRuntimeException("Error during training of Weka algorithm "+alg.getClass(),ex);
    }
    updateInfo();
  }
  
  CorpusRepresentationWeka crWeka;
  
  
  @Override
  protected void loadMalletCorpusRepresentation(File directory) {
    corpusRepresentationMallet = CorpusRepresentationMalletClass.load(directory);
    crWeka = new CorpusRepresentationWeka(corpusRepresentationMallet,false);
  }
  

  @Override
  public List<GateClassification> classify(
          AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    
    Instances instances = crWeka.getRepresentationWeka();
    CorpusRepresentationMalletClass data = (CorpusRepresentationMalletClass)corpusRepresentationMallet;
    List<GateClassification> gcs = new ArrayList<GateClassification>();
    LFPipe pipe = (LFPipe)data.getRepresentationMallet().getPipe();
    Classifier wekaClassifier = (Classifier)model;
    // iterate over the instance annotations and create mallet instances 
    for(Annotation instAnn : instanceAS.inDocumentOrder()) {
      Instance inst = data.extractIndependentFeatures(instAnn, inputAS);
      inst = pipe.instanceFrom(inst);
      // Convert to weka Instance
      weka.core.Instance wekaInstance = CorpusRepresentationWeka.wekaInstanceFromMalletInstance(instances, inst, false);
      // classify with the weka classifier or predict the numeric value: if the mallet pipe does have
      // a target alphabet we assume classification, otherwise we assume regression
      GateClassification gc = null;
      if(pipe.getTargetAlphabet() == null) {
        // regression
        double result=Double.NaN;
        try {
          result = wekaClassifier.classifyInstance(wekaInstance);
        } catch (Exception ex) {
          // Hmm, for now we just log the error and continue, not sure if we should stop here!
          ex.printStackTrace(System.err);
          Logger.getLogger(EngineWeka.class.getName()).log(Level.SEVERE, null, ex);
        }
        gc = new GateClassification(instAnn, (result==Double.NaN ? null : String.valueOf(result)), 1.0);
      } else {
        // classification



        // Weka AbstractClassifier already handles the situation correctly when 
        // distributionForInstance is not implemented by the classifier: in that case
        // is calls classifyInstance and returns an array of size numClasses where
        // the entry of the target class is set to 1.0 except when the classification is a missing
        // value, then all class probabilities will be 0.0
        // If distributionForInstance is implemented for the algorithm, we should get
        // the probabilities or all zeros for missing class from the algorithm.
        double[] predictionDistribution = new double[0];
        try {
          //System.err.println("classifying instance "+wekaInstance.toString());
          predictionDistribution = wekaClassifier.distributionForInstance(wekaInstance);
        } catch (Exception ex) {
          ex.printStackTrace(System.err);
        }
        // This is classification, we should always get a distribution list > 1
        if (predictionDistribution.length < 2) {
          throw new RuntimeException("Classifier returned less than 2 probabilities: " + predictionDistribution.length
                  + "for instance" + wekaInstance);
        }
        double bestprob = 0.0;
        int bestlabel = 0;

        /*
        System.err.print("DEBUG: got classes from pipe: ");
    		Object[] cls = pipe.getTargetAlphabet().toArray();
        boolean first = true;
        for(Object cl : cls) {
          if(first) { first = false; } else { System.err.print(", "); }
          System.err.print(">"+cl+"<");
        }
        System.err.println();
         */
        List<String> classList = new ArrayList<String>();
        List<Double> confidenceList = new ArrayList<Double>();
        for (int i = 0; i < predictionDistribution.length; i++) {
          int thislabel = i;
          double thisprob = predictionDistribution[i];
          String labelstr = (String) pipe.getTargetAlphabet().lookupObject(thislabel);
          classList.add(labelstr);
          confidenceList.add(thisprob);
          if (thisprob > bestprob) {
            bestlabel = thislabel;
            bestprob = thisprob;
          }
        } // end for i < predictionDistribution.length

        String cl
                = (String) pipe.getTargetAlphabet().lookupObject(bestlabel);

        gc = new GateClassification(
                instAnn, cl, bestprob, classList, confidenceList);
      }
      gcs.add(gc);
    }
    return gcs;
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
    File outFile = new File(directory,FILENAME_MODEL);
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(outFile));
      oos.writeObject(model);
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not save Weka model to "+outFile,ex);
    } finally {
      if(oos!=null) try {
        oos.close();
      } catch (IOException ex) {
        // ignore
      }
    }
  }

}
