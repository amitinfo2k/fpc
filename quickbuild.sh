#!/bin/bash
MVNEXEC=mvn
cd impl
$MVNEXEC clean install -DskipTests
cd ../karaf
$MVNEXEC clean install -DskipTests
