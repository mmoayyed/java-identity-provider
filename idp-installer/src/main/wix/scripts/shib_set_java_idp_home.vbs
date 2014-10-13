' c:\program file\foo\bar -> c:/program file/foo/bar/idp
newHome = Replace(Session.Property("INSTALLDIR"), "\", "/") & "/IdP"
Session.Property("JAVA_IDP_HOME") = Replace(newHome, "//", "/")
