@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.idp.installer.plugin.impl.PluginInstallerCLI --home "%~dp0\.." %*
