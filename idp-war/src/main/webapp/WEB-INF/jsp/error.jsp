<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>

<%
final ProfileRequestContext prc = (ProfileRequestContext) request.getAttribute("opensamlProfileRequestContext");
if (!prc.isBrowserProfile()) {
	response.setContentType("text/xml");
	response.setStatus(500);
%>

<S:Fault xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
	<faultcode>S:Server</faultcode>
	<faultstring>SOAP fault</faultstring>
<S:Fault>

<% } else { %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <body>
    
        <h2>ERROR</h2>
        
	<% final RequestContext rc = (RequestContext) request.getAttribute("flowRequestContext");
	   if ( rc != null && rc.getCurrentEvent() != null) { %>
	        <p>ERROR: <%= HTMLEncoder.encodeForHTML(rc.getCurrentEvent().getId()) %></p>
	<% } %>
        
    </body>
</html>

<% } %>