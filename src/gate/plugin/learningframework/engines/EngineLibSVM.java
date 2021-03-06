/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.learningframework.engines;

import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import gate.Annotation;
import gate.AnnotationSet;
import gate.plugin.learningframework.GateClassification;
import gate.plugin.learningframework.data.CorpusRepresentationLibSVM;
import gate.plugin.learningframework.data.CorpusRepresentationMalletTarget;
import gate.plugin.learningframework.mallet.LFPipe;
import gate.util.GateRuntimeException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import libsvm.svm;
import static libsvm.svm.svm_set_print_string_function;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

/**
 *
 * @author Johann Petrak
 */
public class EngineLibSVM extends Engine {

  @Override
  public Object evaluateHoldout(InstanceList instances, double portion, int reapeats, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Object evaluateXVal(InstanceList instances, int k, String parms) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void loadModel(File directory, String parms) {
    try {
      svm_model svmModel = svm.svm_load_model(new File(directory, FILENAME_MODEL).getAbsolutePath());
      System.out.println("Loaded LIBSVM model, nrclasses=" + svmModel.nr_class);
      model = svmModel;
    } catch (Exception ex) {
      throw new GateRuntimeException("Error loading the LIBSVM model", ex);
    }
  }

  @Override
  public void trainModel(String parms) {
    // 1) calculate the default parameter values that depend on the data
    int nrIndepFeatures = corpusRepresentationMallet.getRepresentationMallet().getDataAlphabet().size();
    double defaultGamma = 1.0 / nrIndepFeatures;

    // 2) create all the necessary parameters. In addition to the libsvm parameters documented here
    // https://www.csie.ntu.edu.tw/~cjlin/libsvm/
    // we also support the additional parameter -S/seed <integer> to set the random seed (default is 1)
    Parms ps = new Parms(parms, "s:svm_type:i", "t:kernel_type:i", "d:degree:i", "g:gamma:d",
            "r:coef0:d", "c:cost:d", "n:nu:d", "e:epsilon:d", "m:cachesize:i", "h:shrinking:i",
            "b:probability_estimates:i", "S:seed:i");
    svm_parameter svmparms = new svm_parameter();
    // we use 0 as the default for classification
    svmparms.svm_type = (int) ps.getValueOrElse("svm_type", 0);
    
    // immediately check if the svm type is compatible with regression or classification 
    // as it was selected
    if(algorithm instanceof AlgorithmRegression) {
      Integer algType = (Integer)ps.getValue("svm_type");
      // if the parameter is not give at all, we use 3 as the default for regression
      if(algType == null) svmparms.svm_type = 3;
      if(svmparms.svm_type != 3 && svmparms.svm_type != 4) {
        throw new GateRuntimeException("SvmLib: only -s 3 or -s 4 allowed for regression");
      }
    } else {
      if(svmparms.svm_type != 0 && svmparms.svm_type != 1) {
        throw new GateRuntimeException("SvmLib: only -s 0 or -s 1 allowed for classification");
      }      
    }
    
    svmparms.kernel_type = (int) ps.getValueOrElse("kernel_type", 2);
    svmparms.degree = (int) ps.getValueOrElse("degree", 3);
    svmparms.gamma = (double) ps.getValueOrElse("gamma", defaultGamma);
    svmparms.coef0 = (double) ps.getValueOrElse("coef0", 0.0);
    svmparms.C = (double) ps.getValueOrElse("cost", 1.0);
    svmparms.nu = (double) ps.getValueOrElse("nu", 0.5);
    svmparms.eps = (double) ps.getValueOrElse("epsilon", 0.1);
    svmparms.cache_size = (int) ps.getValueOrElse("cachesize", 100);
    svmparms.shrinking = (int) ps.getValueOrElse("shrinking", 1);
    svmparms.probability = (int) ps.getValueOrElse("probability_estimates", 1); // THIS ONE DIFFERS FROM SVMLIB DEFAULT!
    int seed = (int) ps.getValueOrElse("seed", 1);
    // for the weights, we need a different strategy: our Parms class cannot parse arbitrary 
    // numbered options so we have to do it ourselves here
    List<Double> weights = new ArrayList<Double>();
    List<Integer> featureNumbers = new ArrayList<Integer>();
    // make sure we have a parameter at all before trying to parse it
    if (parms != null && !parms.isEmpty()) {
      String[] tokens = parms.split("\\s+", -1);
      for (int i = 0; i < tokens.length - 1; i++) {
        String token = tokens[i];
        if (token.startsWith("-w")) {
          // this should be a weight parameter: we only use it if it really only contains a number
          // in the option name and does have something that can be parsed as a double as its value,
          // otherwise we simply ignore
          if (token.substring(2).matches("[0-9]+")) {
            String valueString = tokens[i + 1];
            Double value = Double.NaN;
            try {
              value = Double.parseDouble(valueString);
            } catch (Exception ex) {
              // ignore this
            }
            if (value != Double.NaN) {
              int fn = Integer.parseInt(token.substring(2));
              if (fn < nrIndepFeatures) {
                featureNumbers.add(fn);
                weights.add(value);
              }
            }
          }
        }
      } // for int=0; i<tokens.length

      // now actually set the libsvm parms. Java cannot directly convert a collection of Double
      // to an array of double, so we do this manually.
      if (weights.size() > 0) {
        double[] ws = new double[weights.size()];
        int[] idxs = new int[weights.size()];
        for (int i = 0; i < weights.size(); i++) {
          ws[i] = weights.get(i);
          idxs[i] = featureNumbers.get(i);
        }
        svmparms.weight = ws;
        svmparms.weight_label = idxs;
      }
    }
    System.err.println("SVM parms used: "+libsvmParmsAsString(svmparms));
    svm_set_print_string_function(new svm_print_interface() {
      @Override
      public void print(String string) {
        System.err.print(string);
      }
    });

    libsvm.svm.rand.setSeed(seed);

    // convert the mallet instances to svm problem. For this we can simply use the static method,
    // no need really to create an instance of CorpusRepresentationLibSVM for now
    svm_problem svmprob = CorpusRepresentationLibSVM.getFromMallet(corpusRepresentationMallet);

    svm_model svmModel = libsvm.svm.svm_train(svmprob, svmparms);
    model = svmModel;
  }

  @Override
  public List<GateClassification> classify(
          AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    CorpusRepresentationMalletTarget data = (CorpusRepresentationMalletTarget) corpusRepresentationMallet;
    data.stopGrowth();
    // try to figure out if we have regression or classification:
    LFPipe pipe = (LFPipe) data.getPipe();
    Alphabet talph = pipe.getTargetAlphabet();
    int numberOfLabels = 0;
    if (talph == null) {
      // regression
    } else {
      // classification
      numberOfLabels = talph.size();
    }
    svm_model svmModel = (svm_model) model;
    // iterate over all the mallet instances
    List<GateClassification> gcs = new ArrayList<GateClassification>();
    for (Annotation instAnn : instanceAS.inDocumentOrder()) {
      Instance malletInstance = data.extractIndependentFeatures(instAnn, inputAS);
      malletInstance = pipe.instanceFrom(malletInstance);
      svm_node[] svmInstance = CorpusRepresentationLibSVM.libSVMInstanceIndepFromMalletInstance(malletInstance);
      int bestLabel = (new Double(svm.svm_predict(svmModel, svmInstance))).intValue();

      double bestConf = 0.0;

      // TODO: not sure how to handle regression models here, so far this works only with
      // classification!?!
      if(algorithm instanceof AlgorithmRegression) {
        double prediction = svm.svm_predict(svmModel, svmInstance);
        GateClassification gc = new GateClassification(instAnn, prediction);
        gcs.add(gc);
      } else {
        if (svm.svm_check_probability_model(svmModel) == 1) {
          double[] confidences = new double[numberOfLabels];
          double v = svm.svm_predict_probability(svmModel, svmInstance, confidences);
          bestConf = confidences[bestLabel];
        } else {
          double[] confidences = new double[numberOfLabels * (numberOfLabels - 1) / 2];
          svm.svm_predict_values(svmModel, svmInstance, confidences);
          //For now we are not providing decision values for non-prob
          //models because it is complex, see here: 
          //http://www.csie.ntu.edu.tw/~r94100/libsvm-2.8/README
        }

        String labelstr = (String) pipe.getTargetAlphabet().lookupObject(bestLabel);
        GateClassification gc = new GateClassification(
              instAnn, labelstr, bestConf);
        gcs.add(gc);
      }

    }
    data.startGrowth();
    return gcs;
  }

  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    // we always use a predefined class to train, so not really necessary to do antything here.
  }

