/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.net.IPRange;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Servlet for displaying the status of the IdP. */
public class StatusServlet extends HttpServlet {

    /** Serial version UID. */
    private static final long serialVersionUID = -3484474217955021925L;

    /** Servlet init parameter whose value is a space separated list of CIDR blocks allowed to access this servlet. */
    private static final String IP_PARAM_NAME = "AllowedIPs";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StatusServlet.class);

    /** List of CIDR blocks allowed to access this servlet. */
    private LazyList<IPRange> allowedIPs;

    /** Formatter used when printing date/times. */
    private DateTimeFormatter dateFormat;

    /** Time this servlet started up. */
    private DateTime startTime;

    /** {@inheritDoc} */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        allowedIPs = new LazyList<IPRange>();

        String cidrBlocks = StringSupport.trimOrNull(config.getInitParameter(IP_PARAM_NAME));
        if (cidrBlocks != null) {
            for (String cidrBlock : cidrBlocks.split(" ")) {
                allowedIPs.add(IPRange.parseCIDRBlock(cidrBlock));
            }
        }

        dateFormat = ISODateTimeFormat.dateTimeNoMillis();
        startTime = new DateTime(ISOChronology.getInstanceUTC());
        // TODO incomplete v2 port
        // attributeResolver = HttpServletHelper.getAttributeResolver(config.getServletContext());
        // rpConfigManager = HttpServletHelper.getRelyingPartyConfirmationManager(config.getServletContext());
    }

    /** {@inheritDoc} */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        if (!isAuthenticated(request)) {
            // TODO do we need commons httpclient ?
            // response.sendError(HttpStatus.SC_UNAUTHORIZED);
            response.sendError(401);
            return;
        }

        response.setContentType("text/plain");
        PrintWriter output = response.getWriter();

        printOperatingEnvironmentInformation(output);
        output.println();
        printIdPInformation(output);
        output.println();

        // TODO incomplete v2 port
        // printRelyingPartyConfigurationsInformation(output, request.getParameter("relyingParty"));

        output.flush();
    }

    /**
     * Checks whether the client is authenticated.
     * 
     * @param request client request
     * @return true if the client is authenticated, false if not
     * @throws ServletException if the IP address of the request can not be determined
     */
    protected boolean isAuthenticated(HttpServletRequest request) throws ServletException {
        log.debug("Attempting to authenticate client '{}'", request.getRemoteAddr());
        try {
            InetAddress clientAddress = InetAddress.getByName(request.getRemoteAddr());

            for (IPRange range : allowedIPs) {
                if (range.contains(clientAddress)) {
                    return true;
                }
            }

            return false;
        } catch (UnknownHostException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Prints out information about the operating environment. This includes the operating system name, version and
     * architecture, the JDK version, available CPU cores, memory currently used by the JVM process, the maximum amount
     * of memory that may be used by the JVM, and the current time in UTC.
     * 
     * @param out output writer to which information will be written
     */
    protected void printOperatingEnvironmentInformation(PrintWriter out) {
        Runtime runtime = Runtime.getRuntime();
        DateTime now = new DateTime(ISOChronology.getInstanceUTC());

        out.println("### Operating Environment Information");
        out.println("operating_system: " + System.getProperty("os.name"));
        out.println("operating_system_version: " + System.getProperty("os.version"));
        out.println("operating_system_architecture: " + System.getProperty("os.arch"));
        out.println("jdk_version: " + System.getProperty("java.version"));
        out.println("available_cores: " + runtime.availableProcessors());
        out.println("used_memory: " + runtime.totalMemory() / 1048576 + "MB");
        out.println("maximum_memory: " + runtime.maxMemory() / 1048576 + "MB");
        out.println("start_time: " + startTime.toString(dateFormat));
        out.println("current_time: " + now.toString(dateFormat));
        out.println("uptime: " + (now.getMillis() - startTime.getMillis()) + "ms");
    }

    /**
     * Prints out general IdP information. This includes IdP version, start up time, and whether the attribute resolver
     * is currently operational.
     * 
     * @param out output writer to which information will be written
     */
    protected void printIdPInformation(PrintWriter out) {
        Package pkg = Version.class.getPackage();

        out.println("### Identity Provider Information");
        out.println("idp_version: " + pkg.getImplementationVersion());
        out.println("idp_start_time: " + startTime.toString(dateFormat));
        // TODO incomplete v2 port
        // try {
        // attributeResolver.validate();
        // out.println("attribute_resolver_valid: " + Boolean.TRUE);
        // } catch (AttributeResolutionException e) {
        // out.println("attribute_resolver_valid: " + Boolean.FALSE);
        // }
    }

    // TODO relying party methods
}
