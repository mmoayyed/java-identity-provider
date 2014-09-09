<!DOCTYPE html>
<html>
	<body/>
</html>

<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="org.opensaml.saml.saml2.core.LogoutResponse" %>
<%@ page import="org.opensaml.saml.saml2.core.StatusCode" %>

<%
boolean success = false;
final LogoutResponse logout = (LogoutResponse)
	((ProfileRequestContext) request.getAttribute("opensamlProfileRequestContext")).getInboundMessageContext().getMessage();
if (logout != null && logout.getStatus() != null) {
	final StatusCode sc = logout.getStatus().getStatusCode();
	if (sc != null && StatusCode.SUCCESS_URI.equals(sc.getValue())) {
		success = true;
	}
}

if (success) {
	response.setStatus(HttpServletResponse.SC_OK);
} else {
	response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
}
%>
