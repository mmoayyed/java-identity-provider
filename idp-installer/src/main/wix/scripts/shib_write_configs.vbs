'
' Code taken from the Shib SP install
'
Dim FileSystemObj, AntFile, PropsFile, JettyFile, JettyAntFile, LogFile
Dim CustomData, msiProperties, InstallDir, TypeLib

Set TypeLib = CreateObject("Scriptlet.TypeLib")
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
LogFile.WriteLine "Installing to " & InstallDirJava
LogFile.WriteLine "Host " & IdPHostName
LogFile.WriteLine "IntallJetty" & InstallJetty

KeyStorePassword=left(CreateObject("Scriptlet.TypeLib").Guid, 38)
SealerPassword=left(CreateObject("Scriptlet.TypeLib").Guid, 38)
SsoStorePassword=left(CreateObject("Scriptlet.TypeLib").Guid, 38)

set AntFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\idp.install.properties" , 2, True)
if (Err.Number = 0 ) then
    AntFile.WriteLine "#"
    AntFile.WriteLine "# File with properties for ANT"
    AntFile.WriteLine "#"
    AntFile.WriteLine "idp.noprompt=yes"
    AntFile.WriteLine "idp.host.name=" & IdpHostName
    AntFile.WriteLine "idp.uri.subject.alt.name=https://" & IdpHostName & "/shibboleth/idp"
    AntFile.WriteLine "idp.keystore.password=" & KeyStorePassword
    AntFile.WriteLine "idp.sealer.password=" & SealerPassword
    AntFile.WriteLine "idp.target.dir=" & InstallDirJava & "/IdP"
    AntFile.WriteLine "idp.merge.properties=idp.install.replace.properties"
    AntFile.WriteLine "#"
    AntFile.WriteLine "# Debug"
    AntFile.WriteLine "#"
    AntFile.WriteLine "#idp.no.tidy=true"
    AntFile.Close
end if

set PropsFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\idp.install.replace.properties" , 2, True)
if (Err.Number = 0 ) then
    PropsFile.WriteLine "#"
    PropsFile.WriteLine "# File to be merged into idp.properties"
    PropsFile.WriteLine "#"
    PropsFile.WriteLine "idp.entityID=https://" & IdpHostName & "/idp/shibboleth"
    PropsFile.WriteLine "idp.sealer.storePassword=" & SealerPassword
    PropsFile.WriteLine "idp.sealer.keyPassword=" & SealerPassword
    PropsFile.Close
else
    LogFile.Writeline "PropsFile failed " & Err & "  -  " & PropsFile
end if

if (InstallJetty <> "") then
    set JettyAntFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\jetty.install.properties" , 2, True)
    if (Err.Number = 0 ) then
	JettyAntFile.WriteLine "#"
	JettyAntFile.WriteLine "# File with properties for ANT"
	JettyAntFile.WriteLine "#"
	JettyAntFile.WriteLine "jetty.merge.properties="& InstallDirJava & "/IdP/jetty.install.replace.properties"
	JettyAntFile.WriteLine "idp.host.name=" & IdpHostName
	JettyAntFile.WriteLine "idp.keystore.password=" & SsoStorePassword
	JettyAntFile.WriteLine "idp.uri.subject.alt.name=https://" & IdpHostName & "/shibboleth/idp"
	JettyAntFile.WriteLine "idp.target.dir=" & InstallDirJava & "/IdP"
	JettyAntFile.WriteLine "#jetty.no.tidy=true"
	JettyAntFile.Close
    else
	LogFile.Writeline "jettyAnt failed " & Err
    end if

    set JettyFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\jetty.install.replace.properties" , 2, True)
    if (Err.Number = 0 ) then
	JettyFile.WriteLine "#"
	JettyFile.WriteLine "# File to be merged into jetty's idp.ini file"
	JettyFile.WriteLine "#"

	JettyFile.WriteLine "jetty.host=0.0.0.0"
	JettyFile.WriteLine "jetty.https.port=443"
	JettyFile.WriteLine "jetty.backchannel.port=8443"
	JettyFile.WriteLine "jetty.backchannel.keystore.path=" & InstallDirJava & "/IdP/creds/idp-backchannel.p12"
	JettyFile.WriteLine "jetty.browser.keystore.path=" & InstallDirJava & "/IdP/creds/idp-userfacing.p12"
	JettyFile.WriteLine "jetty.backchannel.keystore.password=" & KeyStorePassword
	JettyFile.WriteLine "jetty.browser.keystore.password=" & SsoStorePassword
	JettyFile.WriteLine "jetty.backchannel.keystore.type=PKCS12"
	JettyFile.WriteLine "jetty.browser.keystore.type=PKCS12"
	JettyFile.WriteLine "jetty.war.path=" & InstallDirJava & "/IdP/idp.war"
	JettyFile.WriteLine "jetty.jaas.path=" & InstallDirJava & "/IdP/conf/authn/jaas.config"
	JettyFile.Close
    else
	LogFile.Writeline "jetty failed " & Err
    end if
else
   LogFile.WriteLine "NoJetty " & InstallJetty
end if
LogFile.Close