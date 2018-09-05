#!/usr/bin/env bash

declare LOCATION

LOCATION=$0
LOCATION=${LOCATION%/*}

$LOCATION/runclass.sh -Dnet.shibboleth.idp.cli.arguments=net.shibboleth.idp.cli.MetadataQueryArguments \
    net.shibboleth.idp.cli.CLI "$@"
