#!/usr/bin/env bash

declare LOCATION

LOCATION=$(dirname $0)

$LOCATION/runclass.sh net.shibboleth.idp.installer.plugin.PluginInstallerCLI $LOCATION/../conf/admin/plugin-installer.xml "$@"
