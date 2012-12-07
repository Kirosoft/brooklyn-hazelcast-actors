#!/bin/bash

#stop the script as soon as we run into an uninitialized variable
set -u

#exit the script as soon as we run into a non 0 exit code.
set -e

${EnvironmentVariables}

function trap_handler(){
    SCRIPTNAME="$0"          # equals to my script name
    LASTLINE="$1"            # argument 1: last line of error occurrence
    LASTERR="$2"             # argument 2: error code of last command
    echo "${SCRIPTNAME}: line ${LASTLINE}: exit status of last command: ${SCRIPTNAME}"
}

trap 'trap_handler ${LINENO} $?' ERR

function deploy(){
    echo "deploying"
}

function undeploy(){
    echo "undeploying"
}

function customize(){
    echo "Starting Customize ===================================================="
    cd $RUN_DIR/apache-tomcat-$VERSION
    sed -i.bk s/8080/$HTTP_PORT/g conf/server.xml
    sed -i.bk s/8005/$SHUTDOWN_PORT/g conf/server.xml
    echo "Finished Customize ===================================================="
}

function install() {
    echo "Starting Install ======================================================"
    mkdir -p $RUN_DIR
    cd $RUN_DIR
    echo "Installing in " $RUN_DIR

    if [ -f /tmp/apache-tomcat-$VERSION.tar.gz ];
    then
         echo "Tomcat already downloaded"
    else
        echo "Downloading Tomcat"
        /opt/local/bin/wget http://mirror.host4site.co.il/apache/tomcat/tomcat-7/v${VERSION}/bin/apache-tomcat-${VERSION}.tar.gz  \
                          --directory-prefix=/tmp
        echo "Finished Downloading tomcat"
    fi

    echo "Unpacking Tomcat"
    /usr/bin/tar -xzf /tmp/apache-tomcat-$VERSION.tar.gz -C $RUN_DIR

    echo "Finished Install ======================================================"
}

function launch() {
    #impl:jmx:rmi:///jndi/rmi://localhost:10000/jmxrmi
    export JAVA_OPTS="-Dcom.sun.management.jmxremote.authenticate=false \
              -Dcom.sun.management.jmxremote.ssl=false \
              -Dcom.sun.management.jmxremote.port=$JMX_PORT"
    echo "Starting Launch ======================================================"
    cd $RUN_DIR/apache-tomcat-$VERSION
    nohup bin/catalina.sh start &
    echo "Finished Launch ======================================================"
}

function stop() {
    echo "Starting Stop ========================================================"
    cd $RUN_DIR/apache-tomcat-$VERSION
    bin/catalina.sh stop
    echo "Finished Stop ========================================================"
}

function isRunning() {
    echo "isRunning"
}