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

package net.shibboleth.idp.saml.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import net.shibboleth.idp.saml.xmlobject.Scope;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A container for all the {@link Scope} elements (attached to either a 
 * {@link EntityDescriptor}, {@link IDPSSODescriptor} or 
 * {@link AttributeAuthorityDescriptor}). 
 */
public class ScopesContainer {

    /** The (non Regexp) scopes. */
    @Nonnull private Set<String> simpleScopes = Collections.EMPTY_SET;
    
    /** The Regexp scopes. */
    @Nonnull private List<Predicate<String>> regexpScopes = Collections.EMPTY_LIST;
    
    /** Sets the non-regexp Scopes.
     * <br> We force the input to be a set so as to enforce no duplicates and any performance hit as a result.
     * @param scopes what to set. 
     */
    public void setSimpleScopes(@Nullable final Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            simpleScopes = Collections.EMPTY_SET;
        } else {
            simpleScopes = scopes.stream().
                    map(StringSupport::trimOrNull).
                    filter(e -> null != e).
                    collect(Collectors.toSet());
        }
    }
    
    /** Sets the regexp scopes.
     * <br> We force the input to be a set so as to enforce no duplicates and any performance hit as a result.
     * @param scopes what to set.
     */
    public void setRegexpScopes(@Nullable final Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            regexpScopes = Collections.EMPTY_LIST;
        } else {
            regexpScopes = scopes.stream().
                    filter(e -> null != e).
                    map(Pattern::compile).
                    map(Pattern::asMatchPredicate).
                    collect(Collectors.toList());   
        }
    }
    
    /** Does the provided string match the scopes for this XMLObject?.
     * <br/> We test against the non regexp scopes for the sake of performance.
     * @param scope what to test.
     * @return whether it matches any of the scopes.
     */
    public boolean matchesScope(@Nonnull @NotEmpty final String scope) {
        final String strippedScope = StringSupport.trimOrNull(scope);
        Constraint.isNotNull(strippedScope, "ScopesContainer#matchesScope() requires nonnul or empty Scope");
        if (simpleScopes.contains(strippedScope)) {
            return true;
        }
        for (final Predicate<String> p: regexpScopes) {
            if (p.test(strippedScope)) {
                return true;
            }
        }
        return false;
    }
}
