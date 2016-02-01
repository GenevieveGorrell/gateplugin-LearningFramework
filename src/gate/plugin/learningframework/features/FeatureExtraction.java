/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.features;

import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Utils;
import gate.plugin.learningframework.features.AttributeList;
import gate.plugin.learningframework.features.Ngram;
import gate.plugin.learningframework.features.SimpleAttribute;
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;

/**
 * Code for extracting features from a document based on a FeatureInfo.
 * For now this is intrinsically tied to Mallet: our own internal way of representing
 * features, instances, alphabets etc. uses in a large part the Mallet approach and 
 * the result of extracting features from the document is a Mallet instance.
 * Should we ever want to support other representations, this would need to get refactored
 * to an Interface or base class, with different implementations for different internal 
 * representations. 
 * 
 * @author Johann Petrak
 */
public class FeatureExtraction {
  
  private static final String NGRAMSEP = "_";
  private static final String NAMESEP = ":";
  private static final String MVVALUE = "%%%NA%%%";
  
  private static Logger logger = Logger.getLogger(FeatureExtraction.class.getName());

  /**
   * Extract the instance features for a simple attribute.
   * 
   * This adds the internal features to the inst object and also updates any 
   * alphabets, if they allow growth.
   * If the annotation types of the instance annotation and the annotation specified for the attribute
   * are the same, then the instance annotation is directly used, otherwise, if there is a single
   * overlapping annotation of the type specified in the attribute, that one is used.
   * If there is no overlapping annotation, nothing is extracted for that instance and implicitly,
   * all features are set to 0.0 (TODO: this should probably get treated as if all features were
   * missing so that for each feature, the proper missing value treatment can be applied!)
   * If there are several overlapping annotations, an exception is thrown, this should not happen.
   * 
   * If no feature is specified, we give an indicator of the presence, artificial feature name 
   * for binary feature. 
   * 
   * 
   * @param inst
   * @param att
   * @param inputASname
   * @param instanceAnnotation
   * @param doc
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
        // TODO: handle this as if all features have missing values!!!
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
  } // extractFeature (SimpleAttribute)

  
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
  public static void extractFeature(
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
  } // extractFeature(NGram)
  
  
  // TODO: add to an existing augmentable feature vector or simply return a feature vector
  // so that the caller can add it to an augmentable feature vector.
  // In any case optionally pass on an alphabet: if that is non-null, only add features which
  // are present in the alphabet!
  public static void extractFeature(
          Instance inst, 
          AttributeList al, 
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {
    String textToReturn = "";
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
            //textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + ":" + feat.toString();
          } else {
            try {              
              // TODO: once we return the actual feature vector, convert all numbers to double
              // convert boolean to 1.0 or 0.0 and convert string to Double.parseDouble(string)
              // if(feat instanceof Number) { ...
              double df = Double.parseDouble(feat.toString());
              //textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + "=" + df;
            } catch (NumberFormatException e) {
              logger.warn("LearningFramework: Failed to format numeric feature " + feature + "=" + feat + " as double. Treating as string.");
              //textToReturn = textToReturn + separator + type + ":" + feature + ":r" + i + ":" + feat;
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
  } // extractFeature (AttributeList)

  
  
  
  ///=======================================
  /// HELPER AND UTILITY METHODS
  ///=======================================
  
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
