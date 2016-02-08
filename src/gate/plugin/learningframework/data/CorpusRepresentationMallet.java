/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.data;

import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.mallet.LFPipe;

/**
 * Common base class for Mallet for classification and  Mallet for sequence tagging.
 * @author Johann Petrak
 */
public abstract class CorpusRepresentationMallet extends CorpusRepresentation {
  protected InstanceList instances;

  public InstanceList getRepresentationMallet() { return instances; }
  
  public Object getRepresentation() { return instances; }
  
  /**
   * Prevent the addition of new features or feature values when instances are added.
   */
  public void stopGrowth() {
    LFPipe pipe = (LFPipe)instances.getPipe();
    pipe.getDataAlphabet().stopGrowth();
    Alphabet ta = pipe.getTargetAlphabet();
    if(ta != null) ta.stopGrowth();
    FeatureInfo fi = pipe.getFeatureInfo();
    fi.stopGrowth();
  }
  
  /**
   * Enable the addition of new features or feature values when instances are added.
   * After a CorpusRepresentationMallet instance is created, growth is enabled by default.
   */
  public void startGrowth() {
    LFPipe pipe = (LFPipe)instances.getPipe();
    pipe.getDataAlphabet().startGrowth();
    Alphabet ta = pipe.getTargetAlphabet();
    if(ta != null) ta.startGrowth();
    FeatureInfo fi = pipe.getFeatureInfo();
    fi.startGrowth();    
  }
  
}
