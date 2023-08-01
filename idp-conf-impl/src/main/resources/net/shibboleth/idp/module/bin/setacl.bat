@echo off
setlocal

REM
REM EXAMPLE batch file to set restrictive ACLs on a Shibboleth IdP installation.
REM
REM You should consider this a sample rather than set in stone and adapt it for
REM your own use
REM
REM Two optional Parameters:
REM    The first is the ID to be given explicit read access to the configuration
REM    and write access to the logs.  This could be the OD or a low priv user you
REM    run the container as
REM
REM    The second is the ID to be given ownership of the files.  This finesses an
REM    issue wherebywhich happen if the owner of the files is not given access.
REM    The directory tree then becaomes an unmaintainable mess.
REM
REM    Defaults to 'Administrators'
REM

if "%2%" EQU "" (
   set OWNER_ID=Administrators
) else (
   set OWNER_ID=%2%
)

REM
REM First up, take ownership
REM   /t means recursive

echo Setting owner to %OWNER_ID%
icacls %~dp0\.. /t /setowner %OWNER_ID%
if ERRORLEVEL 1 (
   echo Error: Could not set ownership
   goto done
)

if "%1%"=="" (
   REM Set the ACLS Default ACLS
   REM   /t recursive
   REM   /inheritance:r Remove inhetited ACLS
   REM   /grant:r SYSTEM:F Administrators:F Full access for SYSTEM&Administrators (replacing any existing)

   echo Setting ACL for SYSTEM and Administrators
   icacls %~dp0\.. /t /inheritance:r /grant:r SYSTEM:F Administrators:F
) else (
   REM As above, but add read for the supplied user
   REM GR=GENERIC_READ RD=READ_DATA/ENUMERATE_DIR X=EXECUTE/TRAVERSE_DIR
   echo Setting ACL for SYSTEM, Administrators and %OWNER_ID%
   icacls %~dp0\.. /t /inheritance:r /grant:r SYSTEM:F Administrators:F "%1%:(GR,RD,X)"
   if ERRORLEVEL 1 (
      echo Error: Could not set ACL
      goto done
   )
   REM And the logs (which may not be present yet)
   icacls %~dp0\..\logs /t /grant:r SYSTEM:F Administrators:F %1%:F
)
if ERRORLEVEL 1 (
   echo Error: Could not set ACL
   goto done
)

:done
