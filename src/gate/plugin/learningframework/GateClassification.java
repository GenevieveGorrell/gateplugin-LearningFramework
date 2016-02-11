/*
 * GateClassification.java
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
package gate.plugin.learningframework;

import java.util.List;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.plugin.learningframework.Globals;

public class GateClassification {

  private Annotation instance;
  private String classAssigned;
  private Double confidenceScore;
  private Integer seqSpanID;
  private List<String> classList;
  private List<Double> confidenceList;

  public GateClassification(Annotation instance, String classAssigned,
          Double confidenceScore) {
    this.instance = instance;
    this.classAssigned = classAssigned;
    this.confidenceScore = confidenceScore;
  }

  public GateClassification(Annotation instance, String classAssigned,
          Double confidenceScore, List<String> classes, List<Double> confidences) {
    this.instance = instance;
    this.classAssigned = classAssigned;
    this.confidenceScore = confidenceScore;
    this.classList = classes;
    this.confidenceList = confidences;
  }

  public GateClassification(Annotation instance, String classAssigned,
          Double confidenceScore, Integer sequenceSpanID) {
    this.instance = instance;
    this.classAssigned = classAssigned;
    this.confidenceScore = confidenceScore;
    this.seqSpanID = sequenceSpanID;
  }

  public Annotation getInstance() {
    return instance;
  }

  public void setInstance(Annotation instance) {
    this.instance = instance;
  }

  public String getClassAssigned() {
    return classAssigned;
  }

  public void setClassAssigned(String classAssigned) {
    this.classAssigned = classAssigned;
  }

  public Double getConfidenceScore() {
    return confidenceScore;
  }

  public void setConfidenceScore(Double confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  public Integer getSeqSpanID() {
    return seqSpanID;
  }

  public List<String> getClassList() {
    return classList;
  }

  public List<Double> getConfidenceList() {
    return confidenceList;
  }

  public void setSeqSpanID(Integer sequenceSpanID) {
    this.seqSpanID = sequenceSpanID;
  }
  
  /**
   * Utility function to apply a list of GateClassification to a document.
   * This creates classification/regression output from a list of GateClassification objects.
   * If outputAS is null, then the original instance annotations are modified and receive the
   * target features and additional LearningFramework-specific features (confidence etc.).
   * If outputAS is specified, new annotations which are a copy of the instance annotations
   * are created in the outputAS and the target features are stored in those copies.
   * @param doc
   * @param gcs 
   */
  public static void applyClassification(Document doc, 
          List<GateClassification> gcs, 
          String targetFeature, 
          AnnotationSet outputAS) {
    for(GateClassification gc : gcs) {
      FeatureMap fm = null;
      if(outputAS == null) {
        fm = gc.getInstance().getFeatures();
      } else {
        fm = gate.Utils.toFeatureMap(gc.getInstance().getFeatures());
      }
      fm.put(targetFeature, gc.getClassAssigned());
      fm.put("LF_target", gc.getClassAssigned());
      fm.put(Globals.outputProbFeature, gc.getConfidenceScore());
      if (gc.getClassList() != null && gc.getConfidenceList() != null) {
        fm.put(Globals.outputClassFeature + "_list", gc.getClassList());
        fm.put(Globals.outputProbFeature + "_list", gc.getConfidenceList());
      }
      if (gc.getSeqSpanID() != null) {
        System.err.println("Refactoring error: why do we have a SeqSpanID when doing classification?");
      }
      if(outputAS != null) {
        int id = gate.Utils.addAnn(outputAS, gc.getInstance(), gc.getInstance().getType(), fm);
        Annotation ann = outputAS.get(id);
        // System.err.println("DEBUG adding ann "+ann+", target feature "+targetFeature+" should be "+gc.getClassAssigned());
      }
    } // for
  }
  
  
  @Override
  public String toString() {
    return "GateClassification{type="+instance.getType()+", at="+gate.Utils.start(instance)+
            ", target="+classAssigned+"}";
  }
}
