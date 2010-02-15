#!/bin/bash

EXTRA=.
for a in lib/*.jar 
do
    EXTRA=$EXTRA:$a
done
export EXTRA

if [ "x${TC_CONFIG_PATH}" == "x" ]
then
    export TC_CONFIG_PATH="localhost:9510"
    echo "NB: Using default TC_CONFIG_PATH="$TC_CONFIG_PATH
    echo "Change this if it is not correct"
fi

if [ "x${TC_INSTALL_DIR}" == "x" ]
then
	echo "Please export your terracotta home directory"
	echo "export TC_INSTALL_DIR=..." 
    echo "Example:" 
    echo "  export TC_INSTALL_DIR=$HOME/app/terracotta-3.0.1/"
	exit 1;
fi


export CMD="-cp ${EXTRA} -Dlog4j.configuration=log4j.properties -Xmx128m org.semispace.space.tutorial.NotifyFromSpace"
${TC_INSTALL_DIR}/bin/dso-java.sh -Dtc.config=${TC_CONFIG_PATH} ${CMD} $*
