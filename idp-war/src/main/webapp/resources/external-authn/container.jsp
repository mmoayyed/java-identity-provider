<%@page import="net.shibboleth.idp.authn.ExternalAuthentication" %>
<%
final String key = ExternalAuthentication.startExternalAuthentication(request);
request.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, "jdoe");
ExternalAuthentication.finishExternalAuthentication(key, request, response);
return;
%>