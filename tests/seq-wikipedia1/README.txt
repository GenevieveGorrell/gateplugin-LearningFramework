Small and simple corpus for testing sequence tagging

A few random wikipedia articles, processed with the prepare.xgapp pipeline
which does basic ANNIE plus adds "Link" annotations for interwiki links.

We try to learn a model for predicting the links and run it on the training set.

Evaluation:

Before merge/refactoring:
Mallet CRF: Micro summary	47	4565	113	84	0.1926	0.0100	0.0190	0.5369	0.0279	0.0530

After merge/refactoring:
Mallet CRF: Micro summary	1235	3240	622	221	0.5943	0.2630	0.3646	0.7007	0.3101	0.4299

So for some reason, the old one does seem to do much worse here??

