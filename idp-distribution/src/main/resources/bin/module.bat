@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.idp.cli.impl.ModuleManagerCLI --home "%~dp0\.." %*
