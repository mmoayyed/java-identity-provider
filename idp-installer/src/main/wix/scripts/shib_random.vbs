
'Set WshShell = CreateObject("WScript.Shell")
'WshShell.popup("Hello world")

Set TypeLib = CreateObject("Scriptlet.TypeLib")
JettyPassword=left(TypeLib.Guid, 38)
Session.Property("JETTY_PASS") = JettyPassword
