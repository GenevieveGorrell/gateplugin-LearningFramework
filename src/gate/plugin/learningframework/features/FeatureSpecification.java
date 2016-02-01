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

import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Utils;
import gate.plugin.learningframework.features.Attribute;
import gate.plugin.learningframework.features.AttributeList;
import gate.plugin.learningframework.features.CodeAs;
import gate.plugin.learningframework.features.Datatype;
import gate.plugin.learningframework.features.MissingValueTreatment;
import gate.plugin.learningframework.features.Ngram;
import gate.plugin.learningframework.features.SimpleAttribute;
import gate.util.GateRuntimeException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/** 
 * Parse a feature specification file and extract features based on the specification.
 * This contains the code for parsing a feature specification and creating a FeatureInfo
 * object.
 * @author johann
 */
public class FeatureSpecification {
  
  /**
   * The separator character used to construct n-gram values, which will eventually be used 
   * as feature names.
   */
  private static final String NGRAMSEP = "_";
  private static final String NAMESEP = ":";
  private static final String MVVALUE = "%%%NA%%%";
  
  private static Logger logger = Logger.getLogger(FeatureSpecification.class.getName());

  /**
   * Extract the class for an instance for sequence tagging.
   * 
   * In the case of sequence tagging, we construct the class based on the instance's
 position relative to the class annotation annType. If it occurs at the beginning of the class
 annotation, it's a "beginning". In the middle or at the end, it's an "inside". Instances that
 don't occur in the span of a class annotation are an "outside".
 
 TODO: eventually the exact way of how to create class labels for sequence tagging should be
 parametrizable.
   * 
   * @param type The annotation annType name of the annotation that represents the class, e.g. "Person"
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
      System.err.println("LF ERROR: class type is null or empty, doc="+doc.getName()+" instance="+instanceAnnotation);
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
   * Given an annotation and optional feature, find the feature or just the text of the annotation
   * in the instance and return it.
   *
   * If no feature is specified, just return the text of the annotation.
   *
   * If no annotation is specified, use the instance.
   *
   * If it's a numeric feature, return in the format name=value, which gets used further down the
   * line;
   *
   * If more than one of the annotation occurs within the span of the instance, the first one will
   * be returned.
   */
  // TODO: would it be possible to return something that is not an object here?
  // The better way would be to return a Pair(String,double) for the use of mallet or a
  // Pair(String,Object) for the use of a generic representation that can also handle e.g.
  // multivalued nominal simpleattributes in a single feature.
  // TODO: we may want to handle true boolean features separately, because they will be represented
  // by two orthogonal features, e.g. value:true and value:false, when one feature would be enough!
  
  public static String extractSingleFeature(SimpleAttribute att, String inputASname, Annotation instanceAnnotation, Document doc) {
    if (att == null) {
      return "null";
    }
    Datatype datatype = att.datatype;
    String type = att.annType;
    String feature = att.feature;
    // make compiler happy for now
    String feat = null;
    //String feat = extractFeature(type, feature, datatype, inputASname, instanceAnnotation, doc);
    if (type == null) {
      type = instanceAnnotation.getType();
    }
    ;
    if (feature == null) {
      feature = "cleanstring";
    }
    ;
    if (datatype == Datatype.nominal) {
      return type + ":" + feature + ":" + feat;
    } else {
      try {
        double df = Double.parseDouble(feat);
        return type + ":" + feature + "=" + df;
      } catch (NumberFormatException e) {
        // JP: this was previously saying that it would "treat it as string" and it
        // returned: annType + ":" + feature + ":" + value;
        // In the cases I observed the value of value got printed as "null" so it was
        // either null or the literal string "null"
        // We have to look at this again in the light of the fact that "null" is used to
        // represent missing values!!
        logger.warn("LearningFramework: Failed to format numeric feature " + feature + "=" + feat + " as double. Treating as 0.0");
        return type + ":" + feature + "=" + 0.0;
      }
    }
  }

