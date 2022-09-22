@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.shared.security.impl.BasicKeystoreKeyStrategyTool %*
