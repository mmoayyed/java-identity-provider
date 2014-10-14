@echo off
setlocal

REM Find the necessary resources
set ANT_HOME=%~dp0

REM strip trailing backslash - it confuses java
if "%ANT_HOME:~-1%" == "\" (
  set ANT_HOME=%ANT_HOME:~0,-1%
)

REM We need a JVM
if not defined JAVA_HOME  (
  echo Error: JAVA_HOME is not defined.
  exit /b
)

if not defined JAVACMD (
  set JAVACMD="%JAVA_HOME%\bin\java.exe"
)

if not exist %JAVACMD% (
  echo Error: JAVA_HOME is not defined correctly.
  echo Cannot execute %JAVACMD%
  exit /b
)

if defined CLASSPATH (
  set LOCALCLASSPATH=%CLASSPATH%
)

REM add in the dependency .jar files
set LOCALCLASSPATH=%ANT_HOME%\..\bin\lib\*;%LOCALCLASSPATH%
set LOCALCLASSPATH=%ANT_HOME%\..\webapp\WEB-INF\lib\*;%LOCALCLASSPATH%

if exist %JAVA_HOME%\lib\tools.jar (
    set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\lib\tools.jar
)

if exist %JAVA_HOME%\lib\classes.zip (
    set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\lib\classes.zip
)

%JAVACMD% -cp "%LOCALCLASSPATH%" -Dant.home="%ANT_HOME%" %ANT_OPTS% org.apache.tools.ant.Main -e -f %ANT_HOME%/build.xml %*