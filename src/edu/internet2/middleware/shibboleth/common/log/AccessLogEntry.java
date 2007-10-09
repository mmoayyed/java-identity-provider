/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.log;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.util.HttpHelper;

/**
 * Data object for generating server access logs.
 */
public class AccessLogEntry {
    
    /** Name of the Shibboleth Access logging category. */
    public static final String ACCESS_LOGGER_NAME = "Shibboleth-Access";
    
    /** Formatter used to convert timestamps to strings. */
    private static DateTimeFormatter dateFormatter = ISODateTimeFormat.basicDateTimeNoMillis();
    
    /** Request timestamp. */
    private DateTime requestTime;

    /** Hostname or IP address of the remote host. */
    private String remoteHost;

    /** Hostname or IP address of the server. */
    private String serverHost;

    /** Port the request came in on. */
    private int serverPort;

    /** Path of the request. */
    private String requestPath;

    /**
     * Constructor.
     * 
     * @param request the request
     */
    public AccessLogEntry(HttpServletRequest request) {
        requestTime = new DateTime();
        remoteHost = request.getRemoteHost();
        serverHost = request.getServerName();
        serverPort = request.getServerPort();
        requestPath = HttpHelper.getRequestUriWithoutContext(request);
    }

    /**
     * Constructor.
     * 
     * @param remote the remote client host name or IP
     * @param host the servers host name or IP
     * @param port the servers port number
     * @param path the request path informatio minus the servlet context information
     */
    public AccessLogEntry(String remote, String host, int port, String path) {
        requestTime = new DateTime();
        remoteHost = DatatypeHelper.safeTrimOrNullString(remote);
        serverHost = DatatypeHelper.safeTrimOrNullString(host);
        serverPort = port;
        requestPath = DatatypeHelper.safeTrimOrNullString(path);
    }

    /**
     * Gets the remote client host or IP address.
     * 
     * @return remote client host or IP address
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Gets the request path without servlet context information.
     * 
     * @return request path without servlet context information
     */
    public String getRequestPath() {
        return requestPath;
    }
    
    /**
     * Gets the time the request was made.
     * 
     * @return time the request was made
     */
    public DateTime getRequestTime(){
        return requestTime;
    }

    /**
     * Gets the server's host name or IP address.
     * 
     * @return server's host name or IP address
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * Gets the server's port number.
     * 
     * @return server's port number
     */
    public int getServerPort() {
        return serverPort;
    }
    
    /** {@inheritDoc} */
    public String toString() {
        StringBuilder entryString = new StringBuilder();

        entryString.append(getRequestTime().toString(dateFormatter.withZone(DateTimeZone.UTC)));
        entryString.append("|");

        entryString.append(getRemoteHost());
        entryString.append("|");

        entryString.append(getServerHost());
        entryString.append(":");
        entryString.append(getServerPort());
        entryString.append("|");

        entryString.append(getRequestPath());
        entryString.append("|");

        return entryString.toString();
    }
}