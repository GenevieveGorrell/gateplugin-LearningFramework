/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import cc.mallet.classify.Classifier;
import static gate.plugin.learningframework.engines.Engine.FILENAME_MODEL;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Johann Petrak
 */
public abstract class EngineMallet extends Engine {

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


  
}
