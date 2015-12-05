/*
 * EngineWeka.java
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

package gate.learningframework.classification;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import cc.mallet.pipe.Pipe;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomTree;
import weka.core.Instances;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.learningframework.corpora.CorpusWriter;
import gate.learningframework.corpora.CorpusWriterArff;
import gate.learningframework.corpora.CorpusWriterArffNumericClass;
import gate.learningframework.corpora.CorpusWriterMallet;
import gate.learningframework.corpora.FeatureSpecification;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.RandomForest;
import weka.core.OptionHandler;

public class EngineWeka extends Engine {

	private weka.classifiers.Classifier classifier = null;

	/**
	 * The name of the classifier location.
	 */
	private static String classifiername = "my.model";

	private String params;

	/**
	 * The name of the pipe location. Since we are using
	 * Mallet for feature prep but not for classification,
	 * we have to explicitly save the pipe rather than relying
	 * on Mallet to save it with the classifier.
	 */
	//private Pipe pipe = null;

	private static String pipename = "my.pipe";


	public EngineWeka(File savedModel, Mode mode, String params, String engine, boolean restore){	
		this(savedModel, mode, engine, restore);
		this.params = params;
	}

	public EngineWeka(File savedModel, Mode mode, String engine, boolean restore){
		this.setOutputDirectory(savedModel);
		this.setEngine(engine);
		this.setMode(mode);

		//Restore the classifier and the saved copy of the configuration file
		//from train time.
		if(restore){	
			this.loadClassifier();
		}
	}

	public void loadClassifier(){

		File clf = new File(this.getOutputDirectory(), classifiername);
		File pf = new File(this.getOutputDirectory(), pipename);
		if(clf.exists() && pf.exists()){
			try {
				ObjectInputStream ois =
						new ObjectInputStream (new FileInputStream(clf));
				classifier = (Classifier) ois.readObject();
                                System.out.println("Loaded Weka classifier "+classifier.getClass().getName());
				ois.close();
			} catch(Exception e){
				e.printStackTrace();
			}

			URL confURL = null;
			try {
				confURL = new URL(
						this.getOutputDirectory().toURI().toURL(), 
						Engine.getSavedConf());
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				this.setSavedConfFile(new FeatureSpecification(confURL));
			} catch (ResourceInstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			try {
				ObjectInputStream ois =
						new ObjectInputStream (new FileInputStream(pf));
				pipe = (Pipe) ois.readObject();
				ois.close();
        //System.out.println("Read pipe: "+pipe);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the right trainer depending on what the user specified.
	 * @return
	 */
	public Classifier getTrainer(){
		Classifier classifier = null;
		switch(this.getEngine()){
		case "WEKA_CL_NUM_ADDITIVE_REGRESSION":
			classifier = new AdditiveRegression();
			break;
		case "WEKA_CL_NAIVE_BAYES":
			classifier = new NaiveBayes();
			break;
		case "WEKA_CL_J48":
			classifier = new J48();
			break;
		case "WEKA_CL_RANDOM_TREE":
			classifier = new RandomTree();
			break;
		case "WEKA_CL_IBK":
			classifier = new IBk();
			break;
    case "WEKA_CL_LOGISTIC_REGRESSION":
      classifier = new Logistic();
      break;
		case "WEKA_CL_RANDOM_FOREST":
      classifier = new RandomForest();
      break;
		}
		if(params!=null && !params.isEmpty()){
			String[] p = params.split("\\s+");
			try {
				((OptionHandler)classifier).setOptions(p);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return classifier;
	}

	public void train(FeatureSpecification conf, CorpusWriter trainingCorpus){
		//Start by clearing out the previous saved model.
		File[] files = this.getOutputDirectory().listFiles();
		if(files!=null) {
			for(File f: files) {
				f.delete();
			}
		}

		weka.core.Instances instances = null;
		
		if(algorithm==Algorithm.WEKA_CL_NUM_ADDITIVE_REGRESSION){
			CorpusWriterArffNumericClass trMal = (CorpusWriterArffNumericClass)trainingCorpus;
			instances = trMal.getWekaInstances();
			pipe = trMal.getPipe();
    } else {
			CorpusWriterArff trArff = (CorpusWriterArff)trainingCorpus;
			instances = trArff.getWekaInstances();
			pipe = trArff.getPipe();
		}
		

		if(instances==null || instances.numInstances()<1){
			logger.warn("LearningFramework: No training instances!");
		} else {

			//Sanity check--what data do we have?
			//logger.info("LearningFramework: Instances: " + instances.numInstances());
			//logger.info("LearningFramework: Data labels: " + instances.numAttributes());
			//logger.info("LearningFramework: Target labels: " + instances.numClasses());

			this.classifier = this.getTrainer();

			if(this.classifier!=null){
				try {
					this.classifier.buildClassifier(instances);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//Save the classifier so we don't have to retrain if we
				//restart GATE
				try {
					ObjectOutputStream oos = new ObjectOutputStream
							(new FileOutputStream(this.getOutputDirectory()
									+ "/" + classifiername));
					oos.writeObject(classifier);
					oos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				//Save the pipe
				try {
					ObjectOutputStream oos = new ObjectOutputStream
							(new FileOutputStream(this.getOutputDirectory()
									+ "/" + pipename));
					oos.writeObject(pipe);
					oos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				//We also save a copy of the configuration file, so the user can
				//change their copy without stuffing up their ability to apply this
				//model. We also save some information necessary to restore the model.
				this.storeConf(conf);
				this.writeInfo(
						instances.numInstances(),
						instances.numAttributes(),
						instances.numClasses());
			}
		}
	}

	public List<GateClassification> classify(String instanceAnn, 
			String inputASname, Document doc){
		List<GateClassification> gcs = new ArrayList<GateClassification>();

		AnnotationSet inputAS = doc.getAnnotations(inputASname);

		List<Annotation> instanceAnnotations = inputAS.get(instanceAnn).inDocumentOrder();

		Iterator<Annotation> it = instanceAnnotations.iterator();
		
		//Every instance needs a Weka dataset. We can economize by just
		//making one, now. It's a kind of fake dataset just because
		//Weka wants to know what the feature set is.
		Instances dataset = null;
    if(algorithm == Algorithm.WEKA_CL_NUM_ADDITIVE_REGRESSION) {
		  dataset = CorpusWriterArffNumericClass.malletPipeToWekaDataset(pipe);
    } else {
			dataset = CorpusWriterArff.malletPipeToWekaDataset(pipe);
    }		
    // TODO JP: avoid iterator
		while(it.hasNext()){
			Annotation instanceAnnotation = it.next();
			
			cc.mallet.types.Instance malletInstance = null;

			malletInstance = 
					CorpusWriterMallet.instanceFromInstanceAnnotationNoClass(
					this.getSavedConfFile(), instanceAnnotation, inputASname, doc,
					this.getMode(), "");
			

			//Instance needs to go through the pipe for this classifier, so that
			//it gets mapped using the same alphabet, and the text is in the
			//expected format.
			malletInstance = pipe.instanceFrom(malletInstance);

			weka.core.Instance wekaInstance = 
					CorpusWriterArff.malletInstance2WekaInstanceNoTarget(
							malletInstance, dataset);
			if(algorithm == Algorithm.WEKA_CL_NUM_ADDITIVE_REGRESSION){
				try {
					double result = classifier.classifyInstance(wekaInstance);
					
					GateClassification gc = new GateClassification(
							instanceAnnotation, String.valueOf(result), 1.0);
	
					gcs.add(gc);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
       } else {
        
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
          predictionDistribution = classifier.distributionForInstance(wekaInstance);
        } catch (Exception ex) {
          ex.printStackTrace(System.err);
        }
        // This is classification, we should always get a distribution list > 1
        if(predictionDistribution.length < 2) {
          throw new RuntimeException("Classifier returned less than 2 probabilities: "+predictionDistribution.length+
                  "for instance"+wekaInstance);
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
				for(int i = 0; i < predictionDistribution.length; i++){
					int thislabel = i;
					double thisprob = predictionDistribution[i];
					String labelstr = (String)pipe.getTargetAlphabet().lookupObject(thislabel);
					classList.add(labelstr);
					confidenceList.add(thisprob);
					if(thisprob>bestprob){
						bestlabel = thislabel;
						bestprob = thisprob;
					}
				} // end for i < predictionDistribution.length
				
				String cl = 
						(String)pipe.getTargetAlphabet().lookupObject(bestlabel);
				
				GateClassification gc = new GateClassification(
						instanceAnnotation, cl, bestprob, classList, confidenceList);

				gcs.add(gc);
                        }
		}
		return gcs;
	}

	public void evaluateXFold(CorpusWriter evalCorpus, int folds){
		if(algorithm==Algorithm.WEKA_CL_NUM_ADDITIVE_REGRESSION){
			CorpusWriterArffNumericClass trArff = (CorpusWriterArffNumericClass)evalCorpus;
			Instances newData = trArff.getWekaInstances();
			try {
				Evaluation eval = new Evaluation(newData);
				eval.crossValidateModel(this.getTrainer(), newData, folds, new Random(1));
				logger.info("LearningFramework: " + folds
						+ " fold cross-validation correlation coefficient: " + eval.correlationCoefficient());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
                } else {
			CorpusWriterArff trArff2 = (CorpusWriterArff)evalCorpus;
			Instances newData2 = trArff2.getWekaInstances();
			try {
				Evaluation eval = new Evaluation(newData2);
				eval.crossValidateModel(this.getTrainer(), newData2, folds, new Random(1));
				logger.info("LearningFramework: " + folds
						+ " fold cross-validation accuracy: " 
						+ eval.correct()/eval.numInstances());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void evaluateHoldout(CorpusWriter evalCorpus, 
			float trainingproportion){
		if(algorithm == Algorithm.WEKA_CL_NUM_ADDITIVE_REGRESSION){
			CorpusWriterArffNumericClass trArff = (CorpusWriterArffNumericClass)evalCorpus;
			Instances all = trArff.getWekaInstances();
			Instances[] split = trArff.splitWekaInstances(all, trainingproportion);
			Classifier classifier = this.getTrainer();
			try {
				classifier.buildClassifier(split[0]);
				Evaluation eval;
				eval = new Evaluation(split[0]);
				eval.evaluateModel(classifier, split[1]);
				logger.info("LearningFramework: Holdout correlation "
						+ "coefficient training on " 
						+ trainingproportion + " of the data: "
						+ eval.correlationCoefficient());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
                } else {
			CorpusWriterArff trArff2 = (CorpusWriterArff)evalCorpus;
			Instances all2 = trArff2.getWekaInstances();
			Instances[] split2 = trArff2.splitWekaInstances(all2, trainingproportion);
			Classifier classifier2 = this.getTrainer();
			try {
				classifier2.buildClassifier(split2[0]);
				Evaluation eval;
				eval = new Evaluation(split2[0]);
				eval.evaluateModel(classifier2, split2[1]);
				logger.info("LearningFramework: Holdout accuracy training on " 
						+ trainingproportion + " of the data: "
						+ eval.correct()/(eval.correct() + eval.incorrect()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

        
        @Override
        public String whatIsItString() {
          if(algorithm == null) {
            return classifier.getClass().getCanonicalName();
          } else {
            return algorithm.toString();
          }
        }        
        
}
