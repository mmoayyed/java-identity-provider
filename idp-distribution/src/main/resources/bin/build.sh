#!/usr/bin/env bash

declare LOCATION

LOCATION=$0
LOCATION=${LOCATION%/*}

$LOCATION/ant.sh "$@" build-war
