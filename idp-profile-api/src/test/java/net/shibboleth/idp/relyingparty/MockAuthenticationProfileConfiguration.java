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

package net.shibboleth.idp.relyingparty;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.idp.profile.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Mock implementation of {@link AuthenticationProfileConfiguration}. */
public class MockAuthenticationProfileConfiguration extends AbstractProfileConfiguration
        implements AuthenticationProfileConfiguration {

    /** Selects, and limits, the authentication methods to use for requests. */
    @Nonnull @NonnullElements private List<Principal> defaultAuthenticationMethods;

    /** Precedence of name identifier formats to use for requests. */
    @Nonnull @NonnullElements private List<String> nameIDFormatPrecedence;
    
    /**
     * Constructor.
     * 
     * @param id ID of this profile
     * @param methods default authentication methods to use
     */
    public MockAuthenticationProfileConfiguration(@Nonnull @NotEmpty final String id,
            @Nonnull @NonnullElements final List<Principal> methods) {
        this(id, methods, Collections.<String>emptyList());
    }

    /**
     * Constructor.
     * 
     * @param id ID of this profile
     * @param methods default authentication methods to use
     * @param formats name identifier formats to use
     */
    public MockAuthenticationProfileConfiguration(@Nonnull @NotEmpty final String id,
            @Nonnull @NonnullElements final List<Principal> methods,
            @Nonnull @NonnullElements final List<String> formats) {
        super(id);
        setSecurityConfiguration(new SecurityConfiguration());
        setDefaultAuthenticationMethods(methods);
        setNameIDFormatPrecedence(formats);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods() {
        return ImmutableList.copyOf(defaultAuthenticationMethods);
    }
    
    /**
     * Set the default authentication methods to use, expressed as custom principals.
     * 
     * @param methods   default authentication methods to use
     */
    public void setDefaultAuthenticationMethods(@Nonnull @NonnullElements final List<Principal> methods) {
        Constraint.isNotNull(methods, "List of methods cannot be null");
        
        defaultAuthenticationMethods = Lists.newArrayList(Collections2.filter(methods, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence() {
        return ImmutableList.copyOf(nameIDFormatPrecedence);
    }

    /**
     * Set the name identifier formats to use.
     * 
     * @param formats   name identifier formats to use
     */
    public void setNameIDFormatPrecedence(@Nonnull @NonnullElements final List<String> formats) {
        Constraint.isNotNull(formats, "List of formats cannot be null");
        
        nameIDFormatPrecedence = Lists.newArrayList(Collections2.filter(formats, Predicates.notNull()));
    }

}