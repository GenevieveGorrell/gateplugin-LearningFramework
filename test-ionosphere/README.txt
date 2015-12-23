The document has been created from the ionosphere ARFF dataset that gets distributed
with Weka:
  see e.g. https://github.com/svn2github/weka/blob/master/tags/dev-3-7-3/wekadocs/data/ionosphere.arff

QA numbers for running the training application then the application application for 
SVM, Naive Bayes and RandomForests: since this is classification on the training set, only 
F1.0 strict is really relevent:

Without feature scaling:
LibSVM: 0.9972
RF:     1.0000
NB:     0.8291

With feature scaling:
LibSVM: 0.3590
RF:     0.6410
NB:     0.8291

This values were obtained for
  branch version_1_0, commit 63e734dbc5adf1a7115e8e5d8f1fdaa8ea0f23bb
  branch jp-151218-merge1, commit 2dcaee3fd53066f60e0dd11d371fdda41dde2c2e

