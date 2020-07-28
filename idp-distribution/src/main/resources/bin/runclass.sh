#!/usr/bin/env bash

declare LOCATION
declare COMMAND
declare JAVACMD
declare LOCALCLASSPATH

LOCATION=$(dirname $0)

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD=$JAVA_HOME/jre/sh/java
    else
      JAVACMD=$JAVA_HOME/bin/java
    fi
  else
    JAVACMD=$(which java)
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH=$CLASSPATH
fi

if [ -z "$IDP_BASE_URL" ] ; then
  IDP_BASE_URL="http://localhost/idp"
fi

# add in the dependency .jar files
LOCALCLASSPATH="$LOCATION/../dist/webapp/WEB-INF/lib/*":$LOCALCLASSPATH
LOCALCLASSPATH="$LOCATION/../edit-webapp/WEB-INF/lib/*":$LOCALCLASSPATH
LOCALCLASSPATH="$LOCATION/lib/*":$LOCALCLASSPATH

if [ -n "$JAVA_HOME" ] ; then
  if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
fi

"$JAVACMD" '-classpath' "$LOCALCLASSPATH" -Dnet.shibboleth.idp.cli.baseURL=$IDP_BASE_URL -Dnet.shibboleth.idp.cli.idp.home=$LOCATION/.. "$@"