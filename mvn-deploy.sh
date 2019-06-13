#!/bin/bash
export GPG_TTY=$(tty)

mvn clean deploy -P release -Dmaven.test.skip=true
rm ./All/dependency-reduced-pom.xml