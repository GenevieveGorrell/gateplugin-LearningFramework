/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.data;

import cc.mallet.types.InstanceList;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.mallet.LFPipe;

/**
 *
 * @author Johann Petrak
 */
public abstract class CorpusRepresentation {
  protected FeatureInfo featureInfo;
  protected ScalingMethod scalingMethod;
  protected LFPipe pipe;
  protected InstanceList instances;

  public InstanceList getInstances() { return instances; }
}
