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

package net.shibboleth.idp.saml.profile.config.logic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import java.util.function.Predicate;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPresenterEntityContext;
import org.slf4j.Logger;


/**
 * Predicate which evaluates the inbound {@link SAMLPresenterEntityContext#getEntityId()}
 * against a specified collection of entityIDs.
 * 
 * @deprecated
 */
@Deprecated(since="5.0.0", forRemoval=true)
public class AllowedSAMLPresentersPredicate implements Predicate<ProfileRequestContext> {
    
    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(AllowedSAMLPresentersPredicate.class);
    
    /** The set of allowed presenters. */
    @Nonnull private Set<String> allowedPresenters;
    
    /** Constructor. */
    public AllowedSAMLPresentersPredicate() {
        allowedPresenters = CollectionSupport.emptySet();
    }
    
    /**
     * Set the allowed presenters.
     * 
     * @param presenters the allowed presenters
     */
    public void setAllowedPresenters(@Nullable final Collection<String> presenters) {
        if (presenters == null) {
            allowedPresenters = CollectionSupport.emptySet();
        } else {
            allowedPresenters = new HashSet<>(StringSupport.normalizeStringCollection(presenters));
        }
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            log.debug("ProfileRequestContext was null");
            return false;
        }
        
        final MessageContext mc = input.getInboundMessageContext();
        if (mc == null) {
            log.debug("Inbound MessageContext was null");
            return false;
        }
        
        final SAMLPresenterEntityContext presenterContext = mc.getSubcontext(SAMLPresenterEntityContext.class);
        if (presenterContext == null) {
            log.debug("No inbound SAMLPresenterEntityContext");
            return false;
        }
        
        final boolean result = allowedPresenters.contains(presenterContext.getEntityId());
        log.debug("SAML presenter '{}' was {} in set of allowed presenters", 
                presenterContext.getEntityId(), result ? "found" : "NOT found");
        return result;
    }

}