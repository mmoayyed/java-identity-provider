@echo off
setlocal

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
for %%i in (%ANT_HOME%\build-lib\*.jar) do (
	call %ANT_HOME%\tools\cpappend.bat %%i
)
for %%i in (%ANT_HOME%\lib\*.jar) do (
	call %ANT_HOME%\tools\cpappend.bat %%i
)

if exist %JAVA_HOME%\lib\tools.jar (
    set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\lib\tools.jar
)

if exist %JAVA_HOME%\lib\classes.zip (
    set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\lib\classes.zip
)

%JAVACMD% -cp "%LOCALCLASSPATH%" edu.internet2.middleware.shibboleth.common.attribute.AttributeAuthorityCLI %*