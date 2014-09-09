<!DOCTYPE html>

<%@ taglib uri="urn:mace:shibboleth:2.0:idp:ui" prefix="idpui" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="net.shibboleth.idp.profile.context.MultiRelyingPartyContext" %>
<%@ page import="net.shibboleth.idp.profile.context.RelyingPartyContext" %>
<%@ page import="net.shibboleth.idp.session.SPSession" %>
<%@ page import="net.shibboleth.idp.session.context.LogoutContext" %>
<%@ page import="net.shibboleth.idp.ui.context.RelyingPartyUIContext" %>
<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>

<html>
  <head>
    <meta charset="utf-8" />
    <title>Example Logout Page</title>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/css/main.css"/>
    <style type="text/css">
    iframe#buffer { position:absolute; visibility:hidden; left:0; top:0; }
    </style>
  </head>
  <body>
    <div class="wrapper">
      <div class="container">
        <header>
          <a class="logo" href="../images/dummylogo.png"><img src="<%= request.getContextPath()%>/images/dummylogo.png" alt="Replace or remove this logo"/></a>
        </header>

        <div class="content">
          <div class="column one">
    
            <p>This logout page is an example and should be customized.</p>

            <p>This page is displayed when a logout operation at the Identity Provider completes.</p>
    
            <p><strong>It does NOT result in the user being logged out of any of the applications he/she
            has accessed during a session, with the possible exception of a Service Provider that may have
            initiated the logout operation.</strong></p>
        
		    <%
		    final LogoutContext logoutCtx = (LogoutContext) request.getAttribute("logoutContext");
		    if (logoutCtx != null && !logoutCtx.getSessionMap().isEmpty()) {
		    %>
	            <p>The following is a list of services tracked by the session that has been terminated:</p>
			    <br/>
				<ul>
                <%
                for (final String sp : logoutCtx.getSessionMap().keySet()) {
                    RelyingPartyUIContext rpUIContext = null;
                    final RelyingPartyContext rpCtx =
                        ((MultiRelyingPartyContext) request.getAttribute("multiRPContext")).getRelyingPartyContextById(sp);
                    if (rpCtx != null) {
                        rpUIContext = rpCtx.getSubcontext(RelyingPartyUIContext.class);
                    }
                    if (rpUIContext != null) {
                %>
                        <li><idpui:serviceName uiContext="<%= rpUIContext %>" /></li>
                    <% } else { %>
                        <li><%= HTMLEncoder.encodeForHTML(sp) %></li>
				    <% } %>
				<% } %>
			    </ul>
			<% } %>
          </div>
          <div class="column two">
            <ul class="list list-help">
              <li class="list-help-item"><a href="#"><span class="item-marker">&rsaquo;</span> Forgot your password?</a></li>
              <li class="list-help-item"><a href="#"><span class="item-marker">&rsaquo;</span> Need Help?</a></li>
              <li class="list-help-item"><a href="https://wiki.shibboleth.net/confluence/display/SHIB2/IdPAuthUserPassLoginPage"><span class="item-marker">&rsaquo;</span> How to Customize this Skin</a></li>
            </ul>
          </div>
        </div>
      </div>

      <!-- If flowExecutionUrl set, complete the flow by adding a hidden iframe. -->
      <% if (request.getAttribute("flowExecutionUrl") != null) { %>
          <iframe id="buffer" src="<%= request.getAttribute("flowExecutionUrl") %>&_eventId_proceed" />
      <% } %>

      <footer>
        <div class="container container-footer">
          <p class="footer-text">Insert your footer text here.</p>
        </div>
      </footer>
    </div>
  </body>
</html>