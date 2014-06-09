<!DOCTYPE html>

<%@ taglib uri="urn:mace:shibboleth:2.0:idp:ui" prefix="idpui" %>
<%@ page import="net.shibboleth.idp.authn.context.*" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>
<%@ page import="net.shibboleth.idp.authn.context.AuthenticationContext" %>
<%@ page import="net.shibboleth.idp.authn.context.AuthenticationErrorContext" %>
<%@ page import="net.shibboleth.idp.authn.context.AuthenticationWarningContext" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>


<html>
  <head>
    <meta charset="utf-8" />
    <title>Example Login Page</title>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/css/main.css"/>
  </head>

  <body>
    <div class="wrapper">
      <div class="container">
        <header>
          <a class="logo" href="../images/dummylogo.png"><img src="<%= request.getContextPath()%>/images/dummylogo.png" alt="Replace or remove this logo"/></a>
        </header>
    
        <div class="content">
          <div class="column one">
            <form action="<%= request.getAttribute("flowExecutionUrl") %>" method="post">

                <%
                AuthenticationErrorContext authenticationErrorContext = (AuthenticationErrorContext) request.getAttribute("authenticationErrorContext");
                AuthenticationWarningContext authenticationWarningContext = (AuthenticationWarningContext) request.getAttribute("authenticationWarningContext");

                if (authenticationErrorContext != null || authenticationWarningContext != null) { %>
                <section>
                  <% if ( authenticationErrorContext != null && !authenticationErrorContext.getExceptions().isEmpty()) { %>
		          <p class="form-element form-error">
                    ERROR: <%= HTMLEncoder.encodeForHTML(authenticationErrorContext.getExceptions().get(0).getMessage()) %>
                  </p>
                  <% } %>

                  <% if ( authenticationErrorContext != null && !authenticationErrorContext.getClassifiedErrors().isEmpty()) { %>
                  <p class="form-element form-error">
                    Classified errors: <%= HTMLEncoder.encodeForHTML(authenticationErrorContext.getClassifiedErrors().toString()) %>
                  </p>
                  <% } %>

                  <% if ( authenticationWarningContext != null && !authenticationWarningContext.getClassifiedWarnings().isEmpty()) { %>
                  <p class="form-element form-error">
                    Classified warnings: <%= HTMLEncoder.encodeForHTML(authenticationWarningContext.getClassifiedWarnings().toString()) %>
                  </p>
                  <% } %>
                </section>
              <% }  %>

              <legend>
                Log in to <idpui:serviceName/>
              </legend>

              <section>
                <Label for="username">Username</label>
                <input class="form-element form-field" name="username" type="text" value="">
              </section>

              <section>
                <label for="password">Password</label>
                <input class="form-element form-field" name="password" type="password" value="">
              </section>

              <section>
                <input type="checkbox" name="donotcache" value="1" /> Don't Remember Login
                <button class="form-element form-button" type="submit" name="_eventId_proceed" >Login</button>
              </section>
            </form>
            
             <%
              //
              //    SP Description & Logo (optional)
              //    These idpui lines will display added information (if available
              //    in the metadata) about the Service Provider (SP) that requested
              //    authentication. These idpui lines are "active" in this example
              //    (not commented out) -- this extra SP info will be displayed.
              //    Remove or comment out these lines to stop the display of the
              //    added SP information.
              //
              //    Documentation: 
              //      https://wiki.shibboleth.net/confluence/display/SHIB2/IdPAuthUserPassLoginPage
              //
              //    Example:
            %>
                    <p>
                      <idpui:serviceLogo>default</idpui:serviceLogo>
                      <idpui:serviceDescription>SP description</idpui:serviceDescription>
                    </p>
              

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

      <footer>
        <div class="container container-footer">
          <p class="footer-text">Insert your footer text here.</p>
        </div>
      </footer>
    </div>
    
  </body>
</html>
