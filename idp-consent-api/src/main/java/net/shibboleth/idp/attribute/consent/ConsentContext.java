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

package net.shibboleth.idp.attribute.consent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;


/** Context used to collect data about the user's consent to release information. */
public final class ConsentContext implements Subcontext {

    /** Indicates the result of the consent engine's attempt to gain user consent. */
    public static enum Consent {
        /** Indicates that no claim is made about the user's consent to the release of information. */
        UNSPECIFIED,

        /**
         * Indicates that it was not possible to determine the user's consent for the given request. For example, the
         * only approach for asking the user, supported by the engine implementation, is to display a web page but the
         * protocol in use does not allow that.
         */
        INAPPLICABLE,

        /** Consent engine prompted the user for consent and the user gave it. */
        OBTAINED,

        /**
         * User gave direction in the past that the release of information, in this case, was acceptable. For example,
         * by means of a "remember my decision" box on a web page.
         */
        PRIOR,

        /** User denied the release of information. */
        DENIED
    }

    /** Owner of this consent context. */
    private SubcontextContainer owner;

    /** Attributes about the user that are to be released with the user's consent. */
    private Map<String, Attribute<?>> userAttributes;

    /** The decision determined by the consent engine. */
    private Consent consentDecision;

    /** The attributes the consent engine has determined may be released. */
    private Map<String, Attribute<?>> consentedAttributes;

    /**
     * Constructor.
     * 
     * @param superContext owner of this context
     * @param attributes attributes about the user that are to be released with the user's consent, defensively copied
     */
    public ConsentContext(SubcontextContainer superContext, Map<String, Attribute<?>> attributes) {
        owner = Assert.isNotNull(superContext, "Owning super context may not be null");

        userAttributes = Collections.unmodifiableMap(new HashMap<String, Attribute<?>>(attributes));
        consentedAttributes = new HashMap<String, Attribute<?>>();
        
        consentDecision = Consent.UNSPECIFIED;
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return owner;
    }

    /**
     * Gets the consent decision established by the user.
     * 
     * @return consent decision established by the user
     */
    public Consent getConsentDecision() {
        return consentDecision;
    }

    /**
     * Sets the consent decision established by the user.
     * 
     * @param decision consent decision established by the user
     */
    public void setConsentDecision(Consent decision) {
        consentDecision = decision;
    }

    /**
     * Gets an unmodifiable map representing the information to be released about the user.
     * 
     * @return unmodifiable map representing the information to be released about the user
     */
    public Map<String, Attribute<?>> getUserAttributes() {
        return userAttributes;
    }

    /**
     * Gets the attributes that may be released with the user's consent.
     * 
     * @return attributes that may be released with the user's consent, never null
     */
    public Map<String, Attribute<?>> getConsentedAttributes() {
        return consentedAttributes;
    }
}