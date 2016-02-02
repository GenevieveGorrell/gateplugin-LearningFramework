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
  
  // NOTE: currently these strings are hard-coded but it may make sense to look if there 
  // are better choices or make these configurable in some advanced section of the featureName config
  // file.
  /**
   * Separates the grams in an n-gram with n>1.
   */
  private static final String NGRAMSEP = "_";
  /**
   * Separates the featureName name from the type and also from the attribute kind.
   * Attribute kind is "!N!" for ngram and "!L!" for attribute list, not kind is added for a 
   * simple attribute. Also separates the location index or ngram number.
   */
  private static final String NAMESEP = ":";
  /**
   * Separates the featureName name part from the value part for featureName names of features that 
 indicate the presence of that value.
   * For example a simple attribute extracted from annotation "Person" from featureName "category" 
 with the value "NN" has the instance featureName name "Person:category:NN";
   */
  private static final String VALSEP = "=";
  
  private static final String MVVALUE = "%%%NA%%%";
  
  private static Logger logger = Logger.getLogger(FeatureExtraction.class.getName());

  public static void extractFeature(        
          Instance inst,
          Attribute att,
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {
    if(att instanceof AttributeList) extractFeature(inst,(AttributeList)att,inputASname,instanceAnnotation,doc);
    else if(att instanceof SimpleAttribute) extractFeature(inst,(SimpleAttribute)att,inputASname,instanceAnnotation,doc);
    else if(att instanceof Ngram) extractFeature(inst,(Ngram)att,inputASname,instanceAnnotation,doc);
    else {
      throw new GateRuntimeException("Attempt to call extractFeature with type "+att.getClass());
    }
  }
  
  
  /**
   * Extract the instance features for a simple attribute.
   * 
   * This adds the internal features to the inst object and also updates any 
 alphabets, if they allow growth.
 If the annotation types of the instance annotation and the annotation specified for the attribute
 are the same, then the instance annotation is directly used, otherwise, if there is a single
 overlapping annotation of the annType specified in the attribute, that one is used.
 If there is no overlapping annotation, nothing is extracted for that instance and implicitly,
 all features are set to 0.0 (TODO: this should probably get treated inputAS if all features were
 missing so that for each featureName, the proper missing value treatment can be applied!)
 If there are several overlapping annotations, an exception is thrown, this should not happen.
 
 If no featureName is specified, we give an indicator of the presence, artificial featureName name 
 for binary featureName. 
   * 
   * 
   * @param inst
   * @param att
   * @param inputASname
   * @param instanceAnnotation
   * @param doc
   */
  private static void extractFeature(
          Instance inst,
          SimpleAttribute att,
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {
    String value = "null";
    /*Although the user needn't specify the annotation annType if it's the
     * same inputAS the instance, they may do so. It's intuitive that if they
     * do so, they mean to extract the featureName from the instance, not just
     * the first colocated same annType annotation. This matters in
     * disambiguation, where we have many colocated same annType annotations.
     * Fix it up front by wiping out annType if it's the same inputAS the instance.
     */
    AugmentableFeatureVector fv = (AugmentableFeatureVector) inst.getData();
    String annType = att.annType;
    String featureName = att.feature;
    MissingValueTreatment mvt = att.missingValueTreatment;
    CodeAs codeas = att.codeas;
    Datatype dt = att.datatype;
    Alphabet alphabet = att.alphabet;
    // first of all get the annotation from where we want to construct the annotation.
    // If the annType is the same inputAS the annType of the instance annotation, use the 
    // instance annotation directly. Otherwise, use an annotation of annType annType that overlaps
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
      } 
      if(overlappings.size() == 0) {
        // if there is no overlapping annotation of annType annType, we simply do nothing
        // TODO: handle this inputAS if all features have missing values!!!!!!
        return;
      }
      // we have exactly one annotation, use that one
      sourceAnnotation = gate.Utils.getOnlyAnn(overlappings);
    }
    // NOTE: there should be no way of how a featureName we encounter now is already in the featureName
    // vector, so we do not even check, we simply add the featureName.
    // How we add the featureName depends on the datatype, on the codeas setting if it is nominal,
    // and also on how we treat missing values.
    extractFeatureWorker(inst,sourceAnnotation,doc,annType,featureName,alphabet,dt,mvt,codeas);
  }
    
  private static void extractFeatureWorker(
          Instance inst,
          Annotation sourceAnnotation,
          Document doc,
          String annType,
          String featureName,
          Alphabet alphabet,
          Datatype dt,
          MissingValueTreatment mvt,
          CodeAs codeas)  {
    
    AugmentableFeatureVector fv = (AugmentableFeatureVector)inst.getData();
    // if the featureName name is empty, then all we want is indicate the presence of the annotation
    // inputAS a boolean. No matter what the datatype is, this is always indicated by setting the
    // featureName to 1.0 (while for all instances, where the annotation is missing, the value will
    // implicitly be set to 0.0). 
    if(featureName==null||featureName.isEmpty()) {
      // construct the featureName name and set to 1.0
      // however, only add the featureName if the featureName alphabet is allowed to grow.
      String fname = annType + NAMESEP + NAMESEP + "ISPRESENT";
      addToFeatureVector(fv, fname, 1.0);
    } else {    
      // First get the value inputAS an Object, if there is no value, we have an Object that is null
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
            // TODO: do we have to escape the featureName name in some way here?
            addToFeatureVector(fv, annType+NAMESEP+featureName+VALSEP+val, 1.0);
          } else {
            // we have a missing value, check the missing value treatment for what to do now
            switch(mvt) {
              case ignore_instance: // this is handled elsewhere, nothing to do
                break;
              case keep:  // this represents the MV by not setting any indicator featureName, so nothing to do
                break;
              case zero_value: // for one-of-k we treat this identically to keep, nothing to do
                break;
              case special_value: // we use the predefined special value
                addToFeatureVector(fv,annType+NAMESEP+featureName+VALSEP+MVVALUE,1.0);
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
              // add the featureName, using the value we have stored for it, but only if the featureName
              // itself can be added
              addToFeatureVector(fv, annType+NAMESEP+featureName, alphabet.lookupIndex(val));
            } else {
              // we have not seen this value: if the alphabet is allowed to grow add it and
              // then try to add the featureName, otherwise, do nothing
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
          // just add the value, if possible and if the object can be interpreted inputAS a number
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
          // TODO!!
            // we have a numeric missing value!
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
            // we have a missing boolean value
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
                addToFeatureVector(fv,annType+NAMESEP+featureName,0.5);
                break; 
              default:
                throw new NotImplementedException("MV-Handling");
            }                                  
          
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
  // If there are at least n annotations inputAS speficied by the Ngam TYPE contained in the span of 
  // the instance annotation, then those annotations are arranged in document order and
  // - starting with the second index, up to the last
  // - 
  // CHECK: if we get the same ngram multiple times, we should have a count!!! e.g. unigram "fred" three
  // times we should have 3.0
  // TODO: check what to do if the contained annotations are not in non-overlapping order: should we
  // create an ngram if the second annotations starts before the end of the first or even at the same 
  // offset inputAS the first? If that is the case, what should the order of the annotations then be?
  // NOTE: if the featureName is missing, i.e. it is null or the empty string, then the whole annotation gets ignored
  private static void extractFeature(
          Instance inst,
          Ngram ng, 
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {
    AugmentableFeatureVector fv = (AugmentableFeatureVector) inst.getData();
    int number = ng.number;
    String annType = ng.annType;
    String featureName = ng.feature;
    AnnotationSet inputAS = doc.getAnnotations(inputASname);
    // TODO: this we rely on the ngram only having allowed field values, e.g. annType
    // has to be non-null and non-empty and number has to be > 0.
    // If featureName is null, then for ngrams, the string comes from the covered document
      String[] gram = new String[number];
      List<Annotation> al = Utils.getContainedAnnotations(inputAS, instanceAnnotation, annType).inDocumentOrder();
      // If we have less annotations than our n for n-gram, there is certainly nothing to do, 
      // leave the featureName vector untouched.
      if (al.size() < number) return;
        // this will hold the actual token strings to use for creating the n-grams
        List<String> strings = new ArrayList<String>();
        for(Annotation ann : al) {
          // for ngrams we either have a featureName name 
          if(featureName != null) {
            // NOTE: if the featureName is not a string, we convert it to string
            Object obj = ann.getFeatures().get(featureName);
            // if there is no value at all, then the annotation is ignored
            if(obj!=null) {
              String tmp =obj.toString().trim();
              // if the resulting string is empty, it is also ignored 
              if(!tmp.isEmpty()) {
                strings.add(tmp);
              }
            }
          } else {
            // if the featureName is null, we get the string from the cleaned document text
            String tmp = gate.Utils.cleanStringFor(doc, ann).trim();
            if(!tmp.isEmpty()) {
              strings.add(tmp);
            }
          }
        } // for Annotation ann : al
        // Now construct the actual ngrams and add them to the augmentable featureName vector. 
        // In the process, check first if such a featureName is already there, and if yes, just 
        // increment the value.
        // To avoid overhead, we only create the ngrams on the fly// Now construct the actual ngrams and add them to the augmentable feature vector. 
        // In the process, check first if such a feature is already there, and if yes, just 
        // increment the value.
        // To avoid overhead, we only create the ngrams on the fly
        
        // first check if our strings array is actually big enough so we can create at least one n-gram
        if(strings.size()<number) return;
        
        // now create the ngrams inputAS follows: starting with the first element in strings, go
        // through all the elements up to the (size-n)ths and concatenate with the subsequent 
        // n stings using the pre-defined separator character.
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < (strings.size()-number+1); i++ ) {
          sb.setLength(0);
          for(int j = 0; j < number; j++) {
            if(j!=0) sb.append(NGRAMSEP);
            sb.append(strings.get(i+j));
          }
          String ngram = sb.toString();
          // we have got our ngram now, count it, but only add if we are allowed to!
          addToFeatureVector(fv, annType+NAMESEP+featureName+NAMESEP+"!N!"+NAMESEP+number+VALSEP+ngram, 1.0);
        }
  } // extractFeature(NGram)
  
  
  private static void extractFeature(
          Instance inst, 
          AttributeList al, 
          String inputASname, 
          Annotation instanceAnnotation, 
          Document doc) {

    AugmentableFeatureVector fv = (AugmentableFeatureVector) inst.getData();

    Datatype dt = al.datatype;
    String annType = al.annType;
    String featureName = al.feature;
    int from = al.from;
    int to = al.to;
    Alphabet alphabet = al.alphabet;
    MissingValueTreatment mvt = al.missingValueTreatment;
    CodeAs codeas = al.codeas;
    AnnotationSet inputAS = doc.getAnnotations(inputASname);
    long centre = instanceAnnotation.getStartNode().getOffset();
    List<Annotation> annlistforward = inputAS.get(annType, centre, doc.getContent().size()).inDocumentOrder();
    List<Annotation> annlistbackward = inputAS.get(annType, 0L, centre).inDocumentOrder();
    // go through each of the members in the attribute list and get the annotation
    // then process each annotation just like a simple annotation, only that the name of 
    // featureName gets derived from this list attribute plus the location in the list.
    for (int i = from; i <= to; i++) {
      Annotation ann = null;
      if (i < 0) {
        if (-i <= annlistbackward.size()) {
          ann = annlistbackward.get(annlistbackward.size() + i);
        }
      } else if (i < annlistforward.size()) {
          ann = annlistforward.get(i);
          // make compiler happy for now
          //textToReturn = textToReturn + separator + annType + ":" + featureName + ":r" + i + ":" + extractFeature(annType, featureName, datatype, inputASname, ann, doc);
      }
      if(ann != null) {
        // now extract the actual featureName for that entry:
        String featureNamePrefix = annType + NAMESEP + "!L!" + NAMESEP + i;
        extractFeatureWorker(inst,ann,doc,featureNamePrefix,featureName,alphabet,dt,mvt,codeas);    
      }
    }
  } // extractFeature (AttributeList)

  
  
  
  ///=======================================
  /// HELPER AND UTILITY METHODS
  ///=======================================
  
  /** 
   * Same inputAS the method, but makes sure a non-growable Alphabet is considered.
   * @param fv
   * @param key
   * @param val 
   */
  private static void addToFeatureVector(AugmentableFeatureVector fv, Object key, double val) {
    Alphabet a = fv.getAlphabet();
    if(!a.contains(key) && a.growthStopped()) return;
    fv.add(key,val);
  }
        
        
  // NOTE: we use an AugmentableFeatureVector to represent the growing featureName vector inputAS we 
  // build it.
  // The Mallet documentation is close to non-existing ATM, so here is what the methods we use do:
  // afv.add("x",val) adds val to whatever the current value for "x" is oder adds the featureName, if
  //   the Alphabet can grow. If the Alphabet cannot grow, the method does nothing.
  //   UPDATE: this does not work! if one tries to do that, the indices get messed up and 
  //   the fv will throw an ArrayIndexOutOfBoundsException!!!!
  //   So we have always to explicitly check if the featureName is in the alphabet!!!
  //   UPDATE: Mallet uses assert for checking things like this, so if assertsions are not enable,
  //   no exception is thrown until it is too late!
  // afv.value("x") retrieves the value if "x" is in the vector, otherwise an exception is thrown,
  //   even if "x" is in the alphabet. 
  // afv.contains("x") is true if the featureName vector contains a value for "x" (which implies it must
  //   be in the alphabet)
  // afv.getAlphabet().contains("x") is true if "x" is in the alphabet. 
        
  

}
