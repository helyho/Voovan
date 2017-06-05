#!/bin/bash
export GPG_TTY=$(tty)
./mvn-install.sh
cd All
mvn deploy -P release -Dmaven.test.skip=true
cd ../Common
mvn deploy -P release -Dmaven.test.skip=true
cd ../Network
mvn deploy -P release -Dmaven.test.skip=true
cd ../Web
mvn deploy -P release -Dmaven.test.skip=true