#!/bin/sh
VOOVAN_VERSION=4.3.8

jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path $JAVA_HOME/jmods \
 --add-modules java.base,java.compiler,java.instrument,java.management,java.sql,jdk.attach,jdk.unsupported \
 --output launcher/jvm


mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
mkdir launcher/lib
cp All/target/voovan-framework-$VOOVAN_VERSION.jar launcher/lib/

mkdir launcher/conf
cp -r Web/conf/error-page Web/conf/error.json launcher/conf/
cp Web/conf/web_simple.json launcher/conf/web.json

mkdir launcher/logs

mkdir launcher/WEBAPP

echo "#\!/bin/sh\n./jvm/bin/java -Djdk.attach.allowAttachSelf=true -jar lib/voovan-framework-$VOOVAN_VERSION.jar" > launcher/start.sh
chmod +x launcher/start.sh

echo "#\!/bin/sh\ncat logs/.pid | xargs kill" > launcher/stop.sh
chmod +x launcher/stop.sh

echo "#\!/bin/sh\ncat logs/.pid | xargs kill -9" > launcher/shutdown.sh
chmod +x launcher/shutdown.sh




