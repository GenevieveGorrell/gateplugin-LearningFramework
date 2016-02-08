/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.data;

import cc.mallet.types.InstanceList;

/**
 * Common base class for Mallet for classification and  Mallet for sequence tagging.
 * @author Johann Petrak
 */
public abstract class CorpusRepresentationMallet extends CorpusRepresentation {
  protected InstanceList instances;

  public InstanceList getRepresentationMallet() { return instances; }
  
  public Object getRepresentation() { return instances; }

}
