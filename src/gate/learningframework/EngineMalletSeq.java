/*
 * EngineMalletSeq.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFOptimizableByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByValueGradients;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.optimize.Optimizable;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelVector;
import cc.mallet.types.InstanceList.CrossValidationIterator;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Utils;
import gate.creole.ResourceInstantiationException;

public class EngineMalletSeq extends Engine {

	private CRF crf;

	public EngineMalletSeq(File savedModel, Mode mode, boolean restore){
		this.setOutputDirectory(savedModel);
		this.setMode(mode);

		if(restore){
			this.crf = (CRF)this.loadClassifier();
		}

	}

	public void train(FeatureSpecification conf, CorpusWriter trainingCorpus){
		//Start by clearing out the saved model directory
		File[] files = this.getOutputDirectory().listFiles();
		if(files!=null) {
			for(File f: files) {
				f.delete();
			}
		}

		CorpusWriterMalletSeq tc = (CorpusWriterMalletSeq)trainingCorpus;
		InstanceList trainingData = tc.getInstances();

		//Sanity check--how does the data look?
		logger.info("LearningFramework: Instances: " + trainingData.size());
		logger.info("LearningFramework: Data labels: " + trainingData.getDataAlphabet().size());
		logger.info("LearningFramework: Target labels: " + trainingData.getTargetAlphabet().size());

		//Including the pipe at this stage means we have it available to
		//put data through at apply time.
		crf = new CRF(trainingData.getPipe(), null);
		
		// construct the finite state machine
		crf.addFullyConnectedStatesForLabels();
		// initialize model's weights
		crf.setWeightsDimensionAsIn(trainingData, false);

		//  CRFOptimizableBy* objects (terms in the objective function)
		// objective 1: label likelihood objective
		CRFOptimizableByLabelLikelihood optLabel =
				new CRFOptimizableByLabelLikelihood(crf, trainingData);

		// CRF trainer
		Optimizable.ByGradientValue[] opts =
				new Optimizable.ByGradientValue[]{optLabel};
		// by default, use L-BFGS as the optimizer
		CRFTrainerByValueGradients crfTrainer = new CRFTrainerByValueGradients(crf, opts);

		// all setup done, train until convergence
		crfTrainer.setMaxResets(0);
		crfTrainer.train(trainingData, Integer.MAX_VALUE);

		this.save(conf, this.crf, trainingData.size(),
				trainingData.getDataAlphabet().size(),
				trainingData.getTargetAlphabet().size(), trainingData.getPipe());
	}

	/**
	 * Classify takes the entire document and breaks it down into
	 * sequence instances using sequenceSpan. It returns the result
	 * as a list of GateClassifications.
	 * @param instanceAnn
	 * @param inputASname
	 * @param doc
	 * @param sequenceSpan
	 * @return
	 */
	public List<GateClassification> classify(String instanceAnn, 
			String inputASname, Document doc, String sequenceSpan){

		List<GateClassification> gcs = new ArrayList<GateClassification>();

		AnnotationSet spanAnnotations = doc.getAnnotations(inputASname).get(sequenceSpan);

		Iterator<Annotation> spit = spanAnnotations.iterator();
		while(spit.hasNext()){
			Annotation spanAnn = spit.next();
			Integer sequenceSpanID = spanAnn.getId();
			
			Instance inst = CorpusWriterMalletSeq.sequenceInstanceFromSpanAnnotation(
					this.getSavedConfFile(), inputASname, doc, spanAnn, instanceAnn, 
					this.getMode(), null, null);

			//Always put the instance through the same pipe used for training.
			inst = crf.getInputPipe().instanceFrom(inst);
						
			SumLatticeDefault sl = new SumLatticeDefault(crf, 
					(FeatureVectorSequence)inst.getData());
			
			List<Annotation> instanceAnnotations = gate.Utils.getContainedAnnotations(
					doc.getAnnotations(inputASname), spanAnn, instanceAnn).inDocumentOrder();
			
			//Sanity check that we're mapping the probs back onto the right anns.
			//This being wrong might follow from errors reading in the data to mallet inst.
			if(instanceAnnotations.size()!=((FeatureVectorSequence)inst.getData()).size()){
				logger.warn("LearningFramework: CRF output length: " 
						+ ((FeatureVectorSequence)inst.getData()).size() 
						+ ", GATE instances: " + instanceAnnotations.size()
						+ ". Can't assign.");
			} else {
				for(int i=0;i<instanceAnnotations.size();i++){
					Annotation ann = instanceAnnotations.get(i);
				
					String bestLabel = null;
					double bestProb = 0.0;
					
					//For each label option ..
					for(int j=0;j<crf.getOutputAlphabet().size();j++){
						String label = crf.getOutputAlphabet().lookupObject(j).toString();
						
						//Get the probability of being in state j at position i+1
						//Note that the plus one is because the labels are on the
						//transitions. Positions are between transitions.
						double marg = sl.getGammaProbability(i+1,crf.getState(j));
						if(marg>bestProb){
							bestLabel = label;
							bestProb = marg;
						}
					}
					GateClassification gc = new GateClassification(
							ann, bestLabel, bestProb, sequenceSpanID);
	
					gcs.add(gc);
				}
			}
		}

		return gcs;
	}

	public void evaluateXFold(CorpusWriter evalCorpus, int folds){
		CorpusWriterMalletSeq cwms = (CorpusWriterMalletSeq)evalCorpus;
		double accuracyaccumulator = 0.0;
		
		CrossValidationIterator cvi = cwms.getInstances().crossValidationIterator(folds);
		
		while(cvi.hasNext()){
			InstanceList[] il = cvi.nextSplit();
			InstanceList training = il[0];
			InstanceList test = il[1];
			
			CRF localCrf = new CRF(training.getPipe(), null);
			
			// construct the finite state machine
			localCrf.addFullyConnectedStatesForLabels();
			// initialize model's weights
			localCrf.setWeightsDimensionAsIn(training, false);

			//  CRFOptimizableBy* objects (terms in the objective function)
			// objective 1: label likelihood objective
			CRFOptimizableByLabelLikelihood optLabel =
					new CRFOptimizableByLabelLikelihood(localCrf, training);

			// CRF trainer
			Optimizable.ByGradientValue[] opts =
					new Optimizable.ByGradientValue[]{optLabel};
			// by default, use L-BFGS as the optimizer
			CRFTrainerByValueGradients crfTrainer = new CRFTrainerByValueGradients(localCrf, opts);

			// all setup done, train until convergence
			crfTrainer.setMaxResets(0);
			crfTrainer.train(training, Integer.MAX_VALUE);
			
			accuracyaccumulator += localCrf.averageTokenAccuracy(test);
		}

		logger.info("LearningFramework: " + folds
				+ " fold cross-validation accuracy: " + accuracyaccumulator/folds);
	}

	public void evaluateHoldout(CorpusWriter evalCorpus, float trainingproportion){
		CorpusWriterMalletSeq cwms = (CorpusWriterMalletSeq)evalCorpus;
		
		InstanceList[] sets = cwms.getInstances().split(new Random(1),
				new double[]{trainingproportion, 1-trainingproportion});
		InstanceList traindata = sets[0];
		InstanceList testdata = sets[1];
		
		CRF localCrf = new CRF(traindata.getPipe(), null);
		
		// construct the finite state machine
		localCrf.addFullyConnectedStatesForLabels();
		// initialize model's weights
		localCrf.setWeightsDimensionAsIn(traindata, false);

		//  CRFOptimizableBy* objects (terms in the objective function)
		// objective 1: label likelihood objective
		CRFOptimizableByLabelLikelihood optLabel =
				new CRFOptimizableByLabelLikelihood(localCrf, traindata);

		// CRF trainer
		Optimizable.ByGradientValue[] opts =
				new Optimizable.ByGradientValue[]{optLabel};
		// by default, use L-BFGS as the optimizer
		CRFTrainerByValueGradients crfTrainer = new CRFTrainerByValueGradients(localCrf, opts);

		// all setup done, train until convergence
		crfTrainer.setMaxResets(0);
		crfTrainer.train(traindata, Integer.MAX_VALUE);

		logger.info("LearningFramework: Holdout accuracy training on " 
				+ trainingproportion + " of the data: " + localCrf.averageTokenAccuracy(testdata));
	}
	
	public Algorithm whatIsIt(){
		return Algorithm.MALLET_SEQ_CRF;
	}
}
