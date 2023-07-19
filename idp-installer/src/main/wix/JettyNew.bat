REM @Echo off
REM Generate msm
setlocal

REM Preconditions

if "%1%" == "" (
  Echo JETTY [APACHE_COMMON_ZIP] [JETTY_DISTRO_ZIP] [JETTY_BASE_ZIP]
  goto done
)
if "%2%" == "" (
  Echo JETTY [APACHE_COMMON_ZIP] [JETTY_DISTRO_ZIP] [JETTY_BASE_ZIP]
  goto done
)
if "%3%" == "" (
  Echo JETTY [APACHE_COMMON_ZIP] [JETTY_DISTRO_ZIP] [JETTY_BASE_ZIP]
  goto done
)

if not defined WIX (
   echo WIX should be installed
   goto done
)

REM Clear up detritus from last time

if exist jetty_contents.wxs (
   del jetty_contents.wxs
)

if exist jetty_base_contents.wxs (
   del jetty_base_contents.wxs
)

if exist idp-jetty-base-extract (
   rd /q /s idp-jetty-base-extract
)

if exist procrun-extract (
   rd /q /s procrun-extract
)

if exist jetty-extract (
   rd /q /s jetty-extract
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
  exit /b
)

REM Test and extract

if not exist "%1%" (
   echo Error: Could not locate procrun zip %1%
   goto done
)

if not exist %1.asc (
   echo Error: Could not locate signature for procrun zip %1%.asc
   goto done
)

gpg --verify %1.asc %1
if ERRORLEVEL 1 (
   echo Error: Signature check failed on %1%
   goto done
)

mkdir procrun-extract
cd procrun-extract
"%JARCMD%" xf ..\%1 
cd ..

if not exist procrun-extract\prunsrv.exe (
   echo could not find prunsrv, is %1% really a procrun source?
   goto done
)

if not exist "%2" (
   echo Error: Could not locate Jetty zip %2%
   goto done
)

if not exist "%2".asc (
   echo Error: Could not locate signature for  jetty zip %2%.asc
   goto done
)

gpg --verify %2.asc %2
if ERRORLEVEL 1 (
   echo Error: Signature check failed on %2%
   goto done
)

REM Now do the same again for jetty-base
if not exist %3% (
   echo Error: Could not locate jetty base zip %2%
   goto done
)

if not exist %3.asc (
   echo Error: Could not locate signature for jetty base zip %3%.asc
   goto noJettyBaseSig
)

gpg --verify %3.asc %3
if ERRORLEVEL 1 (
   echo Error: Signature check failed on %3%
   goto done
)
:noJettyBaseSig
mkdir idp-jetty-base-extract
cd idp-jetty-base-extract
"%JARCMD%" xf ..\%3

rem is this a real package?
dir /s idp.ini.windows 1> nl:a 2> nl:b
if ERRORLEVEL 1 (
  cd ..
  echo Could not find idp.ini.windows in Jetty Base package
  goto done;
)
for /D %%X in (*) do set jettyBaseEx=%%X
rename %jettyBaseEx% jetty-base
cd jetty-base

rem organize the jetty base for extraction

rename start.d start.d.dist
if ERRORLEVEL 1 (
  cd ..
  echo jetty-base/start.d directory not found?
  goto done;
)
rem IDP-1149 make doubley sure that we have a jetty-base\tmp dir
mkdir tmp
echo "keeper" > tmp\.keep

cd ..\..
"%WIX%/BIN/HEAT" dir idp-jetty-base-extract\jetty-base -platform -gg -dr IDP_INSTALLDIR -var var.jettyBaseRoot -cg JettyBaseGroup -out jetty_base_contents.wxs -src
if ERRORLEVEL 1 goto done

mkdir jetty-extract
cd jetty-extract
"%JARCMD%" xf ..\%2

dir /s jetty-ssl-context.xml 1> nl:a 2> nl:b
if ERRORLEVEL 1 (
  cd ..
  echo "Could not find jsp.ini in Jetty package"
  goto done;
)
cd ..

REM IDP-1193 - and kill off demo-base

for /D %%X in (jetty-extract/*) do set jex=%%X
rd /s /q jetty-extract\%jex%\demo-base

REM Extract Jetty
echo %jex% 1> jetty-extract/%jex%/JETTY_VERSION.TXT
"%WIX%/BIN/HEAT"  dir jetty-extract\%Jex% -platform -gg -dr JETTYROOT -var var.JettySrc -cg JettyGroup -out jetty_contents.wxs -nologo -srd
if ERRORLEVEL 1 goto done

Rem tidy
del *.wixobj *.wixpdb

:recompile
REM compile Jetty and procrun contents as well as the main command line

"%WIX%/BIN/CANDLE" -nologo -dJettySrc=jetty-extract\%Jex% -dProcrunSrc=procrun-extract -dPlatform=x86 -arch x86 jetty_contents.wxs Jetty-main.wxs procrun.wxs -ext WixFirewallExtension -ext WixUtilExtension
if ERRORLEVEL 1 goto done

"%WIX%/BIN/CANDLE" -nologo -arch x86 -djettyBaseRoot=idp-jetty-base-extract\jetty-base jetty_base_contents.wxs jetty-delete.wxs -ext WixUtilExtension
if ERRORLEVEL 1 goto done


REM link for x64

"%WIX%/BIN/LIGHT" -nologo -out Jetty-x64.msi jetty_base_contents.wixobj jetty_contents.wixobj procrun.wixobj Jetty-main.wixobj jetty-delete.wixobj -ext WixFirewallExtension -sw1072 -ext WixUtilExtension -sice:ICE61
if ERRORLEVEL 1 goto done

goto done
dir Jetty-*.msi

REM Tidy up in the Sucessful exit case
   rd /q /s procrun-extract
   rd /q /s jetty-extract
   del *.wixobj *.wixpdb
   del jetty_contents.wxs
   del jetty_base_contents.wxs
:done

