#!/bin/bash

if test \! -f pom.xml
then
    echo "Must be run from the directory of the pom.xml file"
    exit 1
fi

KAFKA_VERSIONS="3.5.1 3.6.0"
CURRENT_VERSION="$(grep '<version>' pom.xml | head -1 | sed -re 's/[[:space:]]*<version>(.*)<\/version>[[:space:]]*/\1/')"

cp pom.xml pom.xml.old
for KAFKA_VERSION in $KAFKA_VERSIONS
do
    NEW_VERSION="$CURRENT_VERSION"+"$KAFKA_VERSION"
    echo "$NEW_VERSION"
    mvn versions:set -DnewVersion="$NEW_VERSION"
    mvn versions:commit
    sed -i~ -re 's/<kafka.version>.*<\/kafka.version>/<kafka.version>'"$KAFKA_VERSION"'<\/kafka.version>/' pom.xml
    mvn --batch-mode clean deploy
done
mv pom.xml.old pom.xml
rm -f pom.xml.versionsBackup
mvn --batch-mode clean deploy
