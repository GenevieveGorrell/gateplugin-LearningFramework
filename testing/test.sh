#!/bin/bash

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
SCRIPTDIR=`cd "$SCRIPTDIR"; pwd -P`

echo $SCRIPTDIR

rm -rf ${SCRIPTDIR}/savedModel
rm -rf ${SCRIPTDIR}/cl-out

java -Dat.ofai.gate.modularpipelines.configFile="lf.config.yaml" \
-cp ${SCRIPTDIR}/evaluate.jar:${GATE_HOME}/bin/gate.jar:${GATE_HOME}/lib/* \
gate.eval.CrossValidate -t ${SCRIPTDIR}/lf-training.xgapp \
-n -1 -d "cl-training" -v ${SCRIPTDIR}/../../../johann-petrak/gateplugin-VirtualCorpus

rm -rf ${SCRIPTDIR}/temp-data-store0000

mkdir logs
mkdir cl-out
cp cl-test/* cl-out

runPipeline.sh -c lf.config.yaml ${SCRIPTDIR}/lf-evaluation.xgapp cl-out

echo "Should be 1010 v 269."
