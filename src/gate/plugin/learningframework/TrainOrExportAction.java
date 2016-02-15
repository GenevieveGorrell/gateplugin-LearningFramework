/*
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright 2015 South London and Maudsley NHS Trust and King's College London
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 */
package gate.plugin.learningframework;

import gate.plugin.learningframework.data.CorpusRepresentationLibSVM;
import gate.plugin.learningframework.data.CorpusRepresentationMallet;
import gate.plugin.learningframework.data.CorpusRepresentationWeka;

public enum TrainOrExportAction {
  TRAIN(null),
  EXPORT_ARFF(CorpusRepresentationWeka.class),
  EXPORT_MALLET(CorpusRepresentationMallet.class),
  EXPORT_LIBSVM(CorpusRepresentationLibSVM.class);
  private TrainOrExportAction(Class corpusRepresentationClass) {
    this.corpusRepresentationClass = corpusRepresentationClass;
  }
  private Class corpusRepresentationClass = null;
  public Class getCorpusRepresentationClass() { return corpusRepresentationClass; }
}
