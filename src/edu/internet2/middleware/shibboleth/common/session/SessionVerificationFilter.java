package edu.internet2.middleware.shibboleth.common.session;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A filter that provides additional checks to help determine if a user's session 
 * may have been hijacked.
 */
public class SessionVerificationFilter implements Filter {

    public void destroy() {
        // TODO Auto-generated method stub

    }

    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException,
            ServletException {
        // TODO Auto-generated method stub

    }

    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub

    }
}