/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.learningframework.engines;

/**
 * All algorithms implement this interface.
 * @author johann
 */
public interface Algorithm {
  public Class getTrainerClass();
  public Class getEngineClass();
}
