#!/usr/bin/env bash

declare LOCATION

LOCATION=$(dirname $0)
NO_PLUGIN_WEBAPP="TRUE"

$LOCATION/runclass.sh net.shibboleth.idp.installer.plugin.impl.PluginInstallerCLI --home "$LOCATION/.." "$@"
