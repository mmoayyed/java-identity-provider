<html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Example Logout Page</title>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/resources/login.css"/>
  </head>
  <body>
    <div class="wrapper">
      <div class="container">
        <header>
          <a class="logo" href="../images/dummylogo.png"><img src="<%= request.getContextPath()%>/resources/dummylogo.png" alt="Replace or remove this logo"/></a>
        </header>

        <div class="content">
          <div class="column one">
    
              <p>This logout page is an example and should be customized.</p>

              <p>This page is displayed when a logout operation at the Identity Provider completes.</p>
    
              <p><strong>It does NOT result in the user being logged out of any of the applications he/she
              has accessed during a session, with the possible exception of a Service Provider that may have
              initiated the logout operation.</strong></p>
        
              <p>If your Identity Provider deployment relies on the built-in Session mechanism for SSO, the
              following is a list of Service Provider identifiers tracked by the session that has been terminated:</p>
    
             <h2> This display is TBD</h2>
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