@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.idp.installer.plugin.PluginInstallerCLI "%~dp0\..\conf\admin\plugin-installer.xml" %*
