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

package net.shibboleth.idp.authn.context;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * A {@link BaseContext} that holds information about an authentication request's
 * requirement for a specific custom {@link Principal}.
 * 
 * <p>Authentication protocols with features for requesting specific forms of
 * authentication will populate this context type, typically as a child of the
 * {@link AuthenticationContext}, with an expression of those requirements in the
 * form of a protocol-specific operator string and an ordered list of custom
 * {@link Principal} objects.</p>
 * 
 * <p>During the authentication process, interactions with {@link PrincipalSupportingComponent}-
 * supporting objects will depend on them satisfying context requirements, via the use of
 * registered {@link PrincipalEvalPredicateFactory} objects.</p>
 */
public class RequestedPrincipalContext extends BaseContext {

    /** Comparison operator specific to request protocol. */
    @Nonnull @NotEmpty private final String operatorString;

    /** The principals reflecting the request requirements. */
    @Nonnull @NotEmpty @NonnullElements private final List<Principal> requestedPrincipals;
    
    /**
     * Constructor.
     * 
     * @param operator comparison operator specific to request protocol
     * @param principals ordered list of principals reflecting the request requirements
     */
    public RequestedPrincipalContext(@Nonnull @NotEmpty final String operator,
            @Nonnull @NotEmpty @NonnullElements final List<Principal> principals) {
        super();
        Constraint.isNotEmpty(principals, "Principal list cannot be null");
        
        operatorString = Constraint.isNotNull(StringSupport.trimOrNull(operator), "Operator cannot be null or empty");
        requestedPrincipals = new ArrayList(Collections2.filter(principals, Predicates.notNull()));
    }

    /**
     * Get the canonical principal name of the subject.
     * 
     * @return the canonical principal name
     */
    @Nonnull @NotEmpty public String getOperator() {
        return operatorString;
    }

    /**
     * Get an immutable list of principals reflecting the request requirements.
     * 
     * @return  immutable list of principals 
     */
    @Nonnull @NotEmpty @NonnullElements @Unmodifiable public List<Principal> getRequestedPrincipals() {
        return ImmutableList.copyOf(requestedPrincipals);
    }
    
}