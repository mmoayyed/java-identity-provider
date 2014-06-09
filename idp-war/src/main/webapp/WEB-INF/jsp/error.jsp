<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
  	<head>
    	<meta charset="utf-8" />
    	<title>Web Login Service - Error</title>
    	<link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/css/main.css"/>
  	</head>
  	
  	<body>
    <div class="wrapper">
    	<div class="container">
        	<header>
				<img src="<%= request.getContextPath()%>/images/dummylogo.png" alt="Replace or remove this logo"/>
				<h3>Web Login Service - Error</h3>
			</header>
		
        	<div class="content">
			<% final RequestContext rc = (RequestContext) request.getAttribute("flowRequestContext");
			   if ( rc != null && rc.getCurrentEvent() != null) { %>
			        <p>ERROR: <%= HTMLEncoder.encodeForHTML(rc.getCurrentEvent().getId()) %></p>
			<% } %>
    		</div>
    	</div>

      	<footer>
        	<div class="container container-footer">
          		<p class="footer-text">Insert your footer text here.</p>
        	</div>
      	</footer>
      	
    </div>
    </body>
</html>
