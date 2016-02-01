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
public class Ngram extends Attribute implements Serializable {

  public Ngram(int number, String type, String feature) {
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

}
