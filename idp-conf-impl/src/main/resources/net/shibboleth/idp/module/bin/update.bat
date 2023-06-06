@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.idp.installer.impl.UpdateIdPCLI --home "%~dp0\.." %*
