@Echo off
REM Generate msm
setlocal

REM Preconditions

if "%1%" == "" (
  Echo JETTY [APACHE_COMMON_ZIP] [JETTY_DISTRO_ZIP]
  goto done
)
if "%2%" == "" (
  Echo JETTY [APACHE_COMMON_ZIP] [JETTY_DISTRO_ZIP]
  goto done
)

if not defined WIX (
   echo WIX should be installed
   goto done
)

REM Clear up detritus from last time

if exist jetty.wxs (
   del jetty.wxs
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
if exist "%1" (
   "%JARCMD%" xf %1 
) else (
  "%JARCMD%" xf ..\%1 
)
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
   echo Error: Signature check failed on %1%
   goto done
)


mkdir jetty-extract
cd jetty-extract

if exist "%2" (
  "%JARCMD%" xf %2
) else (
  "%JARCMD%" xf ..\%2
)
dir /s jsp.ini 1> nl:a 2> nl:b
if ERRORLEVEL 1 (
  cd ..
  echo "Could not find jsp.ini in Jetty package"
  goto done;
)
cd ..
for /D %%X in (jetty-extract/*) do set jex=%%X
"%WIX%/BIN/HEAT"  dir jetty-extract\%Jex% -platform -gg -dr JETTYROOT -var var.jettySrc -cg JettyGroup -out jetty.wxs -nologo
if ERRORLEVEL 1 goto done

"%WIX%/BIN/CANDLE" -nologo -djettySrc=jetty-extract\%Jex% -dPlatform=x86 -arch x86 jetty.wxs MergeModule.wxs
if ERRORLEVEL 1 goto done

"%WIX%/BIN/LIGHT" -nologo -out Jetty.msm jetty.wixobj mergemodule.wixobj
if ERRORLEVEL 1 goto done

dir Jetty.msm

REM Tidy up in the Sucessful exit case
   rd /q /s procrun-extract
   rd /q /s jetty-extract
   del *.wixobj *.wixpdb

:done

