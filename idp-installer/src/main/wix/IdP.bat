@Echo off
REM Generate msm
setlocal

REM Preconditions

if not defined WIX (
   echo WIX should be installed
   goto done
)

if not exist jetty.msm (
   echo RUN JETTY.BAT FIRST
   goto done
)

REM Clear up detritus from last time

"%WIX%/BIN/CANDLE" -arch x86 -dProjectDir=. idp.wxs
if ERRORLEVEL 1 goto done

"%WIX%/BIN/LIGHT" -out idp.msi -ext WixUIExtension idp.wixobj
if ERRORLEVEL 1 goto done

REM Tidy up in the Sucessful exit case
   del *.wixobj *.wixpdb
   del jetty.msm

:done

