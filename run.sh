#!/bin/sh
#
#
#
PROJECT=ics-helper
VERSION=0.0.1-SNAPSHOT
./gradlew build -x test
java -jar ./build/libs/$PROJECT-$VERSION.jar
