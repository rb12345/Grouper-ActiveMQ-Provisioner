The 2 files groupDispatcherConfig.txt and grouperDispatcher.properties
need to be placed in the directory /opt/grouperDispatcher/conf/


May need to add activeMQ certificates to the default java keystore on the machine where this runs
scp the 2 files to /tmp/amqCerts directory (will need to create the amqCerts directory)

setenv JAVA_HOME /usr/java/jdk1.7.0_45/jre/
cd /$JAVA_HOME/lib/security

$JAVA_HOME/bin/keytool -import -v -trustcacerts -alias comodoCA -keystore cacerts -file /tmp/amqCerts/COMODOHigh-AssuranceSecureServerCA 

$JAVA_HOME/bin/keytool -import -v -trustcacerts -alias activemq.andrew.cmu.edu -keystore cacerts -file /tmp/amqCerts/activemq.andrew.cmu.edu