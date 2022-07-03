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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.codec.digest.Md5Crypt;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.google.common.base.Strings;

import net.shibboleth.idp.authn.AbstractUsernamePasswordCredentialValidator;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.codec.StringDigester;
import net.shibboleth.utilities.java.support.codec.StringDigester.OutputFormat;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A password validator that authenticates against Apache htpasswd files.
 * 
 * @since 4.0.0
 */
@ThreadSafeAfterInit
public class HTPasswdCredentialValidator extends AbstractUsernamePasswordCredentialValidator {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTPasswdCredentialValidator.class);
    
    /** Digester for SHA-1. */
    @NonnullAfterInit private StringDigester digester;
    
    /** Source of information. */
    @Nullable private Resource htPasswdResource;

    /** File timestamp. */
    @Nullable private long lastModified;
    
    /** In-memory copy of entries. */
    @Nonnull @NonnullElements private final Map<String,String> credentialMap;
    
    /** Constructor. */
    public HTPasswdCredentialValidator() {
        lastModified = 0;
        credentialMap = new ConcurrentHashMap<>();
    }
    
    /**
     * Set the resource to use.
     * 
     * @param resource resource to use
     */
    public void setResource(@Nonnull final Resource resource) {
        throwSetterPreconditionExceptions();
        htPasswdResource = Constraint.isNotNull(resource, "Resource cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        try {
            if (htPasswdResource == null) {
                throw new ComponentInitializationException("Resource cannot be null");
            }

            digester = new StringDigester("SHA1", OutputFormat.BASE64);
            
            try (final InputStream is = htPasswdResource.getInputStream()) {
                credentialMap.putAll(readCredentials(is));
            }

            if (htPasswdResource.isFile()) {
                lastModified = htPasswdResource.lastModified();
            } else {
                htPasswdResource = null;
            }
            
        } catch (final IOException e) {
            throw new ComponentInitializationException("Error reading htpasswd resource", e);
        } catch (final NoSuchAlgorithmException e) {
            throw new ComponentInitializationException("Error creating digester", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        
        final String username = usernamePasswordContext.getTransformedUsername();
        
        final String passwd = credentialMap.get(username);
        if (passwd == null) {
            log.debug("{} Username '{}' not found in password resource", getLogPrefix(), username);
            final LoginException e = new LoginException(AuthnEventIds.UNKNOWN_USERNAME); 
            if (errorHandler != null) { 
                errorHandler.handleError(profileRequestContext, authenticationContext, e,
                        AuthnEventIds.UNKNOWN_USERNAME);
            }
            throw e;
        }

        log.debug("{} Attempting to authenticate user '{}' ", getLogPrefix(), username);
        
        
        if (authenticate(usernamePasswordContext, passwd)) {
            log.info("{} Login by '{}' succeeded", getLogPrefix(), username);
            return populateSubject(new Subject(), usernamePasswordContext);
        }
        
        log.info("{} Login by '{}' failed", getLogPrefix(), username);
        
        final LoginException e = new LoginException(AuthnEventIds.INVALID_CREDENTIALS); 
        if (errorHandler != null) { 
            errorHandler.handleError(profileRequestContext, authenticationContext, e,
                    AuthnEventIds.INVALID_CREDENTIALS);
        }
        throw e;
    }
        
    /**
     * Compare input password to stored value.
     * 
     * @param usernamePasswordContext input context
     * @param storedPassword the stored string
     * 
     * @return true iff the password matches
     */
    @Nonnull private boolean authenticate(@Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nonnull final String storedPassword) {
        
        refreshCredentials();
        
        // test Apache MD5 variant encrypted password
        if (storedPassword.startsWith("$apr1$")) {
            if (storedPassword.equals(Md5Crypt.apr1Crypt(usernamePasswordContext.getPassword(), storedPassword))) {
                return true;
            }
        } else if (storedPassword.startsWith("{SHA}")) {
            if (storedPassword.substring("{SHA}".length()).equals(
                    digester.apply(usernamePasswordContext.getPassword()))) {
                return true;
            }
        } else if (storedPassword.equals(Crypt.crypt(usernamePasswordContext.getPassword(), storedPassword))) {
            return true;
        }

        return false;
    }

    /**
     * Check for file refresh.
     */
    private void refreshCredentials() {
        if (htPasswdResource == null) {
            // Nothing to do.
            return;
        }
        
        try {
            if (htPasswdResource.isFile() && htPasswdResource.exists()
                    && (htPasswdResource.lastModified() > lastModified)) {
                try (final InputStream is = htPasswdResource.getInputStream()) {
                    credentialMap.clear();
                    credentialMap.putAll(readCredentials(is));
                }
            }
        } catch (final IOException e) {
            log.error("{} Error reloading credentials", getLogPrefix(), e);
        }
    }
    
    /**
     * Reads the credentials from stream.
     * 
     * @param is input stream
     * 
     * @return map of credentials
     */
    @Nonnull @NonnullElements private Map<String,String> readCredentials(@Nonnull final InputStream is) {
        
        final Map<String,String> credentials = new HashMap<>();
        
        final Pattern entry = Pattern.compile("^([^:]+):(.+)");
        try (final Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    final Matcher m = entry.matcher(line);
                    if (m.matches()) {
                        final String username = m.group(1);
                        final String password = m.group(2);
                        if (Strings.isNullOrEmpty(username)) {
                            log.warn("{} Skipping line with empty username", getLogPrefix());
                            continue;
                        }
                        if (Strings.isNullOrEmpty(password)) {
                            log.warn("{} Skipping '{}' user with blank password", getLogPrefix(), username);
                            continue;
                        }

                        credentials.put(username.trim(), password.trim());
                    }
                }
            }
        }
        
        log.debug("{} Loaded {} password entries", getLogPrefix(), credentials.size());
        
        return credentials;
    }
    
}