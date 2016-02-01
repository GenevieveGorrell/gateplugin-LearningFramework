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
import gate.plugin.learningframework.features.FeatureInfo.Attribute;
import gate.plugin.learningframework.features.FeatureInfo.AttributeList;
import gate.plugin.learningframework.features.FeatureInfo.CodeAs;
import gate.plugin.learningframework.features.FeatureInfo.Datatype;
import gate.plugin.learningframework.features.FeatureInfo.MissingValueTreatment;
import gate.plugin.learningframework.features.FeatureInfo.Ngram;
import gate.plugin.learningframework.features.FeatureInfo.SimpleAttribute;
import gate.util.GateRuntimeException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


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
   * Extract the featureName value for some instance.
   * If annType is null then the value of featureName is taken from the instanceAnnotation, otherwise
 from the coextensive, or overlapping annotation of annType annType. If there is more than
 one coextensive or overlapping annotation, the one with smallest annotation ID is used.
 
 If the featureName is null, then the clean string for the respective annotation is used instead
 of the value of the feature featureName. 
 
 If the document text is wanted, it must be in a feature
 If no feature is specified, we give an indicator of the presence, artificial feature name for binary feature. 
 * 
   * 
   * @param sourceAnnotationTypeName
   * @param featureName
   * @param datatype
   * @param inputASname
   * @param instanceAnnotation
   * @param doc
   * @return 
   */
  public static void extractFeature(
          Instance inst,
          SimpleAttribute att,
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {
    String value = "null";
    /*Although the user needn't specify the annotation annType if it's the
     * same as the instance, they may do so. It's intuitive that if they
     * do so, they mean to extract the featureName from the instance, not just
     * the first colocated same annType annotation. This matters in
     * disambiguation, where we have many colocated same annType annotations.
     * Fix it up front by wiping out annType if it's the same as the instance.
     */
    AugmentableFeatureVector fv = (AugmentableFeatureVector) inst.getData();
    String annType = att.annType;
    String featureName = att.feature;
    MissingValueTreatment mvt = att.missingValueTreatment;
    CodeAs codeas = att.codeas;
    Datatype dt = att.datatype;
    Alphabet alphabet = att.alphabet;
    // first of all get the annotation from where we want to construct the annotation.
    // If the annType is the same as the type of the instance annotation, use the 
    // instance annotation directly. Otherwise, use an annotation of type annType that overlaps
    // with the instance annotation.
    // TODO: what do if there are several such overlapping annotations?
    // For now, we throw an exception if there are several!
    
    Annotation sourceAnnotation = null;
    if (instanceAnnotation.getType().equals(annType)) {
      sourceAnnotation = instanceAnnotation;
    } else {
      AnnotationSet overlappings = gate.Utils.getOverlappingAnnotations(doc.getAnnotations(inputASname), instanceAnnotation, annType);
      if(overlappings.size() > 1) {
        throw new GateRuntimeException("More than one overlapping annotation of type "+annType+" for instance annotation at offset "+
                gate.Utils.start(instanceAnnotation)+" in document "+doc.getName());
      } else if(overlappings.size() == 0) {
        // if there is no overlapping annotation of type annType, we simply do nothing
        return;
      }
      // we have exactly one annotation, use that one
      sourceAnnotation = overlappings.get(0);
    }
    // NOTE: there should be no way of how a feature we encounter now is already in the feature
    // vector, so we do not even check, we simply add the feature.
    // How we add the feature depends on the datatype, on the codeas setting if it is nominal,
    // and also on how we treat missing values.
    
    // if the feature name is empty, then all we want is indicate the presence of the annotation
    // as a boolean. No matter what the datatype is, this is always indicated by setting the
    // feature to 1.0 (while for all instances, where the annotation is missing, the value will
    // implicitly be set to 0.0). 
    if(featureName==null) {
      // construct the feature name and set to 1.0
      // however, only add the feature if the feature alphabet is allowed to grow.
      String fname = annType + NAMESEP + "ISPRESENT";
      addToFeatureVector(fv, fname, 1.0);
    } else {    
      // First get the value as an Object, if there is no value, we have an Object that is null
      Object valObj = sourceAnnotation.getFeatures().get(featureName);
      // no matter what the datatype is, a null is always a missing value, so we set the 
      // property that indicates the existence of a missing valuein the instance right here
      if(valObj == null) {
        inst.setProperty("haveMV", true);
      }
      // if the datatype is nominal, we have to first check what the codeas setting is.
      if(dt==Datatype.nominal) {
        if(codeas==CodeAs.one_of_k) {
          if(valObj != null) {
            // it is not a missing value
            String val = valObj.toString();
            // TODO: do we have to escape the feature name in some way here?
            addToFeatureVector(fv, annType+NAMESEP+featureName+val, 1.0);
          } else {
            // we have a missing value, check the missing value treatment for what to do now
            switch(mvt) {
              case ignore_instance: // this is handled elsewhere, nothing to do
                break;
              case keep:  // this represents the MV by not setting any indicator feature, so nothing to do
                break;
              case zero_value: // for one-of-k we treat this identically to keep, nothing to do
                break;
              case special_value: // we use the predefined special value
                addToFeatureVector(fv,annType+NAMESEP+featureName+MVVALUE,1.0);
                break; 
              default:
                throw new NotImplementedException("MV-Handling");
            }                      
          }
        } else if(codeas==CodeAs.number) {
          if(valObj!=null) {
            // For this representation, we need to maintain a dictionary that maps values to 
            // numbers! This is also done using an Alphabet, and if a value is not in the alphabet,
            // then if the alphabet is allowed to grow, we simply get the new value. But if the 
            // alphabet is not allowed to grow (at application time), finding a new value is an 
            // error and causes an exception
            // The alphabet for an number-coded attribute is stored in the SimpleAttribute object.
            // before we handle the value, we need to convert it to a string
            String val = valObj.toString();
            if(alphabet.contains(val)) {
              // add the feature, using the value we have stored for it, but only if the feature
              // itself can be added
              addToFeatureVector(fv, annType+NAMESEP+featureName, alphabet.lookupIndex(val));
            } else {
              // we have not seen this value: if the alphabet is allowed to grow add it and
              // then try to add the feature, otherwise, do nothing
              if(!alphabet.growthStopped()) {
                // the lookupIndex method automatically adds the value if it is not there yet
                addToFeatureVector(fv, annType+NAMESEP+featureName, alphabet.lookupIndex(val));
              }
            }
          } else {
            // we have a nominal value that should get coded numeric but it is a missing value
            switch(mvt) {
              case ignore_instance: // this is handled elsewhere, nothing to do
                break;
              case keep:  // for this kind of codeas, we use the value NaN
                addToFeatureVector(fv,annType+NAMESEP+featureName, Double.NaN );
                break;
              case zero_value: // use the first value, does not make much sense really, but ...
                // TODO: document that this combination should be avoided, probably
                addToFeatureVector(fv,annType+NAMESEP+featureName, 0.0 );
                break;
              case special_value: // we use the special value -1.0 which should get handled by Mallet somehow
                addToFeatureVector(fv,annType+NAMESEP+featureName,-1.0);
                break; 
              default:
                throw new NotImplementedException("MV-Handling");
            }                                  
          } // if valObj != null .. else
        } else {  // codeas setting for the nominal attribute
          throw new NotImplementedException("CodeAs method not implemented");
        }
      } else if(dt == Datatype.numeric) {
        if(valObj != null) {
          // just add the value, if possible and if the object can be interpreted as a number
          double val = 0.0;
          if(valObj instanceof Number) {
            val = ((Number)valObj).doubleValue();
          } else if(valObj instanceof Boolean) {
            if((Boolean)valObj)  { val = 1.0; } else {val = 0.0;}
          } else {
            // try to convert the string to a number. If that fails, just use 0.0 but log a warning
            try {
              val = Double.parseDouble(valObj.toString());
            } catch (Exception ex) {
              val = 0.0;
              logger.warn("Cannot parse String "+valObj+" as a number, using 0.0 for annotation of type "+annType+ 
                      " at offset "+gate.Utils.start(sourceAnnotation)+" in document "+doc.getName());
            }
          }        
          addToFeatureVector(fv,annType+NAMESEP+featureName,val);
        } else {
          // we got a missing value for a numeric attribute
        }
      } else if(dt == Datatype.bool) {
        if(valObj != null) {
          double val = 0.0;
          if(valObj instanceof Boolean) {
            if((Boolean)valObj) val = 1.0;
          } else if(valObj instanceof Number) {
            if(((Number)valObj).doubleValue() != 0) val = 1.0;
          } else {
            try {
              boolean tmp = Boolean.parseBoolean(valObj.toString());
              if(tmp) val = 1.0;
            } catch (Exception ex) {
              // value is already 0.0
              logger.warn("Cannot parse String "+valObj+" as a boolean, using 0.0 for annotation of type "+annType+ 
                      " at offset "+gate.Utils.start(sourceAnnotation)+" in document "+doc.getName());              
            }
          }
          addToFeatureVector(fv,annType+NAMESEP+featureName,val);
        } else {
          
        }
      } else {
        throw new NotImplementedException("Datatype!");
      }
    
    }
  }

  // TODO: add to an existing augmentable feature vector or simply return a feature vector
  // so that the caller can add it to an augmentable feature vector.
  // In any case optionally pass on an alphabet: if that is non-null, only add features which
  // are present in the alphabet!
  public static void extractRangeFeature(Instance inst, Attribute att, AttributeList al, String inputASname, Annotation instanceAnnotation, Document doc, String separator) {
    String textToReturn = "";
    if (al == null) {
      //return "null";
    }
    Datatype datatype = al.datatype;
    String type = al.annType;
    String feature = al.feature;
    int from = al.from;
    int to = al.to;
    AnnotationSet as = doc.getAnnotations(inputASname);
    long centre = instanceAnnotation.getStartNode().getOffset();
    List<Annotation> annlistforward = as.get(type, centre, doc.getContent().size()).inDocumentOrder();
    List<Annotation> annlistbackward = as.get(type, 0L, centre).inDocumentOrder();
    for (int i = from; i < to; i++) {
      Annotation ann;
      if (i < 0) {
        if (-i <= annlistbackward.size()) {
          ann = annlistbackward.get(annlistbackward.size() + i);
          // make compiler happy for now
          Object feat = null;
          //Object feat = extractFeature(inst,  type, feature, datatype, inputASname, ann, doc);
          // check if this is a missing value
          if(feat == null){
            // TODO: process as the feature specification demands
            // for now, we use a datatype specific default value instead
            if(datatype == Datatype.nominal) {
              feat = "%%%NA%%%";
            } else if(datatype == Datatype.numeric) {
              feat = (Double)0.0;
            }
            // TODO: eventually we should return something here where we can set a flag that
            // the instance contains at least one missing value!
          }
          if (datatype == Datatype.nominal) {
            // TODO: if the encoding is not one-of-k but numbered, set to the value from the dictionary!
            // TODO: once we return a feature vecto instance or similar, this should be 
            // key = 1.0 for one-of-k and key = k for numbered
            textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + ":" + feat.toString();
          } else {
            try {              
              // TODO: once we return the actual feature vector, convert all numbers to double
              // convert boolean to 1.0 or 0.0 and convert string to Double.parseDouble(string)
              // if(feat instanceof Number) { ...
              double df = Double.parseDouble(feat.toString());
              textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + "=" + df;
            } catch (NumberFormatException e) {
              logger.warn("LearningFramework: Failed to format numeric feature " + feature + "=" + feat + " as double. Treating as string.");
              textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + ":" + feat;
            }
          }
        }
      } else if (i < annlistforward.size()) {
        ann = annlistforward.get(i);
        // make compiler happy for now
        //textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + ":" + extractFeature(type, feature, datatype, inputASname, ann, doc);
      }
    }
    if (textToReturn.length() > 1) {
      //Trim off the leading separator
      textToReturn = textToReturn.substring(1);
    }
    // make compiler happy for now
    //return textToReturn;
  }

  /*
   *
   */
  
  // TODO: NOTE: this currently returns a single string which represents all N-grams 
  // If there are at least n annotations as speficied by the Ngam TYPE contained in the span of 
  // the instance annotation, then those annotations are arranged in document order and
  // - starting with the second index, up to the last
  // - 
  // CHECK: if we get the same ngram multiple times, we should have a count!!! e.g. unigram "fred" three
  // times we should have 3.0
  // TODO: check what to do if the contained annotations are not in non-overlapping order: should we
  // create an ngram if the second annotations starts before the end of the first or even at the same 
  // offset as the first? If that is the case, what should the order of the annotations then be?
  // NOTE: if the feature is missing, i.e. it is null or the empty string, then the whole annotation gets ignored
  public static void extractNgramFeature(
          Instance inst,
          Ngram ng, 
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {
    AugmentableFeatureVector fv = (AugmentableFeatureVector) inst.getData();
    int number = ng.number;
    String annType = ng.annType;
    String feature = ng.feature;
    AnnotationSet inputAS = doc.getAnnotations(inputASname);
    // TODO: this we rely on the ngram only having allowed field values, e.g. type
    // has to be non-null and non-empty and number has to be > 0.
    // If feature is null, then for ngrams, the string comes from the covered document
      String[] gram = new String[number];
      List<Annotation> al = Utils.getContainedAnnotations(inputAS, instanceAnnotation, annType).inDocumentOrder();
      // If we have less annotations than our n for n-gram, there is certainly nothing to do, 
      // leave the feature vector untouched.
      if (al.size() < number) return;
        // this will hold the actual token strings to use for creating the n-grams
        List<String> strings = new ArrayList<String>();
        for(Annotation ann : al) {
          // for ngrams we either have a feature name 
          if(feature != null) {
            // NOTE: if the feature is not a string, we convert it to string
            Object obj = ann.getFeatures().get(feature);
            // if there is no value at all, then the annotation is ignored
            if(obj!=null) {
              String tmp =obj.toString().trim();
              // if the resulting string is empty, it is also ignored 
              if(!tmp.isEmpty()) {
                strings.add(tmp);
              }
            }
          } else {
            // if the feature is null, we get the string from the cleaned document text
            String tmp = gate.Utils.cleanStringFor(doc, ann).trim();
            if(!tmp.isEmpty()) {
              strings.add(tmp);
            }
          }
        } // for Annotation ann : al
        // Now construct the actual ngrams and add them to the augmentable feature vector. 
        // In the process, check first if such a feature is already there, and if yes, just 
        // increment the value.
        // To avoid overhead, we only create the ngrams on the fly
        
        // first check if our strings array is actually big enough so we can create at least one n-gram
        if(strings.size()<number) return;
        
        // now create the ngrams as follows: starting with the first element in strings, go
        // through all the elements up to the (size-n)ths and concatenate with the subsequent 
        // n stings using the pre-defined separator character.
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < (strings.size()-number); i++ ) {
          sb.setLength(0);
          for(int j = 0; j < number; j++) {
            if(j!=0) sb.append(NGRAMSEP);
            sb.append(strings.get(i+j));
          }
          String ngram = sb.toString();
          // we have got our ngram now, count it, but only add if we are allowed to!
          addToFeatureVector(fv, ngram, 1.0);
        }
  } // extractNGramFeature

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
    
  /** 
   * Same as the method, but makes sure a non-growable Alphabet is considered.
   * @param fv
   * @param key
   * @param val 
   */
  private static void addToFeatureVector(AugmentableFeatureVector fv, Object key, double val) {
    Alphabet a = fv.getAlphabet();
    if(!a.contains(key) && a.growthStopped()) return;
    fv.add(key,val);
  }
        
        
  // NOTE: we use an AugmentableFeatureVector to represent the growing feature vector as we 
  // build it.
  // The Mallet documentation is close to non-existing ATM, so here is what the methods we use do:
  // afv.add("x",val) adds val to whatever the current value for "x" is oder adds the feature, if
  //   the Alphabet can grow. If the Alphabet cannot grow, the method does nothing.
  //   UPDATE: this does not work! if one tries to do that, the indices get messed up and 
  //   the fv will throw an ArrayIndexOutOfBoundsException!!!!
  //   So we have always to explicitly check if the feature is in the alphabet!!!
  //   UPDATE: Mallet uses assert for checking things like this, so if assertsions are not enable,
  //   no exception is thrown until it is too late!
  // afv.value("x") retrieves the value if "x" is in the vector, otherwise an exception is thrown,
  //   even if "x" is in the alphabet. 
  // afv.contains("x") is true if the feature vector contains a value for "x" (which implies it must
  //   be in the alphabet)
  // afv.getAlphabet().contains("x") is true if "x" is in the alphabet. 
        
       
}
