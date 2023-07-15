'
' Code taken from the Shib SP install
'
Dim FileSystemObj, AntFile, PropsFile, JettyFile, JettyAntFile, LogFile
Dim CustomData, msiProperties, InstallDir, IdPScope, DebugInstall, Domain
Dim ConfigureAd, AdDomain, AdUser, AdPass, AdUseGC, LDAPFile, LDAPPort
Dim LDAPSearchPath

Set FileSystemObj = CreateObject("Scripting.FileSystemObject")

' Eek 
on error resume next

'Get the Parameters values via CustomActionData
CustomData = Session.Property("CustomActionData")
msiProperties = split(CustomData,";@;")
InstallDir = msiProperties(0)

'Remove all trailing backslashes to normalize
do while (mid(InstallDir,Len(InstallDir),1) = "\")
  InstallDir = mid(InstallDir,1,Len(InstallDir)-1)
loop
set LogFile=FileSystemObj.OpenTextFile(InstallDir & "\idp_installation.Log" , 2, True)

InstallDirJava = Replace(InstallDir, "\", "/")
InstallDirWindows = Replace(InstallDirJava, "/", "\\")

IdPHostName = LCase(msiProperties(1))
InstallJetty = LCase(msiProperties(2))
IdPScope = LCase(msiProperties(3))
if IdPScope = "" then
   Domain = IdPHostName
else
   Domain = IdPScope
end if
DebugInstall = LCase(msiProperties(4))
ConfigureAd = LCase(msiProperties(5))
if ConfigureAd = "true" then
   AdDomain = LCase(msiProperties(6))
   AdUser = LCase(msiProperties(7))
   AdPass = msiProperties(8)
   AdUseGC = LCase(msiProperties(9))
end if

LogFile.WriteLine "Installing to " & InstallDirJava
LogFile.WriteLine "Host " & IdPHostName
LogFile.WriteLine "Domain " & Domain
LogFile.WriteLine "Scope " & IdPScope
LogFile.WriteLine "IntallJetty" & InstallJetty

set AntFile=FileSystemObj.OpenTextFile(InstallDir & "\idp.install.properties" , 2, True)
if (Err.Number = 0 ) then
    AntFile.WriteLine "#"
    AntFile.WriteLine "# File with properties the installer"
    AntFile.WriteLine "#"
    AntFile.WriteLine "idp.noprompt=yes"
    AntFile.WriteLine "idp.host.name=" & IdpHostName
    AntFile.WriteLine "idp.uri.subject.alt.name=https://" & Domain & "/idp"
    AntFile.WriteLine "idp.target.dir=" & InstallDirJava 
    AntFile.WriteLine "idp.merge.properties=" & InstallDirJava & "/idp.install.replace.properties"
    if (IdPScope <> "") then
       AntFile.WriteLine "idp.scope=" & IdPScope
    end if
    if ConfigureAd = "true" then
       AntFile.Writeline "idp.LDAP.credential=" & AdPass
       AntFile.WriteLine "ldap.merge.properties=ldap.mergeProperties"
    end if
    AntFile.WriteLine "#"
    AntFile.WriteLine "# Debug"
    AntFile.WriteLine "#"
    if (DebugInstall <> "") then
        AntFile.WriteLine "idp.no.tidy=true"
    else
        AntFile.WriteLine "#idp.no.tidy=true"
    end if
    AntFile.Close
end if

set PropsFile=FileSystemObj.OpenTextFile(InstallDir & "\idp.install.replace.properties" , 2, True)
if (Err.Number = 0 ) then
    PropsFile.WriteLine "#"
    PropsFile.WriteLine "# File to be merged into idp.properties"
    PropsFile.WriteLine "#"
    PropsFile.WriteLine "idp.entityID=https://" & Domain & "/idp"
    if (IdPScope <> "") then
        PropsFile.WriteLine "idp.scope=" & IdPScope
    end if
    PropsFile.Close
else
    LogFile.Writeline "PropsFile failed " & Err & "  -  " & PropsFile
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