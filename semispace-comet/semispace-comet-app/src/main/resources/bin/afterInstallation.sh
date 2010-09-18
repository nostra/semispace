#!/bin/bash

# Perform tasks which are not performed (easily) by the
# app assembler

executeDirectory=`dirname $0`
# Need to change to the correct directory in order to 
# get relative paths to be correct
echo "Changing directory to: " ${executeDirectory}
cd ${executeDirectory}

chmod a+x *.sh

# Rename jetty jars so that they do not have version number.
# The sed expression is not very good, but as long as it works...
cd ../lib
# Remove dummy jar
rm semispace-comet-app*.jar
# Startup jar goes in parent directory
mv *start*.jar ../start.jar
#for a in `ls | grep "jetty\|start"`
#do
#  moveTo=`echo $a | sed s/-[0-9]*\.[0-9]*\.[0-9]*\.jar/.jar/g`
#  #moveTo=`echo $a | sed s/-[0-9]*\.[0-9]*\.[0-9]*\.v........\.jar/.jar/g`
#  if [ "$moveTo" != "$a" ]
#  then
#    echo "Moving " $a" to " $moveTo
#    mv -i $a $moveTo
#  fi
#done

# Rename also the war file(s)
for a in `ls | grep "\.war"` 
do
  moveTo=`echo $a | sed s/-[0-9]*\.[0-9]*\.[0-9]*[-SNAPSHOT]*\.war/.war/g`
  if [ "$moveTo" != "$a" ]
  then
    # Move the war file(s), it if exists in the wrong directory.
    echo "Moving " $a" to ../webapps/"$moveTo 
    mv -i $a ../webapps/$moveTo
  fi 
done

# Back to the bin directory:
cd ../bin

# Remove place holder files
rm -f ../logs/readme.txt
rm -f ../webapps/readme.txt
rm -f ../tmp/readme.txt

echo
echo "Go to the bin directory."
echo "You start the application by writing (in the bin directory)"
echo "export JETTY_HOME=`pwd`/.."
echo "export JETTY_RUN=`pwd`/../tmp"
echo "export JETTY_LOGS=`pwd`/../logs"
echo "export TMP=`pwd`/../tmp"
echo 'export JAVA_OPTIONS="-server -Djava.awt.headless=true -Xmx768m -Xms256m -XX:PermSize=128m -XX:MaxPermSize=256m -XX:NewSize=64m -XX:MaxNewSize=128m -Dspacecfg=mock"'
echo "./jetty.sh start # In order to background the process."
