@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.idp.installer.plugin.PluginInstallerCLI --home "%~dp0\.." %*
