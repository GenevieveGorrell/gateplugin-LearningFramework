/*
 * EngineLibSVM.java
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

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SparseVector;

public class EngineLibSVM  extends Engine {

	private svm_model svmModel = null;

	private String params = null;

	private static svm_print_interface svm_print_null = new svm_print_interface(){
		public void print(String s) {}
	};
	
	private svm_print_interface print_func = null;	// default printing to stdout

	public EngineLibSVM(File savedModel, Mode mode, boolean restore){
		this.setOutputDirectory(savedModel);
		this.setMode(mode);

		//Restore the classifier and the saved copy of the configuration file
		//from train time.
		if(restore){	
			this.svmModel = (svm_model)this.loadClassifier();
		}
	}

	public EngineLibSVM(File savedModel, Mode mode, String params, boolean restore){
		this.params = params;
		this.setOutputDirectory(savedModel);
		this.setMode(mode);

		//Restore the classifier and the saved copy of the configuration file
		//from train time.
		if(restore){	
			this.svmModel = (svm_model)this.loadClassifier();
		}
	}

	private svm_parameter makeParam(double num_feats){
		//Default parameters (kernel etc.) are pretty good. For guidelines 
		//see here: http://www.csie.ntu.edu.tw/~cjlin/papers/guide/guide.pdf
		//To find out what the defaults are, see here: http://www.csie.ntu.edu.tw/~cjlin/libsvm/
		svm_parameter param=new svm_parameter();
		
		param.cache_size=20000; //Default of 100 is a bit low

		//By default we're going to calculate probabilities and write them
		//on the annotations at apply time. This is slow because it isn't
		//such a natural thing for an SVM to do. The user can override this
		//option using the runtime parameter learnerParams.
		//param.probability=1; //Changed my mind. Let's stick to the usual default.

		//This supposedly is the default. However in my experience if it is
		//absent, libsvm chucks a null or never terminates training. User
		//is welcome to override with a different value but it should't be
		//absent.
		param.eps = 0.001;

		//Supposedly the default is 1 but it looks to me like the default is
		//actually 0, because if I don't set it, the behaviour I see is as though
		//it were 0. If it's zero we get the same result for everything.
		//This isn't helpful, so I'll set it here to the sensible default it
		//ought to have.
		param.C = 1;
		
		//Again, supposedly the default is 2 but it looks to me like it's 0.
		//We'll make it 2 so that functionality reflects the current online
		//libsvm documentation.
		param.kernel_type = 2;

		//Weights need setting up
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		
		if(params!=null && !params.equals("") && !params.isEmpty()){
			String[] p = this.params.split("\\s+");

			for(int i=0;i<p.length;i++){
				if(p[i].charAt(0) != '-') break;
				i++;
				switch(p[i-1].charAt(1)){
				case 's':
					String value = p[i];
					param.svm_type = Integer.parseInt(value);
					break;
				case 't':
					param.kernel_type = Integer.parseInt(p[i]);
					break;
				case 'd':
					param.degree = Integer.parseInt(p[i]);
					break;
				case 'g':
					param.gamma = Double.parseDouble(p[i]);
					break;
				case 'r':
					param.coef0 = Double.parseDouble(p[i]);
					break;
				case 'c':
					param.C = Double.parseDouble(p[i]);
					break;
				case 'n':
					param.nu = Double.parseDouble(p[i]);
					break;
				case 'p':
					param.p = Double.parseDouble(p[i]);
					break;
				case 'm':
					param.cache_size = Double.parseDouble(p[i]);
					break;
				case 'e':
					param.eps = Double.parseDouble(p[i]);
					break;
				case 'h':
					param.shrinking = Integer.parseInt(p[i]);
					break;
				case 'b':
					param.probability = Integer.parseInt(p[i]);
					break;
				case 'w':
					++param.nr_weight;
					
					int[] oldlabels = param.weight_label;
					int[] newlabels = new int[param.nr_weight];
					System.arraycopy(oldlabels,0,newlabels,0,param.nr_weight-1);
					param.weight_label = newlabels;
					
					double[] oldvalues = param.weight;
					double[] newvalues = new double[param.nr_weight];
					System.arraycopy(oldvalues,0,newvalues,0,param.nr_weight-1);
					param.weight = newvalues;
							
					param.weight_label[param.nr_weight-1] = Integer.parseInt(p[i-1].substring(2));
					param.weight[param.nr_weight-1] = Double.parseDouble(p[i]);
					break;
				case 'q':
					this.print_func = svm_print_null;
					i--;
					break;
				default:
					logger.warn("LearningFramework: Unknown option: " + p[i-1] + "\n");
				}
			}
		}
		
		//By convention, a gamma of zero indicates that we should 
		//use 1/number of features. We don't ever want to have a 
		//gamma of zero because the model will fail.
		if(param.gamma==0.0){
			param.gamma=1/num_feats;
		}
		
		return param;
	}

	public void train(FeatureSpecification conf, CorpusWriter trainingCorpus){
		//Start by clearing out the previous saved model.
		File[] files = this.getOutputDirectory().listFiles();
		if(files!=null) {
			for(File f: files) {
				f.delete();
			}
		}

		//Mallet feature prep stuff is so helpful so we'll start there
		CorpusWriterMallet trMal = (CorpusWriterMallet)trainingCorpus;

		svm_problem prob = trMal.getLibSVMProblem();

		if(prob.l==0){
			logger.warn("LearningFramework: No training instances!");
		} else {
			//Sanity check--how does the data look?
			logger.info("LearningFramework: Instances: " + trMal.getInstances().size());
			logger.info("LearningFramework: Data labels: " + trMal.getInstances().getDataAlphabet().size());
			logger.info("LearningFramework: Target labels: " + trMal.getInstances().getTargetAlphabet().size());

		}

		svm_parameter param = makeParam(trMal.getInstances().getDataAlphabet().size());
		
		//Reorder the weights--when the user chose their weights they did so on the
		//basis of alphabetical order. The classes didn't come in that way though so
		//we need to reorder the weights the way the user meant.
		Object[] labels = trMal.getInstances().getTargetAlphabet().toArray();
		if(param.weight.length!=0 && labels.length!=param.weight.length){
			logger.warn("LearningFramework: Number of weights not the same as number of classes!");
		} else {
			Arrays.sort(labels);
			double[] sortedweights = new double[param.weight.length];
			for(int i=0;i<param.weight.length;i++){
				//This is the class that the user meant to assign weight i to
				Object label = labels[i];
				//This is where it is according to Mallet
				int oldindex = trMal.getInstances().getTargetAlphabet().lookupIndex(label);
				//So weight i needs to go to the position of oldindex
				sortedweights[oldindex] = param.weight[i];
				logger.info("LearningFramework: class " + label.toString() + " takes weight "
						+ param.weight[i]);
			}
			param.weight = sortedweights;
		}
		
		
		//Train is a static method on svm
		libsvm.svm.svm_set_print_string_function(print_func);
		libsvm.svm.rand.setSeed(1);
		svmModel = libsvm.svm.svm_train(prob, param);
		
		//printModel(trMal);

		this.save(conf, this.svmModel, trMal.getInstances().size(),
				trMal.getInstances().getDataAlphabet().size(),
				trMal.getInstances().getTargetAlphabet().size(), 
				trMal.pipe);
	}


	public List<GateClassification> classify(String instanceAnn, 
			String inputASname, Document doc){
		List<GateClassification> gcs = new ArrayList<GateClassification>();

		AnnotationSet inputAS = doc.getAnnotations(inputASname);

		List<Annotation> instanceAnnotations = inputAS.get(instanceAnn).inDocumentOrder();

		Iterator<Annotation> it = instanceAnnotations.iterator();

		int numberOfLabels = pipe.getTargetAlphabet().size();

		while(it.hasNext()){
			Annotation instanceAnnotation = it.next();

			Instance instance = CorpusWriterMallet.instanceFromInstanceAnnotation(
					this.getSavedConfFile(), instanceAnnotation, inputASname, doc,
					this.getMode(), "", "", "");


			//Instance needs to go through the pipe, so that
			//it gets mapped using the same alphabet, and the text is in the
			//expected format.
			instance = this.pipe.instanceFrom(instance);

			//Now convert to svm format
			svm_node[] vec = CorpusWriterMallet.getLibSVMVectorForInst(instance);

			int bestLabel = (new Double(svm.svm_predict(this.svmModel, vec))).intValue();

			double bestConf = 0.0;
			
			if(svm.svm_check_probability_model(this.svmModel)==1){
				double[] confidences = new double[numberOfLabels];
				double v = svm.svm_predict_probability(
						this.svmModel, vec, confidences);

				//Note to self--is it possible that labels won't always be in order?
				//If so, this won't work! But I think (hope) they always will be!
				bestConf = confidences[bestLabel];
			} else {
				double[] confidences = new double[numberOfLabels*(numberOfLabels - 1)/2];
				svm.svm_predict_values(this.svmModel, vec, confidences);
				//For now we are not providing decision values for non-prob
				//models because it is complex, see here: 
				//http://www.csie.ntu.edu.tw/~r94100/libsvm-2.8/README
			}

			
			String labelstr = (String)this.pipe.getTargetAlphabet().lookupObject(bestLabel);
			GateClassification gc = new GateClassification(
					instanceAnnotation, labelstr, bestConf);

			gcs.add(gc);
		}
		return gcs;
	}

	/*
	 * LibSVM implements a cross validation, so all we have to do is run that.
	 * However we have to count up the correct ones ourselves.
	 */
	public void evaluateXFold(CorpusWriter evalCorpus, int folds){
		CorpusWriterMallet cwm = (CorpusWriterMallet)evalCorpus; 
		svm_problem prob = cwm.getLibSVMProblem();
		svm_parameter param = makeParam(cwm.getInstances().getDataAlphabet().size());

		double[] target = new double[prob.l];

		//svm_cross_validation is a static method on svm that populates the
		//fourth argument as a side-effect
		libsvm.svm.svm_cross_validation(prob, param, folds, target);
		double correctCounter = 0.0;
		for (int i = 0; i < target.length; i++) {
			if (target[i] == prob.y[i]) {
				correctCounter++;
			} 
		}

		logger.info("LearningFramework: " + folds
				+ " fold cross-validation accuracy: " + correctCounter/target.length);
	}

	/*
	 * For holdout evaluation, no convenient method available so we have to code it
	 * ourselves.
	 */
	public void evaluateHoldout(CorpusWriter evalCorpus, float trainingproportion){
		CorpusWriterMallet cwm = (CorpusWriterMallet)evalCorpus; 
		svm_problem[] probs = cwm.getLibSVMProblemSplit(trainingproportion);
		svm_parameter param = makeParam(cwm.getInstances().getDataAlphabet().size());

		//Train is a static method on svm
		svm_model svmMod = libsvm.svm.svm_train(probs[0], param);

		double rightanswers = 0.0;		
		for(int i=0;i<probs[1].l;i++){
			svm_node[] nodearray = probs[1].x[i];
			double rightanswer = probs[1].y[i];
			double givenanswer = libsvm.svm.svm_predict(svmMod, nodearray);
			if(rightanswer==givenanswer){
				rightanswers+=1.0;
			}
		}

		logger.info("LearningFramework: Holdout accuracy training on " 
				+ trainingproportion + " of the data: " + rightanswers/probs[1].l);
	}

	public Algorithm whatIsIt(){
		return Algorithm.valueOf("LIBSVM");
	}
	
	private void printModel(CorpusWriterMallet trMal){
		if(this.svmModel.param.kernel_type==0){
			double[][] coef = svmModel.sv_coef;
			try {
				FileWriter fw = new FileWriter(new File(this.getOutputDirectory(), "model"));

				//For each separator (one vs another)
				for(int i=0;i<coef.length;i++){
					fw.write("Separator " + i + ":\n");
					double[] thisclassifier = coef[i];
					for(int j=0;j<thisclassifier.length;j++){
						fw.write(
								trMal.getInstances().getDataAlphabet().lookupObject(j) 
								+ "\t" + thisclassifier[j]
								+ "\n");
					}
					fw.write("\n");
				}
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				FileWriter fw = new FileWriter(new File(this.getOutputDirectory(), "model"));
				fw.write("Cannot write model that isn't linear.\n");
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
