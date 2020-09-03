#!/usr/bin/env bash

declare LOCATION

LOCATION=$(dirname $0)

$LOCATION/runclass.sh net.shibboleth.idp.installer.plugin.PluginInstallerCLI --home "$LOCATION/.." $LOCATION/../conf/admin/plugin-installer.xml "$@"
