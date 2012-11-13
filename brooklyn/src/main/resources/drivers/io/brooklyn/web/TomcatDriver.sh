#!/bin/bash

TOMCAT_VERSION=7.0.32

${EnvironmentVariables}

function deploy(){
 echo "deploying"
}

function undeploy(){
  echo "undeploying"
}

function customize(){
  echo "Starting Customize ===================================================="
  cd $RUN_DIR/apache-tomcat-$TOMCAT_VERSION
  sed -i.bk s/8080/$HTTP_PORT/g conf/server.xml
  sed -i.bk s/8005/$SHUTDOWN_PORT/g conf/server.xml
  echo "Finished Customize ===================================================="
}

function install() {
  echo "Starting Install ======================================================"
  mkdir -p $RUN_DIR
  cd $RUN_DIR
  echo "Installing in " $RUN_DIR

  if [ -f /tmp/apache-tomcat-$TOMCAT_VERSION.tar.gz ];
  then
     echo "Tomcat already downloaded"
  else
      echo "Downloading Tomcat"
      /opt/local/bin/wget http://mirror.host4site.co.il/apache/tomcat/tomcat-7/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
      echo "Finished Downloading tomcat with exitcode:" $?
  fi

  echo "Unpacking Tomcat"
  /usr/bin/tar -xzf /tmp/apache-tomcat-$TOMCAT_VERSION.tar.gz -C $RUN_DIR

  echo "Finished Install ======================================================"
}

function launch() {
   #service:jmx:rmi:///jndi/rmi://localhost:10000/jmxrmi
   export JAVA_OPTS="-Dcom.sun.management.jmxremote.authenticate=false \
              -Dcom.sun.management.jmxremote.ssl=false \
              -Dcom.sun.management.jmxremote.port=$JMX_PORT"
   echo "Starting Launch ======================================================"
   cd $RUN_DIR/apache-tomcat-$TOMCAT_VERSION
   bin/catalina.sh start
   echo "Finished Launch ======================================================"
}

function stop() {
   echo "Starting Stop ========================================================"
   cd $RUN_DIR/apache-tomcat-$TOMCAT_VERSION
   bin/catalina.sh stop
   echo "Finished Stop ========================================================"
}

function isRunning() {
    echo "isRunning"
}