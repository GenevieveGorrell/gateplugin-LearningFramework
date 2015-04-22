/*
 * SemanticModelingPR.java
 *  
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 *
 * Genevieve Gorrell, 3 Mar 2015
 */

package gate.learningframework.semanticmodeling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;







//import cc.mallet.pipe.Pipe;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vsm.VectorSpaceModel;
import gate.AnnotationSet;
import gate.Annotation;
import gate.Controller;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.Utils;
import gate.creole.ControllerAwarePR;
import gate.creole.ResourceInstantiationException;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.learningframework.corpora.CorpusWriter;
import gate.learningframework.corpora.CorpusWriterSspace;
import gate.util.InvalidOffsetException;

/**
 * <p>Semantic Modeling in GATE.</p>
 */

@CreoleResource(name = "Semantic Modeling PR", comment = "Wraps a number of methods of building a semantic"
		+ " model from a corpus and using it to transform an instance at apply time into a semantic vector"
		+ " that can be used in a variety of ways (visualization, comparison).")
public class SemanticModelingPR extends AbstractLanguageAnalyser implements
ProcessingResource,
Serializable, ControllerAwarePR {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static final Logger logger = Logger.getLogger("SemanticModelingPR");

	/**
	 * The directory to which the semantic model will be saved.
	 * 
	 */
	private java.net.URL saveDirectory;

	/**
	 * The name of the annotation set to be used as input.
	 * 
	 */
	private String inputASName;

	/**
	 * The name of the output annotation set.
	 * 
	 */
	//private String outputASName;

	/**
	 * The name of the output feature.
	 * 
	 */
	//private String outputFeature;

	/**
	 * The annotation type to be treated as instance. Leave blank to use the document as instance.
	 * 
	 */
	private String instanceType;

	/**
	 * The annotation feature to be treated as instance. Leave blank to use the string as instance.
	 * 
	 */
	private String instanceFeature;

	/**
	 * The semantic model to be used, for example LDA
	 * 
	 */
	private ModelType modelType;

	/**
	 * The name for the saved semantic model file name
	 * 
	 */
	private String modelFileName = "my.model";

	private SemanticModel semanticModel = null;

	@RunTime
	@CreoleParameter(comment = "The directory to which the semantic model will be saved.")
	public void setSaveDirectory(URL output) {
		this.saveDirectory = output;
	}

	public URL getSaveDirectory() {
		return this.saveDirectory;
	}

	/*@RunTime
	@CreoleParameter(comment = "The name for the semantic model to be saved as.")
	public void setModelFileName(String mfn) {
		this.modelFileName = mfn;
	}

	public String getModelFileName() {
		return this.modelFileName;
	}*/


	@RunTime
	@Optional
	@CreoleParameter
	public void setInputASName(String iasn) {
		this.inputASName = iasn;
	}

	public String getInputASName() {
		return this.inputASName;
	}

	/*@RunTime
	@Optional
	@CreoleParameter(defaultValue = "")
	public void setOutputASName(String oasn) {
		this.outputASName = oasn;
	}

	public String getOutputASName() {
		return this.outputASName;
	}

	@RunTime
	@Optional
	@CreoleParameter(defaultValue = "")
	public void setOutputFeature(String of) {
		this.outputFeature = of;
	}

	public String getOutputFeature() {
		return this.outputFeature;
	}*/

	@RunTime
	@CreoleParameter(defaultValue = "Sentence", comment = "The annotation type to "
			+ "be treated as instance.")
	public void setInstanceType(String type) {
		this.instanceType = type;
	}

	public String getInstanceType() {
		return this.instanceType;
	}

	@RunTime
	@CreoleParameter(defaultValue = "Sentence", comment = "The annotation feature to "
			+ "be treated as instance. Leave blank to use string.")
	public void setInstanceFeature(String feat) {
		this.instanceFeature = feat;
	}

	public String getInstanceFeature() {
		return this.instanceFeature;
	}

	@RunTime
	@CreoleParameter(comment = "The model type to be prepared. Ignored at "
			+ "application time.")
	public void setModelType(ModelType modeltype) {
		this.modelType = modeltype;
	}

	public ModelType getModelType() {
		return this.modelType;
	}

	@Override
	public Resource init() throws ResourceInstantiationException {
		return this;
	}

	@Override
	public void execute() throws ExecutionException {	 
		Document doc = getDocument();

		AnnotationSet instances = doc.getAnnotations(inputASName).get(instanceType);
		for(Annotation instance: instances){
			String stringToAdd = "";
			if(instanceFeature!=null && !instanceFeature.isEmpty()){
				stringToAdd = instance.getFeatures().get(instanceFeature).toString();
			} else {
				stringToAdd = Utils.cleanStringFor(doc, instance);
			}
			stringToAdd = CorpusWriterSspace.prepare(stringToAdd);
			this.semanticModel.add(stringToAdd);
		}

	}

	@Override
	public synchronized void interrupt() {
		super.interrupt();
	}

	@Override
	public void controllerExecutionAborted(Controller arg0, Throwable arg1)
			throws ExecutionException {
		logger.warn("SemanticModeling: Execution aborted!");
	}

	@Override
	public void controllerExecutionFinished(Controller arg0)
			throws ExecutionException {
		semanticModel.conclude();				
		logger.info("Semantic Modeling: Preparation complete!");	
	}

	@Override
	public void controllerExecutionStarted(Controller arg0)
			throws ExecutionException {
		if(modelType==null){
			logger.warn("SemanticModeling: Please select a model type!");
			semanticModel=null;
			interrupt();
		} else {
			switch(modelType){
			case MALLET_LDA:
				//semanticModel = new SemanticModelMallet(savedModelDirectoryFile, modelFileName, false);
				break;
			case SEMSPACE_TFIDF:
				try {
					semanticModel = new SemanticModelSspace(new File(saveDirectory.getFile()), modelFileName, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
		
	}

}
