/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.data;

import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.mallet.LFPipe;
import java.io.File;

/**
 * The base class of all classes that handle the representation of instances.
 * The LearningFramework uses the MalletSeq and Mallet representations whenever possible.
 * The other subclasses so far are only used to convert from Mallet representation for 
 * training, classification or export. 
 * @author Johann Petrak
 */
public abstract class CorpusRepresentation {
  protected FeatureInfo featureInfo;
  protected ScalingMethod scalingMethod;
  protected LFPipe pipe;
  
  /**
   * Returns whatever object the concrete representation uses to represent the instances.
   * In addition, each specific CorpusRepresentation subclass has a representation specific
   * method that returns the correct type of data, e.g. getRepresentationLibSVM or getRepresentationWeka.
   * @return 
   */
  public abstract Object getRepresentation();
  
  /**
   * Write the instances to one or more files.
   * If parms is null, the "default natural format" for that representation is used, otherwise
   * some other format that this representation supports is created, depending on the concrete
   * parameters given.
   * @param directory 
   */
  public abstract void export(File directory, String parms);
  
  /**
   * Remove all instances but leave other information intact.
   * This removes all the instances but retains information about the features/attributes 
   * and how instances should get transformed or scaled.
   */
  public abstract void clear();

  // TODO: it may be good in some situations, if we could import data from external sources
  // directly, but not sure about the details. This is not implemented at the moment at all.
}
