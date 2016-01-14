/*
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright 2015 South London and Maudsley NHS Trust and King's College London
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 */
package gate.plugin.learningframework;

import gate.learningframework.classification.Operation;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import gate.AnnotationSet;
import gate.Annotation;
import gate.Controller;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.ControllerAwarePR;
import gate.creole.ResourceInstantiationException;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.learningframework.corpora.CorpusWriter;
import gate.plugin.learningframework.corpora.CorpusWriterArff;
import gate.plugin.learningframework.corpora.CorpusWriterArffNumericClass;
import gate.plugin.learningframework.corpora.CorpusWriterMallet;
import gate.plugin.learningframework.corpora.CorpusWriterMalletSeq;
import gate.plugin.learningframework.corpora.FeatureSpecification;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import libsvm.svm_problem;

/**
 * Export a training set in some tool-specific format.
 */
@CreoleResource(name = "LearningFrameworkExport", comment = "Export machine learning instances to files in various formats")
public class LearningFrameworkExport extends LearningFrameworkPRBase {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  static final Logger logger = Logger.getLogger(LearningFrameworkExport.class.getCanonicalName());

  protected java.net.URL featureSpecURL;

  @RunTime
  @CreoleParameter(comment = "The feature specification file.")
  public void setFeatureSpecURL(URL featureSpecURL) {
    if (!featureSpecURL.equals(this.featureSpecURL)) {
      this.featureSpecURL = featureSpecURL;
      this.conf = new FeatureSpecification(featureSpecURL);
    }
  }

  public URL getFeatureSpecURL() {
    return featureSpecURL;
  }


 
  @RunTime
  @CreoleParameter(defaultValue = "NONE", comment = "If and how to scale features. ")
  public void setScaleFeatures(ScalingMethod sf) {
    scaleFeatures = sf;
  }

  public ScalingMethod getScaleFeatures() {
    return scaleFeatures;
  }
  protected ScalingMethod scaleFeatures = ScalingMethod.NONE;

  private CorpusWriter exportCorpus = null;

  private FeatureSpecification conf = null;


  @Override
  public void execute(Document doc) {
    exportCorpus.add(doc);
  }

  @Override
  public void afterLastDocument(Controller arg0, Throwable t) {
    
     // TODO!!!
    /**
      case EXPORT_ARFF:
      case EXPORT_ARFF_THRU_CURRENT_PIPE:
      case EXPORT_ARFF_NUMERIC_CLASS:
      case EXPORT_ARFF_NUMERIC_CLASS_THRU_CURRENT_PIPE:
        exportCorpus.conclude();
        ((CorpusWriterArff) exportCorpus).writeToFile();
        break;
      case EXPORT_LIBSVM:
        // JP: this should get moved to some better place
        // JP: I have no idea if conclude works properly here! CHECK!
        exportCorpus.conclude();
        svm_problem prob = ((CorpusWriterMallet) exportCorpus).getLibSVMProblem();
        PrintStream out = null;
        File savedir = gate.util.Files.fileFromURL(saveDirectory);
        File expdir = new File(savedir, "exportedLibSVM");
        expdir.mkdir();
        try {
          out = new PrintStream(new File(expdir, "data.libsvm"));
          for (int i = 0; i < prob.l; i++) {
            out.print(prob.y[i]);
            for (int j = 0; j < prob.x[i].length; j++) {
              out.print(" ");
              out.print(prob.x[i][j].index);
              out.print(":");
              out.print(prob.x[i][j].value);
            }
            out.println();
          }
          out.close();
        } catch (FileNotFoundException ex) {
          System.err.println("Could not write training instances to svm format file");
          ex.printStackTrace(System.out);
        }
        try {
          ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(expdir
                  + "/my.pipe"));
          oos.writeObject(exportCorpus.getInstances().getPipe());
          oos.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
    */
  }

  @Override
  protected void beforeFirstDocument(Controller c) {
    File savedModelDirectoryFile = new File(
            gate.util.Files.fileFromURL(dataDirectory), Globals.savedModelDirectory);
    conf = new FeatureSpecification(featureSpecURL);
    
    // TODO
    /*
      case EXPORT_LIBSVM:
        File trainfilemallet = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);
        exportCorpus = new CorpusWriterMallet(conf, instanceType,
                inputASName, trainfilemallet, mode, classType,
                targetFeature, identifierFeature, scaleFeatures);
        break;
      case EXPORT_ARFF:
        File outputfilearff = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);
        exportCorpus = new CorpusWriterArff(this.conf, this.instanceType, this.inputASName,
                outputfilearff, mode, classType, targetFeature, identifierFeature, null,
                scaleFeatures);
        break;
      case EXPORT_ARFF_THRU_CURRENT_PIPE:
        File outputfilearff2 = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);

        if (CorpusWriterArff.getArffPipe(outputfilearff2) == null) {
          logger.warn("LearningFramework: No pipe found in corpus output directory! "
                  + "Begin by exporting arff training data without using pipe "
                  + "so as to create one which you can then use to export test "
                  + "data.");
          break;
        }

        exportCorpus = new CorpusWriterArff(this.conf, this.instanceType, this.inputASName,
                outputfilearff2, mode, classType, targetFeature, identifierFeature,
                CorpusWriterArff.getArffPipe(outputfilearff2), scaleFeatures);
        break;
      case EXPORT_ARFF_NUMERIC_CLASS:
        File outputfilearff3 = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);
        exportCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceType, this.inputASName,
                outputfilearff3, mode, classType, targetFeature, identifierFeature, null, scaleFeatures);
        break;
      case EXPORT_ARFF_NUMERIC_CLASS_THRU_CURRENT_PIPE:
        File outputfilearff4 = new File(
                gate.util.Files.fileFromURL(dataDirectory), corpusoutputdirectory);

        if (CorpusWriterArff.getArffPipe(outputfilearff4) == null) {
          logger.warn("LearningFramework: No pipe found in corpus output directory! "
                  + "Begin by exporting arff training data without using pipe "
                  + "so as to create one which you can then use to export test "
                  + "data.");
          break;
        }

        exportCorpus = new CorpusWriterArffNumericClass(this.conf, this.instanceType, this.inputASName,
                outputfilearff4, mode, classType, targetFeature, identifierFeature,
                CorpusWriterArff.getArffPipe(outputfilearff4), scaleFeatures);
        break;
    */
  }
  
  public void finishedNoDocument(Controller arg0, Throwable throwable) {
    // no need to do anything
  }
  

}
