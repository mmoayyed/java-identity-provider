@echo off
setlocal

"%~dp0\runclass.bat" -Dnet.shibboleth.idp.cli.arguments=net.shibboleth.idp.cli.DumpConfigArguments net.shibboleth.idp.cli.CLI %*
