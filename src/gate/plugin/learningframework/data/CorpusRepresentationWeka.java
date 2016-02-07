/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.data;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import weka.core.FastVector;
import weka.core.Instances;

/**
 *
 * @author Johann Petrak
 */
public class CorpusRepresentationWeka extends CorpusRepresentation {
  public static Instances getInstancesFromMallet(InstanceList malletInstances) {
    // TODO!!!!
    Pipe pipe = malletInstances.getPipe();
		FastVector atts = new FastVector();
		for(int i=0;i<pipe.getDataAlphabet().size();i++){
			String attributeName = (String)pipe.getDataAlphabet().lookupObject(i);
			atts.addElement(new weka.core.Attribute(attributeName));
		}
		
                // if the target has a label alphabet, we assume a nominal target otherwise numeric
                if(pipe.getTargetAlphabet() != null) {
		  //Nominal class should be fully expanded out
		  FastVector classVals = new FastVector();
		  for(int i=0;i<pipe.getTargetAlphabet().size();i++){
			classVals.addElement((String)pipe.getTargetAlphabet().lookupObject(i));
  		  }
               
		  weka.core.Attribute classatt = new weka.core.Attribute("class", classVals);
		  atts.addElement(classatt);
		
		  Instances wekaInstances = new Instances("GATE", atts, 0);
		  wekaInstances.setClass(classatt);
                } else {
                  // numeric target
                }
                // TODO: make it compile for now
                return new Instances("TMP",null,0);
  }
  
  
}
