/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.data;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.SparseVector;
import libsvm.svm_node;
import libsvm.svm_problem;

/**
 *
 * @author Johann Petrak
 */
public class CorpusRepresentationLibSVM extends CorpusRepresentation {
  public static svm_problem getFromMallet(InstanceList instances) {
    svm_problem prob = new svm_problem();
    int numTrainingInstances = instances.size();
    prob.l = numTrainingInstances;
    prob.y = new double[prob.l];
    prob.x = new svm_node[prob.l][];

    for (int i = 0; i < numTrainingInstances; i++) {
      Instance instance = instances.get(i);

      //Labels
      prob.y[i] = ((Label) instance.getTarget()).getIndex();

      //Features
      SparseVector data = (SparseVector) instance.getData();
      int[] indices = data.getIndices();
      double[] values = data.getValues();
      prob.x[i] = new svm_node[indices.length];
      for (int j = 0; j < indices.length; j++) {
        svm_node node = new svm_node();
        node.index = indices[j];
        node.value = values[j];
        prob.x[i][j] = node;
      }
    }
    return prob;
  }

}
