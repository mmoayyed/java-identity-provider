<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <body>
    
        <h2>ERROR</h2>
        
	<% final RequestContext rc = (RequestContext) request.getAttribute("flowRequestContext");
	   if ( rc != null && rc.getCurrentEvent() != null) { %>
	        <p>ERROR: <%= HTMLEncoder.encodeForHTML(rc.getCurrentEvent().getId()) %></p>
	<% } %>
        
    </body>
</html>
