/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.learningframework.classification;

/**
 *
 * @author Johann Petrak
 */
public class Globals {
  static final String outputClassFeature = "LF_class";
  static final String outputProbFeature = "LF_confidence";
  static final String outputSequenceSpanIDFeature = "LF_seq_span_id";
  //In the case of NER, output instance annotations to temporary
  //AS, to keep them separate.
  static final String tempOutputASName = "tmp_ouputas_for_ner12345";
  static final String savedModelDirectory = "savedModel";
  static final String trainFilename = "trainfile";
}
