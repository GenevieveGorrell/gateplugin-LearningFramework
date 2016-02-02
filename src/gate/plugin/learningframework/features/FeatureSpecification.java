/*
 * FeatureSpecification.java
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
package gate.plugin.learningframework.features;

import gate.Annotation;
import gate.Document;
import gate.Utils;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.StringReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Parse a feature specification file and extract features based on the specification. This contains
 * the code for parsing a feature specification and creating a FeatureInfo object.
 *
 * @author johann
 */
public class FeatureSpecification {

  private static Logger logger = Logger.getLogger(FeatureSpecification.class.getName());

  /**
   * Extract the class for an instance for sequence tagging.
   *
   * In the case of sequence tagging, we construct the class based on the instance's position
   * relative to the class annotation annType. If it occurs at the beginning of the class
   * annotation, it's a "beginning". In the middle or at the end, it's an "inside". Instances that
   * don't occur in the span of a class annotation are an "outside".
   *
   * TODO: eventually the exact way of how to create class labels for sequence tagging should be
   * parametrizable.
   *
   * @param type The annotation annType name of the annotation that represents the class, e.g.
   * "Person"
   * @param inputASname, the annotation set name of the set which contains the class annotations
   * @param instanceAnnotation, the instance annotation, e.g. "Token".
   * @param doc the document which is currently being processed
   * @return
   */
  public static String extractClassNER(String type, String inputASname, Annotation instanceAnnotation, Document doc) {
    if (type != null && !type.equals("")) {
      String textToReturn = "";
      List<Annotation> annotations = Utils.getOverlappingAnnotations(doc.getAnnotations(inputASname), instanceAnnotation, type).inDocumentOrder();
      if (annotations.size() > 0) {
        //Pick a mention to focus on--there should be only one by rights.
        Annotation mention = annotations.get(0);
        if (mention.getStartNode().getOffset() == instanceAnnotation.getStartNode().getOffset()) {
          textToReturn = "beginning";
        } else {
          textToReturn = "inside";
        }
      } else {
        //No overlapping mentions so it's an outside
        textToReturn = "outside";
      }
      return textToReturn;
    } else {
      System.err.println("LF ERROR: class type is null or empty, doc=" + doc.getName() + " instance=" + instanceAnnotation);
      return null;
    }
  }

  /**
   * Get the class for this instance.
   */
  public static double extractNumericClassForClassification(String type, String feature, String inputASname, Annotation instanceAnnotation, Document doc) {
    if (type != null && !type.equals("") && !instanceAnnotation.getType().equals(type)) {
      //Not on instance
      List<Annotation> annotations = Utils.getContainedAnnotations(doc.getAnnotations(inputASname), instanceAnnotation, type).inDocumentOrder();
      if (annotations.size() > 0) {
        Annotation annotationToPrint = annotations.get(0);
        if (feature != null) {
          Object toReturn = (Object) annotationToPrint.getFeatures().get(feature);
          if (toReturn instanceof String) {
            return Double.parseDouble((String) toReturn);
          } else {
            return ((Number) toReturn).doubleValue();
          }
        }
      }
    } else //On instance
     if (feature != null) {
        Object toReturn = (Object) instanceAnnotation.getFeatures().get(feature);
        if (toReturn instanceof String) {
          return Double.parseDouble((String) toReturn);
        } else {
          return ((Number) toReturn).doubleValue();
        }
      }
    logger.warn("LearningFramework: Failed to retrieve class.");
    return 0.0;
  }

