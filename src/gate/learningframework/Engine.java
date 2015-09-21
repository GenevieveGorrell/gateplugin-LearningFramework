/*
 * Engine.java
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

package gate.learningframework;

//import static gate.learningframework.Engine.classifiername;
//import static gate.learningframework.Engine.pipename;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import weka.classifiers.Classifier;
import cc.mallet.pipe.Pipe;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;

public abstract class Engine {

	static final Logger logger = Logger.getLogger("Engine");
	
	private String engine;
	
	public static String info = "info";

	private static String savedConf = "conf";
	
	private FeatureSpecification savedConfFile;
	
	private File outputDirectory;
	
	private Mode mode;

	/**
	 * The name of the classifier location.
	 */
	private static String classifiername = new String("my.classifier");
	
	/**
	 * The name of the pipe location. Since we are using
	 * Mallet for feature prep but not always for classification,
	 * we have to explicitly save the pipe rather than relying
	 * on Mallet to save it with the classifier. In any case
	 * the version saved with the classifier doesn't always seem
	 * reliable.
	 */
	public Pipe pipe = null;

	private static String pipename = new String("my.pipe");

	public Object loadClassifier(){
		File clf = new File(this.getOutputDirectory(), classifiername);
		File pf = new File(this.getOutputDirectory(), pipename);
		Object classifier = null;
		if(clf.exists() && pf.exists()){
			try {
				ObjectInputStream ois =
						new ObjectInputStream (new FileInputStream(clf));
				classifier = ois.readObject();
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
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		return classifier;
	}

	public void save(FeatureSpecification conf, Object classifier,
			int exnum, int dataalph, int targalph, Pipe pipe){
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

		//Save the pipe explicitly with all engines, for greater control
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
		this.writeInfo(exnum, dataalph, targalph);
		this.pipe = pipe;
	}
	
	public abstract void train(FeatureSpecification conf, CorpusWriter trainingCorpus);
	
	//public abstract List<GateClassification> classify(
	//		List<Annotation> instanceAnnotations, 
	//		AnnotationSet inputAS, Document doc);
	
	public abstract Algorithm whatIsIt();
	
	public abstract void evaluateXFold(CorpusWriter evalCorpus, int folds);

	public abstract void evaluateHoldout(CorpusWriter evalCorpus, float trainingproportion);
	
	public void writeInfo(int exnum, int dataalph, int targalph){
		try {
			FileWriter fw = new FileWriter(new File(outputDirectory, info));
			fw.write(this.whatIsIt() + "\n" + new Date() + "\n");
			fw.write(this.getMode().toString() + "\n");
			fw.write(exnum + " instances\n");
			fw.write(dataalph + " features\n");
			fw.write(targalph + " labels\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Store a copy of the configuration file used at training time with the
	 * saved model. This means that if the user changes their configuration file,
	 * it won't stuff up their ability to use their saved model.
	 * @param conf
	 */
	public void storeConf(FeatureSpecification conf){
		File confFile = new File(this.getOutputDirectory(), Engine.getSavedConf());

		try {
			FileUtils.copyFile(new File(conf.getUrl().getFile()), confFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		URL confURL = null;
		try {
			confURL = confFile.toURI().toURL();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(confURL!=null){
			try {
				this.setSavedConfFile(new FeatureSpecification(confURL));
			} catch (ResourceInstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Use the info file to correctly restore the learner from the saved
	 * model directory.
	 * @param savedModelDirectoryFile
	 * @return
	 */
	public static Engine restoreLearner(File savedModelDirectoryFile){
		//Restore previously trained model
		File infofile = new File(savedModelDirectoryFile, Engine.info);
		Engine learner = null;

		if(infofile.exists()){

			List<String> infostrings = null;
			try {
				infostrings = Files.readAllLines(
						infofile.toPath(), Charset.forName("UTF-8"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(infostrings!=null && infostrings.size()>2){
				String engine = infostrings.get(0).trim();
				Algorithm algo = Algorithm.valueOf(engine);
				String modestr = infostrings.get(2).trim();
				Mode mode = Mode.valueOf(modestr);
				switch(algo){
				case LIBSVM:
					learner = new EngineLibSVM(savedModelDirectoryFile, mode, true);
					break;
				case MALLET_CL_C45:
				case MALLET_CL_DECISION_TREE:
				case MALLET_CL_MAX_ENT:
				case MALLET_CL_NAIVE_BAYES_EM:
				case MALLET_CL_NAIVE_BAYES:
				case MALLET_CL_WINNOW:
					learner = new EngineMallet(savedModelDirectoryFile, mode, engine, true);
					break;
				case MALLET_SEQ_CRF:
					learner = new EngineMalletSeq(savedModelDirectoryFile, mode, true);
					break;
				case WEKA_CL_NUM_ADDITIVE_REGRESSION:
				case WEKA_CL_NAIVE_BAYES:
				case WEKA_CL_J48:
				case WEKA_CL_RANDOM_TREE:
				case WEKA_CL_IBK:
				case WEKA_CL_MULTILAYER_PERCEPTRON:
				case WEKA_CL_JRIP:
				case WEKA_CL_NBTREE:
				case WEKA_CL_RANDOM_FOREST:
					learner = new EngineWeka(savedModelDirectoryFile, mode, engine, true);
					break;
				}
			}
		} else {
			//No learner to restore
		}
		
		return learner;
	}

	public String getInfo() {
		return info;
	}

	public void setOutputDirectory(File outputDirectory){
		this.outputDirectory = outputDirectory;
	}
	
	public File getOutputDirectory(){
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}
		return this.outputDirectory;
	}
	
	public String getEngine() {
		return engine;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public static String getSavedConf() {
		return savedConf;
	}

	public FeatureSpecification getSavedConfFile() {
		return savedConfFile;
	}

	public void setSavedConfFile(FeatureSpecification savedConfFile) {
		this.savedConfFile = savedConfFile;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}
}
