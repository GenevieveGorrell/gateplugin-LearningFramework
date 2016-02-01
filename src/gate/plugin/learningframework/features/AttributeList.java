/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.features;

import java.io.Serializable;

/**
 *
 * @author Johann Petrak
 */
public class AttributeList extends SimpleAttribute implements Serializable {

  public AttributeList(String type, String feature, Datatype datatype, CodeAs codeas, MissingValueTreatment missingValueTreatment, String missingValueValue, String scalingMethod, String transformMethod, int from, int to) {
    super(type, feature, datatype, codeas, missingValueTreatment, missingValueValue, scalingMethod, transformMethod);
    this.from = from;
    this.to = to;
  }
  int from;
  int to;
  
  // NOTE: this inherits the alphabet from SimpleAttribute: even though this object represents a 
  // whole set of features, the alphabet gets shared by all of them!

}
