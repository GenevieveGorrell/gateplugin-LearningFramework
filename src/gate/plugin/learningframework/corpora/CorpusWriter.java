/*
 * CorpusWriter.java
 *  
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright 2015 South London and Maudsley NHS Trust and King's College London
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 *
 * Genevieve Gorrell, 9 Jan 2015
 */
package gate.plugin.learningframework.corpora;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import cc.mallet.types.FeatureVector;
import cc.mallet.types.InstanceList;
import gate.Document;
import gate.plugin.learningframework.Mode;
import gate.plugin.learningframework.ScalingMethod;
import java.util.ArrayList;

public abstract class CorpusWriter {

  static final Logger logger = Logger.getLogger("CorpusWriter");

  /**
   * The annotation set from which to draw the annotations.
   */
  private String inputASName;

  /**
   * The annotation type to be treated as instance.
   */
  private String instanceName;

  private PrintStream outputStream;

  protected ScalingMethod scaleFeatures;

  public void setOutputDirectory(File outputFile) {
    this.outputDirectory = outputFile;
  }

  public File getOutputDirectory() {
    return this.outputDirectory;
  }

  public void setInputASName(String iasn) {
    this.inputASName = iasn;
  }

  public String getInputASName() {
    return this.inputASName;
  }

  public void setInstanceName(String inst) {
    this.instanceName = inst;
  }

  public String getInstanceName() {
    return this.instanceName;
  }

  private FeatureSpecification conf = null;

  private File outputDirectory;

  private Mode mode;

  private String classType;

  private String classFeature;

  private String identifierFeature;

  static String outputfilenamearff = "output.arff";
  static String outputfilenamearffpipe = "output-thru-pipe.arff";
  static String pipefilenamearff = "my.pipe";

  String outputfile = outputfilenamearff;

  public CorpusWriter(FeatureSpecification conf, String inst, String inpas,
          File outputDirectory, Mode mode, String classType, String classFeature,
          String identifierFeature, ScalingMethod scaleFeatures) {
    this.conf = conf;
    this.instanceName = inst;
    this.inputASName = inpas;
    this.outputDirectory = outputDirectory;
    this.mode = mode;
    this.classType = classType;
    this.classFeature = classFeature;
    this.identifierFeature = identifierFeature;
    this.scaleFeatures = scaleFeatures;
  }

  public void initializeOutputStream(String file) {
    outputDirectory.mkdirs();

    File output = new File(outputDirectory, file);
    if (output.exists()) {
      output.delete();
    }
    try {
      output.createNewFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      outputStream = new PrintStream(new FileOutputStream(output, true));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static FeatureVector[] trim(FeatureVector[] array) {
    if (array.length > 0 && array[0] == null) {
      FeatureVector[] arraytoreturn = new FeatureVector[array.length - 1];
      for (int i = 1; i < array.length; i++) {
        arraytoreturn[i - 1] = array[i];
      }
      return trim(arraytoreturn);
    } else {
      return array;
    }
  }

  public abstract void add(Document doc);

  public void conclude() {
    System.out.println("DEBUG running conclude, class writer is " + this.getClass());
    if (scaleFeatures != ScalingMethod.NONE) {
      System.out.println("DEBUG LearningFramework: scaling features using " + scaleFeatures);
      normalize();
    }
  }

  public void normalize() {
    //We need to add the scaling step to the pipe.
    //System.out.println("DEBUG normalize: instances="+instances);
    //System.out.println("DEBUG normalize: instances="+getInstances());
    System.out.println("DEBUG normalize: getDataAlphabet=" + instances.getDataAlphabet());
    System.out.println("DEBUG normalize: size=" + instances.getDataAlphabet().size());
    double[] sums = new double[instances.getDataAlphabet().size()];
    double[] sumsofsquares = new double[instances.getDataAlphabet().size()];
    //double[] numvals = new double[instances.getDataAlphabet().size()];
    double[] means = new double[instances.getDataAlphabet().size()];
    double[] variances = new double[instances.getDataAlphabet().size()];

    for (int i = 0; i < instances.size(); i++) {
      FeatureVector data = (FeatureVector) instances.get(i).getData();
      int[] indices = data.getIndices();
      double[] values = data.getValues();
      for (int j = 0; j < indices.length; j++) {
        int index = indices[j];
        double value = values[j];
        sums[index] += value;
        sumsofsquares[index] += (value * value);
        //numvals[index]++;
      }
    }

    //Now use the accumulators to prepare means and variances
    //for each feature in the alphabet, to be used for scaling.
    for (int i = 0; i < sums.length; i++) {
      means[i] = sums[i] / instances.getDataAlphabet().size();
      variances[i] = sumsofsquares[i] / instances.getDataAlphabet().size();
    }

    //We make a new pipe and apply it to all the instances
    FeatureVector2NormalizedFeatureVector normalizer
            = new FeatureVector2NormalizedFeatureVector(means, variances, instances.getAlphabet());
    InstanceList newInstanceList = new InstanceList(normalizer);
    for (int i = 0; i < instances.size(); i++) {
      newInstanceList.addThruPipe(instances.get(i));
    }
    instances = newInstanceList;

    //Add the pipe to the pipes so application time data will go through it
    ArrayList<Pipe> pipeList = pipe.pipes();
    pipeList.add(normalizer);
    System.out.println("DEBUG normalize: added normalizer pipe " + normalizer);
    pipe = new SerialPipes(pipeList);
    System.out.println("DEBUG pipes after normalization: " + pipe);
  }

  public PrintStream getOutputStream(String file) {
    return outputStream;
  }

  public void setOutputStream(PrintStream outputStream) {
    this.outputStream = outputStream;
  }

  public FeatureSpecification getConf() {
    return conf;
  }

  public void setConf(FeatureSpecification conf) {
    this.conf = conf;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public String getClassType() {
    return classType;
  }

  public void setClassType(String classType) {
    this.classType = classType;
  }

  public String getClassFeature() {
    return classFeature;
  }

  public void setClassFeature(String classFeature) {
    this.classFeature = classFeature;
  }

  public String getIdentifierFeature() {
    return identifierFeature;
  }

  public void setIdentifierFeature(String identifierFeature) {
    this.identifierFeature = identifierFeature;
  }

  protected SerialPipes pipe;

  public SerialPipes getPipe() {
    return pipe;
  }

  public void setPipe(SerialPipes pipe) {
    this.pipe = pipe;
  }

  protected InstanceList instances;

  public InstanceList getInstances() {
    return instances;
  }

  public void setInstances(InstanceList is) {
    instances = is;
  }

}
