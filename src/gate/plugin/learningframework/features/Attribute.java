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
public abstract class Attribute implements Serializable {

  public String annType;
  public String feature;
  
  public abstract void stopGrowth();
  public abstract void startGrowth();

}
