#!/bin/bash
export GPG_TTY=$(tty)
mvn clean install -Dmaven.test.skip=true -P release

rm ./All/dependency-reduced-pom.xml