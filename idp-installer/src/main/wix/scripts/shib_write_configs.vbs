'
' Code taken from the Shib SP install
'
Dim FileSystemObj, AntFile, PropsFile, JettyFile
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

InstallDirJava = Replace(InstallDir, "\", "/")
InstallDirWindows = Replace(InstallDirJava, "/", "\\")

IdPHostName = LCase(msiProperties(1))
InstallJetty = LCase(msiProperties(2))

Set TypeLib = CreateObject("Scriptlet.TypeLib")
KeyStorePassword=left(TypeLib.Guid, 38)
SealerPassword=left(TypeLib.Guid, 38)

set AntFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\idp.install.properties" , 2, True)
if (Err.Number = 0 ) then
    AntFile.WriteLine "#"
    AntFile.WriteLine "# File with properties for ANT"
    AntFile.WriteLine "#"
    AntFile.WriteLine "idp.noprompt=yes"
    AntFile.WriteLine "idp.host.name=" & IdpHostName
    AntFile.WriteLine "idp.uri.subject.alt.name=https://" & IdpHostName & "/shibboleth/idp"
    AntFile.WriteLine "idp.keystore.password=" & KeyStorePassword
    AntFile.WriteLine "idp.sealer.password=" & KeyStorePassword
    AntFile.WriteLine "idp.target.dir=" & InstallDirJava & "/IdP"
    AntFile.WriteLine "idp.merge.properties=idp.install.replace.properties"
    AntFile.WriteLine "#"
    AntFile.WriteLine "# Debug"
    AntFile.WriteLine "#"
    AnyFile.WriteLine "idp.no.tidy=true"
    AntFile.Close
end if

set PropsFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\idp.install.replace.properties" , 2, True)
if (Err.Number = 0 ) then
    PropsFile.WriteLine "#"
    PropsFile.WriteLine "# File to be merged into idp.properties"
    PropsFile.WriteLine "#"
    PropsFile.WriteLine "idp.entityID=https://" & IdpHostName & "/idp/shibboleth"
    PropsFile.WriteLine "idp.sealer.storePassword" & SealerPassword
    PropsFile.WriteLine "idp.sealer.keyPassword" & SealerPassword
    PropsFile.Close
end if

if (InstallJetty <> "") then
    set JettyFile=FileSystemObj.OpenTextFile(InstallDir & "\IdP\jetty.install.properties" , 2, True)
    if (Err.Number = 0 ) then
	JettyFile.WriteLine "#"
	JettyFile.WriteLine "# File to be merged into jetty's idp.ini file"
	JettyFile.WriteLine "#"

	JettyFile.WriteLine "jetty.https.port=443"
	JettyFile.WriteLine "jetty.backchannel.port=8443"
	JettyFile.WriteLine "jetty.backchannel.keystore.path=" & InstallDirJava & "/IdP/creds/idp-tls.p12"
	JettyFile.WriteLine "jetty.browser.keystore.path=" & InstallDirJava & "/IdP/creds/idp-tls.p12"
	JettyFile.WriteLine "jetty.backchannel.keystore.password=" & KeyStorePassword
	JettyFile.WriteLine "jetty.browser.keystore.password=" & KeyStorePassword
	JettyFile.WriteLine "jetty.backchannel.keystore.type=PKCS12"
	JettyFile.WriteLine "jetty.browser.keystore.type=PKCS12"
	JettyFile.WriteLine "jetty.war.path=" & InstallDirJava & "/IdP/idp.war"
	JettyFile.WriteLine "jetty.jaas.path=" & InstallDirJava & "/IdP/conf/authn/jaas.config"
	JettyFile.Close
    end if
end if
