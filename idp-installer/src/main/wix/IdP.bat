@echo off
REM Generate msi
setlocal

REM Preconditions

if "%2%" == "" (
  Echo Idp PATH_TO_IDP_DISTRIBUTION PATH_TO_JETTY_BASE_DISTRO
  goto done
)

if not defined WIX (
   echo WIX should be installed
   goto done
)

if not exist jetty-x64.msm (
   echo RUN JETTY.BAT FIRST
   goto done
)

if not exist jetty-x86.msm (
   echo RUN JETTY.BAT FIRST
   goto done
)

REM Clear up detritus from last time

if exist idp_contents.wxs (
   del idp_contents.wxs
)

if exist idp-extract (
   rd /q /s idp-extract
)

if exist idp-jetty-base-extract (
   rd /q /s idp-jetty-base-extract
)

REM Set up environment

if not defined JAVA_JDK  (
   set JAVA_JDK=%JAVA_HOME%
)

if not defined JAVA_JDK  (
  echo Error: Nether JAVA_JDK nor JAVA_HOME is defined.
  exit /b
)

if not defined JARCMD (
  set JARCMD=%JAVA_JDK%\bin\jar.exe
)

if not exist "%JARCMD%" (
  echo Error: JAVA_HOME is not defined correctly.
  echo Cannot execute %JARCMD%
  goto done
)

REM Test and extract

if not exist %1% (
   echo Error: Could not locate IdP zip %1%
   goto done
)

if not exist %1.asc (
   echo Error: Could not locate signature for IdP zip %1%.asc
   goto noIdPSig
)

gpg --verify %1.asc %1
if ERRORLEVEL 1 (
   echo Error: Signature check failed on %1%
   goto done
)
:noIdPSig
mkdir idp-extract
cd idp-extract
if exist "%1" (
  "%JARCMD%" xf %1 
) else (
  "%JARCMD%" xf ..\%1 
)

rem is this a real package?
dir /s idp.properties 1> nl:a 2> nl:b
if ERRORLEVEL 1 (
  cd ..
  echo Could not find idp.properties in IdP package
  goto done;
)
cd ..

REM Now do the same again for jetty-base
if not exist %2% (
   echo Error: Could not locate jetty base zip %2%
   goto done
)

if not exist %2.asc (
   echo Error: Could not locate signature for jetty base zip %2%.asc
   goto noJettyBaseSig
)

gpg --verify %2.asc %2
if ERRORLEVEL 1 (
   echo Error: Signature check failed on %2%
   goto done
)
:noJettyBaseSig
mkdir idp-jetty-base-extract
cd idp-jetty-base-extract
if exist "%2" (
  "%JARCMD%" xf %2 
) else (
  "%JARCMD%" xf ..\%2
)
rem is this a real package?
dir /s idp.ini 1> nl:a 2> nl:b
if ERRORLEVEL 1 (
  cd ..
  echo Could not find idp.ini in Jetty Base package
  goto done;
)
for /D %%X in (*) do set jettyBaseEx=%%X
rename %jettyBaseEx% JettyBase

cd ..\idp-extract
for /D %%X in (*) do set idpex=%%X

rem we need to create the dist tree ourselves
rem (used to be done by maven, now left to us)
md %idpex%\dist
move %idpex%\conf %idpex%\dist
move %idpex%\flows %idpex%\dist
move %idpex%\messages %idpex%\dist
move %idpex%\views %idpex%\dist
move %idpex%\webapp %idpex%\dist

rem The file name gets too big....
rename %idpex% IdPEx
set idpex=IdPEx

rem remove any old embedded detritus
if exist %idpex%\embedded (
   rd /q /s %idpex%\embedded
)

copy the appropriate jetty base

move ..\idp-jetty-base-extract\JettyBase %idpex%\jetty-base
if ERRORLEVEL 1 (
  cd ..
  echo jetty-base directory not found?
  goto done;
)

rem We want to call jetty-base/start.d jetty-base/start.d.dist

rename  %idpex%\jetty-base\start.d start.d.dist
if ERRORLEVEL 1 (
  cd ..
  echo jetty-base/start.d directory not found?
  goto done;
)
cd ..

"%WIX%/BIN/HEAT" dir idp-extract\%idpex% -platform -gg -dr INSTALLDIR -var var.idpSrc -cg IdPGroup -out idp_contents.wxs -srd
if ERRORLEVEL 1 goto done

REM Build
"%WIX%/BIN/CANDLE" -nologo -arch x86 -didpSrc=idp-extract\%idpex% idp_contents.wxs
if ERRORLEVEL 1 goto done

"%WIX%/BIN/CANDLE" -nologo -arch x86 -dProjectDir=. ShibbolethIdP-registry.wxs ShibbolethIdP-main.wxs ShibbolethIdP-delete.wxs -dmsitype=x86 -ext WixUtilExtension

"%WIX%/BIN/CANDLE" -nologo -arch x86 -dProjectDir=. ShibbolethIdP-gui.wxs ShibbolethIdP-install-dlg.wxs ShibbolethIdP-warndir-dlg.wxs ShibbolethIdP-adconfig-dlg.wxs ShibbolethIdP-update-dlg.wxs
if ERRORLEVEL 1 goto done

"%WIX%/BIN/LIGHT" -nologo -out idp-x86.msi -ext WixUIExtension ShibbolethIdP-main.wixobj idp_contents.wixobj ShibbolethIdP-registry.wixobj ShibbolethIdP-gui.wixobj ShibbolethIdP-install-dlg.wixobj ShibbolethIdP-adconfig-dlg.wixobj ShibbolethIdP-update-dlg.wixobj ShibbolethIdP-warndir-dlg.wixobj ShibbolethIdP-delete.wixobj -ext WixUtilExtension -sice:ICE61
if ERRORLEVEL 1 goto done

del ShibbolethIdP-main.wixobj
del ShibbolethIdP-registry.wixobj
"%WIX%/BIN/CANDLE" -nologo -arch x64 -dProjectDir=. ShibbolethIdP-main.wxs ShibbolethIdP-registry.wxs ShibbolethIdP-delete.wxs -dmsitype=x64 -ext WixUtilExtension
if ERRORLEVEL 1 goto done

"%WIX%/BIN/LIGHT" -nologo -out idp-x64.msi -ext WixUIExtension ShibbolethIdP-main.wixobj idp_contents.wixobj ShibbolethIdP-registry.wixobj ShibbolethIdP-gui.wixobj ShibbolethIdP-install-dlg.wixobj ShibbolethIdP-adconfig-dlg.wixobj ShibbolethIdP-update-dlg.wixobj ShibbolethIdP-warndir-dlg.wixobj ShibbolethIdP-delete.wixobj -ext WixUtilExtension -sice:ICE61
if ERRORLEVEL 1 goto done


dir idp*.msi

REM Tidy up in the Sucessful exit case
   del *.wixobj *.wixpdb
   rd /s /q idp-extract
   rd /s /q idp-jetty-base-extract
   del idp_contents.wxs
:done

