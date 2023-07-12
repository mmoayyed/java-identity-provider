Dim FileSystemObj, InstallerPropertyFile, ReplacePropsFile, JettyFile, LogFile
Dim CustomData, InstallDir, IdPScope, DebugInstall, Domain
Dim ConfigureAd, AdDomain, AdUser, AdPass, AdUseGC, LDAPFile, LDAPPort
Dim LDAPSearchPath

Set FileSystemObj = CreateObject("Scripting.FileSystemObject")

' Eek 
on error resume next

InstallDir = Session.Property("INSTALLDIR")

'Remove all trailing backslashes to normalize
do while (mid(InstallDir,Len(InstallDir),1) = "\")
  InstallDir = mid(InstallDir,1,Len(InstallDir)-1)
loop
set LogFile=FileSystemObj.OpenTextFile(InstallDir & "\idp_installation.Log" , 2, True)

InstallDirJava = Replace(InstallDir, "\", "/")
InstallDirWindows = Replace(InstallDirJava, "/", "\\")

IdPHostName = LCase(Session.Property("DNSNAME"))
InstallJetty = LCase(Session.Property("INSTALL_JETTY"))
IdPScope = LCase(Session.Property("IDP_SCOPE"))
if IdPScope = "" then
   Domain = IdPHostName
else
   Domain = IdPScope
end if
DebugInstall = LCase(Session.Property("DEBUG_INSTALL"))
ConfigureAd = LCase(Session.Property("CONFIGURE_AD"))
if ConfigureAd = "true" then
   AdDomain = LCase(Session.Property("AD_DOMAIN"))
   AdUser = LCase(Session.Property("AD_USER"))
   AdPass = Session.Property("AD_PASS")
   AdUseGC = LCase(Session.Property("AD_USE_GC"))
end if

LogFile.WriteLine "Installing to " & InstallDirJava
LogFile.WriteLine "Host " & IdPHostName
LogFile.WriteLine "Domain " & Domain
LogFile.WriteLine "Scope " & IdPScope
LogFile.WriteLine "IntallJetty" & InstallJetty

set InstallerPropertyFile=FileSystemObj.OpenTextFile(InstallDir & "\idp.install.properties" , 2, True)
if (Err.Number = 0 ) then
    InstallerPropertyFile.WriteLine "#"
    InstallerPropertyFile.WriteLine "# File with properties the installer"
    InstallerPropertyFile.WriteLine "#"
    InstallerPropertyFile.WriteLine "idp.noprompt=yes"
    InstallerPropertyFile.WriteLine "idp.host.name=" & IdpHostName
    InstallerPropertyFile.WriteLine "idp.uri.subject.alt.name=https://" & Domain & "/idp"
    InstallerPropertyFile.WriteLine "idp.target.dir=" & InstallDirJava 
    InstallerPropertyFile.WriteLine "idp.merge.properties=" & InstallDirJava & "/idp.install.replace.properties"
    if (IdPScope <> "") then
       InstallerPropertyFile.WriteLine "idp.scope=" & IdPScope
    end if
    if ConfigureAd = "true" then
       InstallerPropertyFile.Writeline "idp.LDAP.credential=" & AdPass
       InstallerPropertyFile.WriteLine "ldap.merge.properties=ldap.mergeProperties"
    end if
    InstallerPropertyFile.WriteLine "#"
    InstallerPropertyFile.WriteLine "# Debug"
    InstallerPropertyFile.WriteLine "#"
    if (DebugInstall <> "") then
        InstallerPropertyFile.WriteLine "idp.no.tidy=true"
    else
        InstallerPropertyFile.WriteLine "#idp.no.tidy=true"
    end if
    InstallerPropertyFile.Close
end if

set ReplacePropsFile=FileSystemObj.OpenTextFile(InstallDir & "\idp.install.replace.properties" , 2, True)
if (Err.Number = 0 ) then
    ReplacePropsFile.WriteLine "#"
    ReplacePropsFile.WriteLine "# File to be merged into idp.properties"
    ReplacePropsFile.WriteLine "#"
    ReplacePropsFile.WriteLine "idp.entityID=https://" & Domain & "/idp"
    if (IdPScope <> "") then
        ReplacePropsFile.WriteLine "idp.scope=" & IdPScope
    end if
    ReplacePropsFile.Close
else
    LogFile.Writeline "ReplacePropsFile failed " & Err & "  -  " & ReplacePropsFile
end if


if ConfigureAd = "true" then

    if AdUseGc= "true" then
        LDAPPort="3268"
        LDAPSearchPath="DC=" &Replace(AdDomain, ".", ", DC=")
    else
        LDAPPort="389"
        LDAPSearchPath="CN=Users, DC=" &Replace(AdDomain, ".", ", DC=")
    end if

    set LDAPFile=FileSystemObj.OpenTextFile(InstallDir & "\ldap.mergeProperties" , 2, True)
    if (Err.Number = 0 ) then
        LDAPFile.Writeline "idp.authn.LDAP.authenticator= adAuthenticator"
        LDAPFile.Writeline "idp.authn.LDAP.ldapURL=ldap://" & AdDomain & ":" & LDAPPort
        LDAPFile.Writeline "idp.authn.LDAP.baseDN=" & LDAPSearchPath
        LDAPFile.Writeline "idp.authn.LDAP.userFilter= (sAMAccountName={user})"
        LDAPFile.Writeline "idp.authn.LDAP.bindDN=" & AdUser
        LDAPFile.Writeline "idp.attribute.resolver.LDAP.searchFilter= (sAMAccountName=$resolutionContext.principal)"
        LDAPFile.Writeline "idp.authn.LDAP.dnFormat= %s@" & AdDomain
        LDAPFile.Close
    else
	LogFile.Writeline "AD Properties failed " & Err
    end if
else
   LogFile.WriteLine "NoAd " & ConfigureAd
end if


LogFile.Close