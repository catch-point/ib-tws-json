#!/bin/bash

BASEDIR=$HOME
PREFIX="$(dirname $0)"
SHELL_JAR="$(find "$PREFIX" -name tws-shell*.jar)"
API_JAR="$(find "$PREFIX" -name TwsApi.jar)"
TWS_EXE="$BASEDIR/Jts/tws"
JAVA_EXE="$(cat "$(dirname "$TWS_EXE")/.install4j/inst_jre.cfg")/bin/java"
TWS_JARS="$(ls -t "$(dirname "$TWS_EXE")/jars"/*.* |xargs |sed 's/ \+/:/g')"
TWS_VMARGS_FILE="$TWS_EXE.vmoptions"
TWS_VMARGS="$(grep -E '^-' "$TWS_VMARGS_FILE" |xargs)"
IBC_ENTRY_POINT="com.meerkattrading.tws.Shell"

exec $JAVA_EXE -cp "$TWS_JARS:$API_JAR:$SHELL_JAR" $TWS_VMARGS "$IBC_ENTRY_POINT" $@