  /**
   * Get the class for this instance.
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
  
  
  // TODO: methos for directly extracting feature vectors and targets for classification and
  // sequence tagging (and regression). Maybe make some of the static utility methods private, 
  // if noone will need them!

	private org.jdom.Document jdomDocConf = null;
	
	
	private URL url;
        
	public FeatureSpecification(URL configFileURL) {
		this.url = configFileURL;
		
		SAXBuilder saxBuilder = new SAXBuilder(false);
		try {
			try {
				this.jdomDocConf = saxBuilder.build(configFileURL);
			} catch(JDOMException jde){
				throw new GateRuntimeException(jde);
			}
		} catch (java.io.IOException ex) {
			throw new GateRuntimeException(ex);
		}

		Element rootElement = jdomDocConf.getRootElement();
		
		List<Element> attributeElements = rootElement.getChildren("ATTRIBUTE");

		for(int i=0;i<attributeElements.size();i++){
			Element attributeElement = attributeElements.get(i);
			String dtstr = getChildTextOrElse(attributeElement, "DATATYPE", null);
                        if(dtstr == null)
                          throw new GateRuntimeException("DATATYPE not specified for ATTRIBUTE "+(i+1));
                        Datatype dt = Datatype.valueOf(dtstr);
                        // TODO: this should be named ANNOTATIONTYPE or ANNTYPE to avoid confusion
                        // with the datatype
                        String atype = getChildTextOrElse(attributeElement, "TYPE", "");
                        // must not be empty
                        if(atype.isEmpty()) {
                          throw new GateRuntimeException("TYPE in ATTRIBUTE "+(i+1)+" must not be missing or empty");
                        }
                        String feat = getChildTextOrElse(attributeElement,"FEATURE","");
                        String codeasstr = getChildTextOrElse(attributeElement,"CODEAS","one-of-k").toLowerCase();
                        if(!codeasstr.equals("one_of_k") && codeasstr.equals("number")) {
                          throw new GateRuntimeException("CODES for ATTRIBUTE "+(i+1)+" not one-of-k or number but "+codeasstr);
                        }
                        CodeAs codeas = CodeAs.valueOf(codeasstr);
                        String missingValueTreatmentStr = getChildTextOrElse(attributeElement, "MISSINGVALUETREATMENT", "zero_value");
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
                        // compiler happyness
			//this.simpleattributes.add(att);
		}

		List<Element> ngramElements = rootElement.getChildren("NGRAM");

		for(int i=0;i<ngramElements.size();i++){
			Element ngramElement = ngramElements.get(i);
			Ngram ng = new Ngram(
					Integer.parseInt(ngramElement.getChildText("NUMBER")),
					ngramElement.getChildText("TYPE"),
					ngramElement.getChildText("FEATURE")
			);
                        // compiler happyness
			// this.ngrams.add(ng);
		}
		
		List<Element> attributeListElements = rootElement.getChildren("ATTRIBUTELIST");

		for(int i=0;i<attributeListElements.size();i++){
			Element attributeListElement = attributeListElements.get(i);
			Datatype dt = Datatype.valueOf("unset");
			if(attributeListElement.getChildText("DATATYPE")!=null){
				dt = Datatype.valueOf(attributeListElement.getChildText("DATATYPE"));
			}
                        /* happyness
			AttributeList al = new AttributeList(
					attributeListElement.getChildText("TYPE"),
					attributeListElement.getChildText("FEATURE"),
					dt,
					Integer.parseInt(attributeListElement.getChildText("FROM")),
					Integer.parseInt(attributeListElement.getChildText("TO"))
			);
			this.attributelists.add(al);
                        */
		}
	}
	/*
	public List<SimpleAttribute> getAttributes() {
		return simpleattributes;
	}

	public void setAttributes(List<SimpleAttribute> attributes) {
		this.simpleattributes = attributes;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public List<Ngram> getNgrams() {
		return ngrams;
	}

	public void setNgrams(List<Ngram> ngrams) {
		this.ngrams = ngrams;
	}

	public List<AttributeList> getAttributelists() {
		return attributelists;
	}

	public void setAttributelists(List<AttributeList> attributelists) {
		this.attributelists = attributelists;
	}
        */
       //// HELPER METHODS
        
        /**
         * Return the text of a single child element or a default value.
         * This checks that there is at most one child of this annType and throws and exception
 if there are more than one. If there is no child of this name, then the value
 elseVal is returned. NOTE: the value returned is trimmed if it is a string, but 
 case is preserved
         */
  private static String getChildTextOrElse(Element parent, String name, String elseVal) {
          List<Element> children = parent.getChildren(name);
          if(children.size() > 1) {
            throw new GateRuntimeException("Element "+parent.getName()+" has more than one nested "+name+" element");
          }
          String tmp = parent.getChildTextTrim(name);
          if(tmp==null) return elseVal;
          else return tmp;
        }
    
       
}
