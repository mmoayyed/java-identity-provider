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

package net.shibboleth.idp.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * Interface for JCommander command line argument handling for an HTTP-based remote service call. 
 */
public interface CommandLineArguments {
    
    /**
     * Should command usage be displayed?
     * 
     * @return  true iff this is a help request
     */
    boolean isUsage();

    /**
     * Validate the parameter set.
     * 
     * @throws IllegalArgumentException if the parameters are invalid
     */
    void validate();
    
    /**
     * Compute the full URL to connect to.
     * 
     * @return the URL to connect to
     * 
     * @throws MalformedURLException if the URL constructed is invalid 
     */
    @Nonnull public URL buildURL() throws MalformedURLException;
    
    /**
     * Builds the HTTP-Basic value to be used in the Authorization -header, containing username and password.
     *
     * @return The value to be used in the Authorization -header, or null if username or password didn't have a value.
     * 
     * @since 4.2.0
     */
    @Nullable @NotEmpty public default String getBasicAuthHeader() {
        return null;
    }

    /**
     * Values of "header" parameter.
     * 
     * @return header parameter
     * 
     * @since 4.2.0
     */
    @Nullable @NonnullElements @NotLive @Unmodifiable public default Map<String,String> getHeaders() {
        return null;
    }
    
    /**
     * Value of method parameter.
     * 
     * @return HTTP method
     * 
     * @since 4.2.0
     */
    @Nullable @NotEmpty public default String getMethod() {
        return null;
    }

}