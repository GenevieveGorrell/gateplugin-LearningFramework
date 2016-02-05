/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.features;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johann Petrak
 */
public class Ngram extends Attribute implements Serializable, Cloneable {

  public Ngram(String aname, int number, String type, String feature) {
    this.name = aname;
    this.number = number;
    this.annType = type;
    this.feature = feature;
  }
  int number = -1;

  @Override
  public void stopGrowth() {
    /// we do not have any alphabets in an Ngram attribute, do nothing
  }

  @Override
  public void startGrowth() {
    /// we do not have any alphabets, do nothing
  }

  @Override
  public String toString() {
    return "NgramAttribute(type="+annType+
            ",feature="+feature+
            ",number="+number;
  }
  
  @Override
  public Ngram clone() {
      return (Ngram) super.clone();
  }
  
}
