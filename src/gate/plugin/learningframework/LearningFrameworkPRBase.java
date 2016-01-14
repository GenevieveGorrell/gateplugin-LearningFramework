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

import java.io.Serializable;
import java.net.URL;

import org.apache.log4j.Logger;

import gate.Controller;
import gate.Document;
import gate.Resource;
import gate.creole.ControllerAwarePR;
import gate.creole.ResourceInstantiationException;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

/**
 * Base class for all LearningFramework PRs providing the shared parameters and some shared
 * processing.
 */
public abstract class LearningFrameworkPRBase
        extends AbstractLanguageAnalyser
        implements Serializable, ControllerAwarePR {

  /**
   *
   */
  private Logger logger = Logger.getLogger(LearningFrameworkPRBase.class.getCanonicalName());

  // =================================================================
  // Creole Parameters for all the PRs that derive from this class
  // =================================================================
  protected URL dataDirectory;

  @RunTime
  @CreoleParameter(comment = "The directory where all data will be stored and read from")
  public void setDataDirectory(URL output) {
    dataDirectory = output;
  }

  public URL getDataDirectory() {
    return this.dataDirectory;
  }

  protected String inputASName;

  @RunTime
  @Optional
  @CreoleParameter
  public void setInputASName(String iasn) {
    this.inputASName = iasn;
  }

  public String getInputASName() {
    return this.inputASName;
  }

  protected String instanceType;

  @RunTime
  @CreoleParameter(defaultValue = "Token", comment = "The annotation type to "
          + "be treated as instance.")
  public void setInstanceType(String inst) {
    this.instanceType = inst;
  }

  public String getInstanceType() {
    return this.instanceType;
  }


  protected Mode mode;

  @RunTime
  @CreoleParameter(defaultValue = "CLASSIFICATION", comment = "Whether to do "
          + "classification or named entity recognition.")
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public Mode getMode() {
    return this.mode;
  }

  protected String classType;

  @RunTime
  @CreoleParameter(comment = "Annotation type containing/indicating the class.")
  public void setClassType(String classType) {
    this.classType = classType;
  }

  public String getClassType() {
    return this.classType;
  }

  protected String classFeature;

  @RunTime
  @Optional
  @CreoleParameter(comment = "For classification, the feature "
          + "containing the class. Ignored for NER, where type only is used.")
  public void setClassFeature(String classFeature) {
    this.classFeature = classFeature;
  }

  public String getClassFeature() {
    return this.classFeature;
  }

  protected String identifierFeature;

  @RunTime
  @Optional
  @CreoleParameter(comment = "The feature of the instance that "
          + "can be used as an identifier for that instance.")
  public void setIdentifierFeature(String identifierFeature) {
    this.identifierFeature = identifierFeature;
  }

  public String getIdentifierFeature() {
    return this.identifierFeature;
  }

  protected String learnerParams;

  @RunTime
  @Optional
  @CreoleParameter(comment = "Some of the learners take parameters. Parameters "
          + "can be entered here. For example, the LibSVM supports parameters.")
  public void setLearnerParams(String learnerParams) {
    this.learnerParams = learnerParams;
  }

  public String getLearnerParams() {
    return this.learnerParams;
  }

  //================================================================
  // Instance variables, visible to the child classes
  //================================================================
  /**
   * A flag that indicates that the PR has just been started. Used in execute() to run code that
   * needs to run once before any documents are processed.
   */
  protected boolean justStarted = false;

  /**
   * A flag that indicates that at least one document was processed.
   */
  protected boolean haveSomeDocuments = false;

  protected Controller controller;

  protected Throwable throwable;

  //===============================================================================
  // Implementation of the relevant API methods for LanguageAnalyzers. These
  // get inherited by the implementing class. This also defines abstract methods 
  // that make it easier to handle the control flow:
  // void execute(Document doc) - replaces void execute()
  // void beforeFirstDocument() - called before the first document is processed
  //     (not called if there were no documents in the corpus, for example)
  // void afterLastDocument()   - called after the last document was processed
  //     (not called if there were no documents in the corpus, for example)
  //================================================================================
  @Override
  public Resource init() throws ResourceInstantiationException {
    return this;
  }

  @Override
  public void execute() throws ExecutionException {
    if (justStarted) {
      beforeFirstDocument(controller);
      justStarted = false;
    }
    execute(getDocument());
    haveSomeDocuments = true;
  }

  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1)
          throws ExecutionException {
    // reset the flags for the next time the controller is run
    controller = arg0;
    throwable = arg1;
    if (haveSomeDocuments) {
      afterLastDocument(arg0, arg1);
    } else {
      finishedNoDocument(arg0, arg1);
    }
  }

  @Override
  public void controllerExecutionFinished(Controller arg0)
          throws ExecutionException {
    controller = arg0;
    if (haveSomeDocuments) {
      afterLastDocument(arg0, null);
    } else {
      finishedNoDocument(arg0, null);
    }
  }

  @Override
  public void controllerExecutionStarted(Controller arg0)
          throws ExecutionException {
    controller = arg0;
    justStarted = true;
    haveSomeDocuments = false;
  }

  //=====================================================================
  // New simplified API for the child classes 
  //=====================================================================
  protected abstract void execute(Document document);

  protected abstract void beforeFirstDocument(Controller ctrl);

  protected abstract void afterLastDocument(Controller ctrl, Throwable t);
  
  protected abstract void finishedNoDocument(Controller ctrl, Throwable t);
}
