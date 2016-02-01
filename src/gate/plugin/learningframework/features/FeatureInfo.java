/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.learningframework.features;

import cc.mallet.types.Alphabet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This contains the information from the parsed FeatureSpecification plus additional information
 * accumulated during the extraction of a corpus. The additional information is stuff like mappings
 * between the attribute names from the feature specification and the actual names used in the
 * Mallet feature vector, or the mapping between nominal values coded as numeric and the number used
 * to represent the value. Like alphabets, the information in this object can be locked using the
 * stopGrowth() method.
 *
 * @author Johann Petrak
 */
public class FeatureInfo implements Serializable {

  private static final long serialVersionUID = 1;
  protected boolean growthStopped = false;

  public void stopGrowth() {
    growthStopped = true;
  }

  public void startGrowth() {
    growthStopped = false;
  }

  public boolean growthStopped() {
    return growthStopped;
  }

  protected List<Attribute> attributes;

  public FeatureInfo() {
    attributes = new ArrayList<Attribute>();
  }

  public List<Attribute> getFeatureInfo() { return attributes; }
  
  public static class Attribute implements Serializable {

    public String annType;
    public String feature;
    
  }

  public static class SimpleAttribute extends Attribute implements Serializable {

    public SimpleAttribute(String type, String feature,
            Datatype datatype,
            CodeAs codeas,
            MissingValueTreatment missingValueTreatment,
            String missingValueValue,
            String scalingMethod,
            String transformMethod
    ) {
      this.annType = type;
      this.feature = feature;
      this.datatype = datatype;
      this.codeas = codeas;
      this.missingValueTreatment = missingValueTreatment;
      if(datatype == Datatype.nominal && codeas == CodeAs.number) {
        alphabet = new Alphabet();
      }
    }
    public CodeAs codeas = CodeAs.one_of_k;
    public Datatype datatype;
    public MissingValueTreatment missingValueTreatment = MissingValueTreatment.zero_value;
    public Alphabet alphabet;
  }

  public static class Ngram extends Attribute implements Serializable {

    public Ngram(int number, String type, String feature) {
      this.number = number;
      this.annType = type;
      this.feature = feature;
    }

    int number = -1;
  }

  public static class AttributeList extends SimpleAttribute implements Serializable {

    public AttributeList(
            String type, 
            String feature,
            Datatype datatype,
            CodeAs codeas,
            MissingValueTreatment missingValueTreatment,
            String missingValueValue,
            String scalingMethod,
            String transformMethod,
            int from,
            int to
    ) {
      super(type,feature,datatype,codeas,missingValueTreatment,missingValueValue,scalingMethod,transformMethod);
      this.from = from;
      this.to = to;
    }
    
    int from;
    int to;
  }

  public static enum Datatype {
    nominal, numeric, bool;
  }

  public static enum CodeAs {
    one_of_k, number
  }

  /**
   * How to treat/represent missing values when creating the instances.
   *
   * "keep" tries to preserve the missing value for the learning algorithm. This is done by
   * representing the missing value as a value that is not otherwise used: for one_of_k
   * representations, all k features are set to 0.0, for numeric representations of a nominal value,
   * -1 is used, for numeric features, NaN is used and for boolean 0.5 is used. NOTE: this will only
   * work if the algorithm supports missing values in some way!
   *
   * "special_value" replaces the missing value with a special value that should be different from
   * all other values, but still can be handled by algorithms which do not support missing values.
   * This is not really always possible, but the values used are a hopefully good compromise: for
   * nominal features, the value "%%%NA%%%" is used, for numeric values "-1.0" is used and for
   * boolean "0.5" is used if the boolean is represented as a number, otherwise false is used.
   *
   * "zero_value" is similar to "special_value" but uses the "zero" value for the datatype, i.e. the
   * empty string for nominal, false for boolean and 0.0 for numeric.
   *
   * "ignore_instance" records the fact that a missing value is present in the instance and filters
   * the instance. This means that the instance is not used for training and at application time,
   * that no classification is performed for it.
   *
   * "impute_mostfreq" initially uses "keep" but makes another pass over all instances at training
   * time and replaces the value with the most frequent value. This may not be a good idea for
   * truely continuous numeric features. The same value is then also used at application time.
   *
   * "impute_median" initially uses "keep" but makes another pass over all isntances at training
   * time and replaces the value with the median value. At application time, that median value is
   * also used.
   *
   * "use_value" uses the value giving in the element MISSINGVALUEVALUE for the attribute.
   *
   * NOTE: not all treatments are yet implemented!!!!
   *
   */
  public static enum MissingValueTreatment {
    ignore_instance, keep, special_value, zero_value, impute_mostfreq, impute_median, use_value
  }

}
