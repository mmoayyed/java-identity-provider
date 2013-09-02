<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="net.shibboleth.idp.authn.context.*" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>

<%
RequestContext flowRequestContext = (RequestContext) request.getAttribute("flowRequestContext");
ProfileRequestContext profileRequestContext = (ProfileRequestContext) flowRequestContext.getConversationScope().get("org.opensaml.profile.context.ProfileRequestContext");
AuthenticationContext authnContext = profileRequestContext.getSubcontext(AuthenticationContext.class, false);
AuthenticationErrorContext errorContext = authnContext.getSubcontext(AuthenticationErrorContext.class, false);
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <body>
        
<% if ( errorContext != null && !errorContext.getExceptions().isEmpty()) { %>
		<p>ERROR: <%= errorContext.getExceptions().get(0).getMessage() %></p>
<% } %>
        
        <form action="<%= request.getAttribute("flowExecutionUrl") %>" method="post">
            Username: <input type="text" name="username" value=""/> <br/>
            Password: <input type="password" name="password" value=""/> <br/>
            
            <input type="submit" name="_eventId_proceed" value="Login"/>
        </form>
        
    </body>
</html>