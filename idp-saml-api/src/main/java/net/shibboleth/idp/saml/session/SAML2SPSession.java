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

package net.shibboleth.idp.saml.session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.NameID;
import org.w3c.dom.Element;

import com.google.common.base.Objects;

import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * Extends an {@link SPSession} with SAML 2.0 information required for
 * reverse lookup in the case of a logout. 
 */
public class SAML2SPSession extends BasicSPSession {

    /** The NameID asserted to the SP. */
    @Nonnull private final NameID nameID;
    
    /** The SessionIndex asserted to the SP. */
    @Nonnull @NotEmpty private final String sessionIndex;
    
    /**
     * Constructor.
     *
     * @param id the identifier of the service associated with this session
     * @param flowId authentication flow used to authenticate the principal to this service
     * @param creation creation time of session, in milliseconds since the epoch
     * @param expiration expiration time of session, in milliseconds since the epoch
     * @param assertedNameID the NameID asserted to the SP
     * @param assertedIndex the SessionIndex asserted to the SP
     */
    // Checkstyle: ParameterNumber OFF
    public SAML2SPSession(@Nonnull @NotEmpty final String id, @Nonnull @NotEmpty final String flowId,
            @Positive final long creation, @Positive final long expiration, @Nonnull final NameID assertedNameID,
            @Nonnull @NotEmpty final String assertedIndex) {
        super(id, flowId, creation, expiration);
        
        nameID = Constraint.isNotNull(assertedNameID, "NameID cannot be null");
        sessionIndex = Constraint.isNotNull(StringSupport.trimOrNull(assertedIndex),
                "SessionIndex cannot be null or empty");
    }
    // Checkstyle: ParameterNumber ON
   
    /**
     * Get the {@link NameID} asserted to the SP.
     * 
     * @return the asserted {@link NameID}
     */
    @Nonnull public NameID getNameID() {
        return nameID;
    }

    /**
     * Get the {@link org.opensaml.saml.saml2.core.SessionIndex} value asserted to the SP.
     * 
     * @return the SessionIndex value
     */
    @Nonnull @NotEmpty public String getSessionIndex() {
        return sessionIndex;
    }

    /** {@inheritDoc} */
    @Nullable public String getSPSessionKey() {
        return nameID.getValue();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return (getId() + '!' + nameID.getValue() + '!' + sessionIndex).hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof SAML2SPSession) {
            
            // TODO: need to implement a strong matching algorithm in a support class
            
            if (Objects.equal(getSessionIndex(), ((SAML2SPSession) obj).getSessionIndex())) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        Marshaller marshaller = Constraint.isNotNull(
                XMLObjectSupport.getMarshaller(nameID), "Marshaller for NameID was null");
        try {
            Element node = marshaller.marshall(nameID);
            return Objects.toStringHelper(this).add("NameID", SerializeSupport.nodeToString(node))
                    .add("SessionIndex", sessionIndex).toString();
        } catch (MarshallingException e) {
            throw new IllegalArgumentException("Error marshalling NameID", e);
        }
    }

}