  @Override
  public void saveModel(File directory) {
    try {
      svm.svm_save_model(new File(directory, FILENAME_MODEL).getAbsolutePath(), (svm_model) model);
    } catch (Exception e) {
      throw new GateRuntimeException("Error saving LIBSVM model", e);
    }
  }

  @Override
  protected void loadMalletCorpusRepresentation(File directory) {
    corpusRepresentationMallet = CorpusRepresentationMalletTarget.load(directory);
  }
  
  private String libsvmParmsAsString(libsvm.svm_parameter parms) {
    StringBuilder sb = new StringBuilder();
    sb.append("svmparms{");
    sb.append("C=");
    sb.append(parms.C);
    sb.append(",cache_size=");
    sb.append(parms.cache_size);
    sb.append(",coef0=");
    sb.append(parms.coef0);
    sb.append(",degree=");
    sb.append(parms.degree);
    sb.append(",eps=");
    sb.append(parms.eps);
    sb.append(",gamma=");
    sb.append(parms.gamma);
    sb.append(",kernel_type=");
    sb.append(parms.kernel_type);
    sb.append(",nr_weight=");
    sb.append(parms.nr_weight);
    sb.append(",nu=");
    sb.append(parms.nu);
    sb.append(",p=");
    sb.append(parms.p);
    sb.append(",probability=");
    sb.append(parms.probability);
    sb.append(",shrinking=");
    sb.append(parms.shrinking);
    sb.append(",svm_type=");
    sb.append(parms.svm_type);
    sb.append(",weight=");
    if(parms.weight!=null)
      for(int i = 0; i<parms.weight.length; i++) {
        if(i!=0) sb.append(",");
        sb.append(parms.weight[i]);
      }
    sb.append(",weight_label=");
    if(parms.weight_label != null)
    for(int i = 0; i<parms.weight_label.length; i++) {
      if(i!=0) sb.append(",");
      sb.append(parms.weight_label[i]);
    }
    sb.append("}");
    return sb.toString();
  }

}
