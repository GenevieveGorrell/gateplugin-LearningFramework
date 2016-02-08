/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.learningframework.data;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.InstanceList;
import gate.plugin.learningframework.engines.Parms;
import gate.plugin.learningframework.features.CodeAs;
import gate.plugin.learningframework.features.Datatype;
import gate.plugin.learningframework.features.FeatureExtraction;
import gate.plugin.learningframework.mallet.LFPipe;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.NotImplementedException;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;

/**
 *
 * @author Johann Petrak
 */
public class CorpusRepresentationWeka extends CorpusRepresentation {

  weka.core.Instances data;

  public CorpusRepresentationWeka(CorpusRepresentationMallet other) {
    data = getFromMallet(other);
  }

  @Override
  public Object getRepresentation() {
    return data;
  }

  public Instances getRepresentationWeka() {
    return data;
  }

  /**
   * Export the data. If parms is null then default ARFF format is used. In addition, parms can
   * contain the parameter -format fmt and additional parameters specific to the format. If format
   * is
   * <ul>
   * <li> "csv": -F fieldSeparator (default is tab) -M missingValueString (default is ?) -N
   * (suppress header row, default is no)
   * <li> "
   *
   * @param directory
   * @param format
   */
  public void export(File directory, String parms) {
    if (parms == null) {
      ArffSaver saver = new ArffSaver();
      saver.setInstances(data);
      File outFile = new File(directory, "data.arff");
      try {
        saver.setDestination(outFile);
      } catch (IOException ex) {
        throw new GateRuntimeException("Error exporting Weka data to " + outFile, ex);
      }
      try {
        saver.writeBatch();
      } catch (IOException ex) {
        throw new GateRuntimeException("Error exporting Weka data to " + outFile, ex);
      }
    } else {
      // first parse the parms to see if we have a -format value
      Parms ps = new Parms(parms, ":format:s");
      String format = (String) ps.getValueOrElse("format", "");
      if (format.equals("csv")) {
        ps = new Parms(parms, "F:F:s", "M:M:s", "N:N:b");
        String fieldSep = gate.util.Strings.unescape((String) ps.getValueOrElse("F", "\\t"));
        String mv = gate.util.Strings.unescape((String) ps.getValueOrElse("M", "?"));
        boolean noHeader = (boolean) ps.getValueOrElse("M", "?");
        CSVSaver saver = new CSVSaver();
        saver.setInstances(data);
        File outFile = new File(directory, "data.csv");
        try {
          saver.setDestination(outFile);
        } catch (IOException ex) {
          throw new GateRuntimeException("Error exporting Weka data to " + outFile, ex);
        }
        try {
          saver.writeBatch();
        } catch (IOException ex) {
          throw new GateRuntimeException("Error exporting Weka data to " + outFile, ex);
        }
      } else if (format.equals("json")) {
        throw new NotImplementedException("Save to json not yet implemented");
      } else if (format.equals("libsvm")) {
        throw new NotImplementedException("Save to libsvm not yet implemented");

      } else if (format.equals("svmlight")) {
        throw new NotImplementedException("Save to svmlight not yet implemented");

      } else if (format.equals("matlab")) {
        throw new NotImplementedException("Save to matlab not yet implemented");
      } else {
        throw new GateRuntimeException("Unknown format for exporting Weka representation: "+format);
      }
    }
  }

