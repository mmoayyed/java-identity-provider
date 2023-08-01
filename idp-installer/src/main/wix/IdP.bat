@echo off
REM Generate msi
setlocal

REM Preconditions

if "%1%" == "" (
  Echo Idp PATH_TO_IDP_DISTRIBUTION
  goto done
)

if not defined WIX (
   echo WIX should be installed
   goto done
)

REM Clear up detritus from last time

if exist idp_contents.wxs (
   del idp_contents.wxs
)

if exist idp-extract (
   rd /q /s idp-extract
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
"%JARCMD%" xf %1 

rem is this a real package?
dir /s install.bat  1> nl:a 2> nl:b
if ERRORLEVEL 1 (
  cd ..
  echo Could not find install.bat in IdP package
  goto done;
)
for /D %%X in (*) do set idpex=%%X

rem The file name gets too big....
rename %idpex% IdPEx
set idpex=IdPEx

rem remove any old embedded detritus
if exist %idpex%\embedded (
   rd /q /s %idpex%\embedded
)

cd ..

"%WIX%/BIN/HEAT" dir idp-extract\%idpex% -platform -gg -dr IDPDISTDIR -var var.idpSrc -cg IdPGroup -out idp_contents.wxs -srd
if ERRORLEVEL 1 goto done

REM Build
"%WIX%/BIN/CANDLE" -nologo -arch x86 -didpSrc=idp-extract\%idpex% idp_contents.wxs
if ERRORLEVEL 1 goto done

"%WIX%/BIN/CANDLE" -nologo -arch x86 -dProjectDir=. ShibbolethIdP-gui.wxs ShibbolethIdP-install-dlg.wxs ShibbolethIdP-warn-dlg.wxs ShibbolethIdP-adconfig-dlg.wxs ShibbolethIdP-update-dlg.wxs
if ERRORLEVEL 1 goto done

"%WIX%/BIN/CANDLE" -nologo -arch x64 -dProjectDir=. ShibbolethIdP-main.wxs ShibbolethIdP-registry.wxs ShibbolethIdP-delete.wxs -ext WixUtilExtension
if ERRORLEVEL 1 goto done

"%WIX%/BIN/LIGHT" -nologo -out idp-x64.msi -ext WixUIExtension ShibbolethIdP-main.wixobj idp_contents.wixobj ShibbolethIdP-registry.wixobj ShibbolethIdP-gui.wixobj ShibbolethIdP-install-dlg.wixobj ShibbolethIdP-adconfig-dlg.wixobj ShibbolethIdP-update-dlg.wixobj ShibbolethIdP-warn-dlg.wixobj ShibbolethIdP-delete.wixobj -ext WixUtilExtension -sice:ICE61
if ERRORLEVEL 1 goto done


dir idp*.msi

REM Tidy up in the Sucessful exit case
   del *.wixobj *.wixpdb
   rd /s /q idp-extract
   rd /s /q idp-jetty-base-extract
   del idp_contents.wxs
:done

