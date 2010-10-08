#!/bin/bash

EXTRA=.
for a in lib/*.jar 
do
    EXTRA=$EXTRA:$a
done
export EXTRA

if [ "x${TC_CONFIG_PATH}" == "x" ] && [ "x$1" == "x" ]
then
    export TC_CONFIG_PATH="localhost:9510"
    echo "NB: Using default TC_CONFIG_PATH="$TC_CONFIG_PATH
    echo "Change this if it is not correct"
    echo ""
fi

if [ "x${TC_INSTALL_DIR}" == "x" ] && [ "x$1" == "x" ]
then
	echo "Please export your terracotta home directory"
	echo "export TC_INSTALL_DIR=..." 
    echo "Example:" 
    echo "  export TC_INSTALL_DIR=$HOME/app/terracotta-3.0.1/"
    echo ""
    echo "If you intend to run in a standalone manner, supply endpoint"
    echo "For instance:"
    echo $0 " http://localhost:8080/semispace-google/services/tokenspace"
    exit 1;
fi


export CMD="-Xmx512m -cp ${EXTRA} -Djava.library.path=lib -Xmx128m org.semispace.googled.GoogleMain"
if [ "x$1" == "x" ]
then 
    echo "Running with terracotta. Supply an argument to run via localhost webservices"
    echo "********************************************************************************"
    echo "NOTICE: The spring configuration in the webapp make the connection fail."
    echo "The following is just an illustration, which presently does not work"
    echo "This may be corrected in a later version of either "
    echo "Terracotta or Semispace."
    echo "********************************************************************************"
    ${TC_INSTALL_DIR}/platform/bin/dso-java.sh -Dcom.tc.loader.system.name="Jetty.path:/semispace-google" -Dtc.config=${TC_CONFIG_PATH} ${CMD}
    
else
    echo "Running with endpoint: " $1
    echo "Examples of an endpoint are:"
    echo " mvn jetty:run   http://localhost:8080/google/services/tokenspace"
    echo " standalone app  http://localhost:8080/semispace-google/services/tokenspace"
    java ${CMD} $1
fi