  /**
   * Create a Weka dataset from Mallet instances.
   *
   * @param cr
   * @return
   */
  public static Instances getFromMallet(CorpusRepresentationMallet cr) {
    // NOTE/TODO: this should eventually work for both Class and Seq mallet representations,
    // but for now we only support class
    if (!(cr instanceof CorpusRepresentationMalletClass)) {
      throw new GateRuntimeException("Conversion to weka not implemented yet: " + cr.getClass());
    }
    InstanceList malletInstances = cr.getRepresentationMallet();
    Alphabet dataAlph = malletInstances.getDataAlphabet();
    Pipe pipe = malletInstances.getPipe();
    // the pipe should always be an instance of LFPipe, but we allow this to be used for instancelists
    // which have been created in a different way and contain some other type of Pipe.
    // If we do hava a LFPipe, we create a map that can be used to figure out which of the 
    // mallet features are either boolean or nominal with a numeric coding. Otherwise, we 
    // regard all features as numeric. 

    // This maps from the mallet feature name to the alphabet for a nominal feature we have
    // stored in our attribute, or to a placeholder alphabet containing true/false if we have
    // a boolean feature.
    Alphabet booleanAlph = new Alphabet();
    booleanAlph.lookupIndex("false");
    booleanAlph.lookupIndex("true");
    Map<String, Alphabet> name2lfalph
            = new HashMap<String, Alphabet>();

    if (pipe instanceof LFPipe) {
      LFPipe lfpipe = (LFPipe) pipe;
      // go through all the antries in the instances data alphabet and try to figure out which
      // of the featuers are either boolean ore nominals coded as number
      for (int i = 0; i < dataAlph.size(); i++) {
        String malletFeatureName = (String) dataAlph.lookupObject(i);
        gate.plugin.learningframework.features.Attribute lfatt
                = FeatureExtraction.lookupAttributeForFeatureName(lfpipe.getFeatureInfo().getAttributes(), malletFeatureName);
        Alphabet alphToUse = null;
        if (lfatt instanceof gate.plugin.learningframework.features.AttributeList) {
          if (((gate.plugin.learningframework.features.AttributeList) lfatt).datatype == Datatype.bool) {
            alphToUse = booleanAlph;
          } else {
            if (((gate.plugin.learningframework.features.AttributeList) lfatt).datatype == Datatype.nominal
                    && ((gate.plugin.learningframework.features.AttributeList) lfatt).codeas == CodeAs.number) {
              alphToUse = ((gate.plugin.learningframework.features.AttributeList) lfatt).alphabet;
            }
          }
        } else if (lfatt instanceof gate.plugin.learningframework.features.SimpleAttribute) {
          if (((gate.plugin.learningframework.features.SimpleAttribute) lfatt).datatype == Datatype.bool) {
            alphToUse = booleanAlph;
          } else {
            if (((gate.plugin.learningframework.features.SimpleAttribute) lfatt).datatype == Datatype.nominal
                    && ((gate.plugin.learningframework.features.SimpleAttribute) lfatt).codeas == CodeAs.number) {
              alphToUse = ((gate.plugin.learningframework.features.SimpleAttribute) lfatt).alphabet;
            }
          }
        }
        // if alphToUse is not null, add it to the map
        if (alphToUse != null) {
          name2lfalph.put(malletFeatureName, alphToUse);
        }
      }
    }
    // This is the information weka needs about the attributes
    ArrayList<Attribute> wekaAttributes = new ArrayList<Attribute>();
    // now go through the data alphabet again and add one weka attribute to the attributes list
    // for each mallet feature. If we know an alphabet for the mallet feature, create the 
    // weka attribute as a nominal otherwise as a numeric weka attribute.
    for (int i = 0; i < pipe.getDataAlphabet().size(); i++) {
      String malletFeatureName = (String) pipe.getDataAlphabet().lookupObject(i);
      Alphabet lfalph = name2lfalph.get(malletFeatureName);
      if (lfalph == null) {
        wekaAttributes.add(new Attribute(malletFeatureName));
      } else {
        List<String> nomVals = new ArrayList<String>(lfalph.size());
        for (int j = 0; j < lfalph.size(); j++) {
          nomVals.add((String) lfalph.lookupObject(j));
        }
        wekaAttributes.add(new Attribute(malletFeatureName, nomVals));
      }
    }
    // now add the class attribute: if there is a target alphabet, the class must be nominal,
    // so create a nominal weka attribute, otherwise, create a numeric one
    weka.core.Attribute classAttr;
    if (pipe.getTargetAlphabet() != null) {
      Alphabet talph = pipe.getTargetAlphabet();
      // create the values for the target from the target alphabet
      List<String> classVals = new ArrayList<String>();
      for (int i = 0; i < talph.size(); i++) {
        classVals.add((String) talph.lookupObject(i));
      }
      classAttr = new Attribute("class", classVals);
      wekaAttributes.add(classAttr);
    } else {
      classAttr = new Attribute("target");
      wekaAttributes.add(classAttr);
    }
    // create the weka instances
    Instances insts = new weka.core.Instances("GATELearningFramework", wekaAttributes, malletInstances.size());
    insts.setClass(classAttr);
    int wekaTargetIndex = wekaAttributes.indexOf(classAttr);

    //////
    // Now convert the mallet instances to the weka instances
    /////
    for (cc.mallet.types.Instance malletInstance : malletInstances) {
      // NOTE: once we implement MalletSeq, each instance itself is a sequence of instances,
      // so we have to loop over those and over the array of targets too!

      // for each instance, we need an array of values and an array of indices,
      // and the size of those is the number of features in the mallet instance
      // NOTE: the instance and target
      FeatureVector fv = (FeatureVector) malletInstance.getData();
      int size = fv.numLocations();
      // TODO: for now we just directly copy over the mallet values to the weka values
      // We may need to handle certain cases with missing values separately!

      // create  the arrays with one more entry which will be the target!
      int indices[] = new int[size + 1];
      double values[] = new double[size + 1];
      for (int i = 0; i < size; i++) {
        indices[i] = fv.indexAtLocation(i);
        values[i] = fv.valueAtLocation(i);
      }
      // now set the target
      indices[size] = wekaTargetIndex;
      values[size] = (double) malletInstance.getTarget();

      weka.core.SparseInstance wekaInstance = new weka.core.SparseInstance(1.0, values, indices, values.length);
      wekaInstance.setDataset(insts);
      insts.add(wekaInstance);

    }

    return insts;
  }

}