  /**
   * Identifier gets used by mallet and maybe some others to define the instance. It might be useful
   * in the case that we write out the corpus for identifying what is going on in there. Mostly it
   * just does't matter though. Here, we extract it from the document for our instance.
   */
  public static String extractIdentifier(String type, String feature, String inputASname, Annotation instanceAnnotation, Document doc) {
    String textToReturn = "null";
    if (type != null && !type.equals("") && !instanceAnnotation.getType().equals(type)) {
      List<Annotation> annotations = Utils.getOverlappingAnnotations(doc.getAnnotations(inputASname), instanceAnnotation, type).inDocumentOrder();
      if (annotations.size() > 0) {
        Annotation annotationToPrint = annotations.get(0);
        if (feature != null) {
          if (annotationToPrint.getFeatures().get(feature) != null) {
            textToReturn = annotationToPrint.getFeatures().get(feature).toString();
          } else {
            textToReturn = "null";
          }
        } else {
          //No feature specified so we'll just use the text of the annotation.
          String annotationText = "";
          try {
            annotationText = Utils.cleanStringFor(doc, annotationToPrint);
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (annotationText.length() > 0) {
            textToReturn = annotationText;
          } else {
            textToReturn = "null";
          }
        }
      }
    } else if (feature != null) {
      if (instanceAnnotation.getFeatures().get(feature) != null) {
        textToReturn = instanceAnnotation.getFeatures().get(feature).toString();
      } else {
        textToReturn = "null";
      }
    } else {
      //No feature specified so we'll just use the text of the annotation.
      String annotationText = "";
      try {
        annotationText = Utils.cleanStringFor(doc, instanceAnnotation);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (annotationText.length() > 0) {
        textToReturn = annotationText;
      } else {
        textToReturn = "null";
      }
    }
    return textToReturn.replaceAll("[\\n\\s]+", "-");
  }

  /**
   * Get the class for this instance.
   *
   * @param type
   * @return
   */
  public static String extractClassForClassification(String type, String feature, String inputASname, Annotation instanceAnnotation, Document doc) {
    String textToReturn = "null";
    if (type != null && !type.equals("") && !instanceAnnotation.getType().equals(type)) {
      List<Annotation> annotations = Utils.getContainedAnnotations(doc.getAnnotations(inputASname), instanceAnnotation, type).inDocumentOrder();
      if (annotations.size() > 0) {
        Annotation annotationToPrint = annotations.get(0);
        if (feature != null) {
          String val = (String) annotationToPrint.getFeatures().get(feature);
          if (val != null && !val.isEmpty() && !val.equals("")) {
            textToReturn = val;
          }
        }
      }
    } else if (feature != null) {
      String val = (String) instanceAnnotation.getFeatures().get(feature);
      if (val != null && !val.isEmpty() && !val.equals("")) {
        textToReturn = val;
      }
    }
    return textToReturn.replaceAll("[\\n\\s]+", "-");
  }

  private org.jdom.Document jdomDocConf = null;

  private URL url;

  public FeatureSpecification(URL configFileURL) {
    url = configFileURL;

    SAXBuilder saxBuilder = new SAXBuilder(false);
    try {
      try {
        this.jdomDocConf = saxBuilder.build(configFileURL);
        parseConfigXml();
      } catch (JDOMException jde) {
        throw new GateRuntimeException(jde);
      }
    } catch (java.io.IOException ex) {
      throw new GateRuntimeException("Error parsing config file URL " + url, ex);
    }
  }

  public FeatureSpecification(String configString) {
    SAXBuilder saxBuilder = new SAXBuilder(false);
    try {
      try {
        this.jdomDocConf = saxBuilder.build(new StringReader(configString));
        parseConfigXml();
      } catch (JDOMException jde) {
        throw new GateRuntimeException(jde);
      }
    } catch (java.io.IOException ex) {
      throw new GateRuntimeException("Error parsing config file String:\n" + configString, ex);
    }
  }

  public FeatureSpecification(File configFile) {
    SAXBuilder saxBuilder = new SAXBuilder(false);
    try {
      try {
        this.jdomDocConf = saxBuilder.build(configFile);
        parseConfigXml();
      } catch (JDOMException jde) {
        throw new GateRuntimeException(jde);
      }
    } catch (java.io.IOException ex) {
      throw new GateRuntimeException("Error parsing config file " + configFile, ex);
    }
  }

  private void parseConfigXml() {

    // TODO: process children in order, then dispatch how to parse based on type.
    // Then, parsing ATTRIBUTE and ATTRIBUTELIST is nearly identical except that 
    // we parse the range in addition for ATTRIBUTELIST.
    // Make an else part where we document how we might add additional stuff...
    Element rootElement = jdomDocConf.getRootElement();

    List<Element> elements = rootElement.getChildren();

    attributes = new ArrayList<Attribute>();
    
    int n = 0;
    for (Element element : elements) {
      n++;
      String elementName = element.getName().toLowerCase();
      if (elementName.equals("attribute")) {
        attributes.add(parseSimpleAttribute(element, n));
      } else if (elementName.equals("attributelist")) {
        SimpleAttribute att = parseSimpleAttribute(element, n);
        int from = Integer.parseInt(element.getChildText("FROM"));
        int to = Integer.parseInt(element.getChildText("TO"));
        attributes.add(new AttributeList(att, from, to));
      } else if (elementName.equals("ngram")) {
        attributes.add(parseNgramAttribute(element, n));
      } else {
        throw new GateRuntimeException("Not a recognized element name for the LearningFramework config file: " + elementName);
      }
    }
  } // parseConfigXml

  private SimpleAttribute parseSimpleAttribute(Element attributeElement, int i) {

    String dtstr = getChildTextOrElse(attributeElement, "DATATYPE", null);
    if (dtstr == null) {
      throw new GateRuntimeException("DATATYPE not specified for ATTRIBUTE " + i);
    }
    Datatype dt = Datatype.valueOf(dtstr);
    // TODO: this should be named ANNOTATIONTYPE or ANNTYPE to avoid confusion
    // with the datatype
    String atype = getChildTextOrElse(attributeElement, "TYPE", "");
    // must not be empty
    if (atype.isEmpty()) {
      throw new GateRuntimeException("TYPE in ATTRIBUTE " + i + " must not be missing or empty");
    }
    String feat = getChildTextOrElse(attributeElement, "FEATURE", "");
    String codeasstr = getChildTextOrElse(attributeElement, "CODEAS", "one_of_k").toLowerCase();
    if (!codeasstr.equals("one_of_k") && codeasstr.equals("number")) {
      throw new GateRuntimeException("CODES for ATTRIBUTE " + i + " not one-of-k or number but " + codeasstr);
    }
    CodeAs codeas = CodeAs.valueOf(codeasstr);
    String missingValueTreatmentStr = getChildTextOrElse(attributeElement, "MISSINGVALUETREATMENT", "special_value");
    MissingValueTreatment mvt = MissingValueTreatment.valueOf(missingValueTreatmentStr);
    // TODO: not implemented yet, but we should add this!!
    String scalingMethod = "";
    String transformMethod = "";
    String missingValueValue = ""; // if MVs should get replaced with a constant value, that value as a String
    SimpleAttribute att = new SimpleAttribute(
            atype,
            feat,
            dt,
            codeas,
            mvt,
            missingValueValue,
            scalingMethod,
            transformMethod
    );
    return att;
  }

  private Attribute parseNgramAttribute(Element ngramElement, int i) {
    String annType = getChildTextOrElse(ngramElement,"TYPE","").trim();
    if (annType.isEmpty()) {
      throw new GateRuntimeException("TYPE in NGRAM " + i + " must not be missing or empty");
    }
    
    String feature = getChildTextOrElse(ngramElement,"FEATURE","").trim();
    if (feature.isEmpty()) {
      throw new GateRuntimeException("TYPE in NGRAM " + i + " must not be missing or empty");
    }
    Ngram ng = new Ngram(
            Integer.parseInt(ngramElement.getChildText("NUMBER")),
            annType,
            feature
    );
    return ng;
  }

  List<Attribute> attributes;
  
  /**
   * Return the stored list of attributes.
   * This is the original list, not a shallow or deep copy. 
   * @return 
   */
  public List<Attribute> getAttributes() {
    return attributes;
  }

  //// HELPER METHODS
  /**
   * Return the text of a single child element or a default value. This checks that there is at most
   * one child of this annType and throws and exception if there are more than one. If there is no
   * child of this name, then the value elseVal is returned. NOTE: the value returned is trimmed if
   * it is a string, but case is preserved
   */
  private static String getChildTextOrElse(Element parent, String name, String elseVal) {
    List<Element> children = parent.getChildren(name);
    if (children.size() > 1) {
      throw new GateRuntimeException("Element " + parent.getName() + " has more than one nested " + name + " element");
    }
    String tmp = parent.getChildTextTrim(name);
    if (tmp == null) {
      return elseVal;
    } else {
      return tmp;
    }
  }

}
