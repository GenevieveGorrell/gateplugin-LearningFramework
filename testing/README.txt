
QA numbers for running the sequence training app, the running the sequence application
app and evaluating Key vs LearningFramework, type Mention (no feature):

This values were obtained for 
  branch version_1_0, commit 63e734dbc5adf1a7115e8e5d8f1fdaa8ea0f23bb
  branch master, commit 5fb8195a6df73a72eb823d28db19d6b9c68f78bf
  branch jp-151218-merge1, commit 2dcaee3fd53066f60e0dd11d371fdda41dde2c2e

	312	887	150	97	0.5581	0.2407	0.3364	0.7317	0.3156	0.4410

Update 2016-01-19:

Sequence tagging pipelines, values obtained for first running lf-sequence-training.xgapp then
lf-sequence-application.xgapp 
  branch version_1_0, commit f00bd394b3a0e5bb1dada9cbf292948bd0409d8b
    Micro summary	312	887	150	97	0.5581	0.2407	0.3364	0.7317	0.3156	0.4410

  branch jp-151218-merge1 commit 946cefbd681d0c1bf3c440147cd4e474e2db10c1
    Micro summary	312	887	150	97	0.5581	0.2407	0.3364	0.7317	0.3156	0.4410 

Sequence tagging as above, but using the LIBSVM algorithm instead
   version_1_0
     Micro summary	0	1296	0	0	1.0000	0.0000	0.0000	1.0000	0.0000	0.0000
   jp-151218-merge1
     Micro summary	0	1296	0	0	1.0000	0.0000	0.0000	1.0000	0.0000	0.0000
