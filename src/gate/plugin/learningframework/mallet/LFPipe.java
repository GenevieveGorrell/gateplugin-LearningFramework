/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.mallet;

import cc.mallet.pipe.SerialPipes;
import java.io.Serializable;

/**
 * An extended version of the Mallet SerialPipes class which allows us to store
 * some additional important information.
 * This adds methods to store the feature configuration, to associate each entry from the 
 * feature config with one or more features, to associate each feature with its feature config,
 * and to associate features which are nominal and codedas numeric with their value alphabet. 
 * All the additional information is stored in a single container: this container is used when
 * the features get extracted from documents to look up and store the relevant information. 
 * 
 * @author Johann Petrak
 */
public class LFPipe extends SerialPipes implements Serializable {
  private static final long serialVersionUID = 1;
}
