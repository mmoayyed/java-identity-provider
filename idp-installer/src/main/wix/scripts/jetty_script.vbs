' c:\program file\foo\bar/idp/ -> c:/program file/foo/bar/idp
newHome = Replace(Session.Property("IDP_INSTALLDIR"), "\", "/")
if (Right(newHome, 1) = "/") then
    Session.Property("JAVA_IDP_HOME") = Left(newHome, Len(newHome) -1)
else 
    Session.Property("JAVA_IDP_HOME") = newHome
end if

' create the jetty password while we are at it

Set TypeLib = CreateObject("Scriptlet.TypeLib")
JettyPassword=left(TypeLib.Guid, 38)
' Prefix the property with the MM GUID....
Session.Property("JETTY_PASS") = JettyPassword
