/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.time.Instant;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.profile.SAML2ObjectSupport;

import com.google.common.base.MoreObjects;

import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.xml.SerializeSupport;

/**
 * Extends a {@link BasicSPSession} with SAML 2.0 information required for
 * reverse lookup in the case of a logout. 
 */
public class SAML2SPSession extends BasicSPSession {

    /** The NameID asserted to the SP. */
    @Nonnull private final NameID nameID;
    
    /** The SessionIndex asserted to the SP. */
    @Nonnull @NotEmpty private final String sessionIndex;
    
    /** The ACS location used for the associated response. */
    @Nullable @NotEmpty private final String acsLocation;
    
    /** Whether logout propagation is possible. */
    private final boolean supportsLogoutPropagation;
    
// Checkstyle: ParameterNumber OFF
    /**
     * Constructor.
     *
     * @param id the identifier of the service associated with this session
     * @param creation creation time of session
     * @param expiration expiration time of session
     * @param assertedNameID the NameID asserted to the SP
     * @param assertedIndex the SessionIndex asserted to the SP
     * @param acsLoc the response endpoint used
     * @param supportsLogoutProp whether the session supports logout propagation
     */
    public SAML2SPSession(@Nonnull @NotEmpty final String id, @Nonnull final Instant creation,
            @Nonnull final Instant expiration, @Nonnull final NameID assertedNameID,
            @Nonnull @NotEmpty final String assertedIndex, @Nullable @NotEmpty final String acsLoc,
            final boolean supportsLogoutProp) {
        super(id, creation, expiration);
        
        nameID = Constraint.isNotNull(assertedNameID, "NameID cannot be null");
        sessionIndex = Constraint.isNotNull(StringSupport.trimOrNull(assertedIndex),
                "SessionIndex cannot be null or empty");
        acsLocation = StringSupport.trimOrNull(acsLoc);
        supportsLogoutPropagation = supportsLogoutProp;
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
    @Override
    @Nullable public String getSPSessionKey() {
        return nameID.getValue();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable @NotEmpty public String getProtocol() {
        return SAMLConstants.SAML20P_NS;
    }
    
    /**
     * Get the ACS location used for the response that produced this session.
     * 
     * @return ACS location
     */
    @Nullable @NotEmpty public String getACSLocation() {
        return acsLocation;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsLogoutPropagation() {
        return supportsLogoutPropagation;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (getId() + '!' + nameID.getValue() + '!' + sessionIndex).hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof SAML2SPSession) {
            final NameID n1 = ((SAML2SPSession) obj).getNameID();
            final NameID n2 = nameID;
            if (n1 != null && n2 != null && Objects.equals(n1.getValue(), n2.getValue())
                    && SAML2ObjectSupport.areNameIDFormatsEquivalent(n1.getFormat(), n2.getFormat())
                    && Objects.equals(n1.getNameQualifier(), n2.getNameQualifier())
                    && Objects.equals(n1.getSPNameQualifier(), n2.getSPNameQualifier())) {
                return Objects.equals(getSessionIndex(), ((SAML2SPSession) obj).getSessionIndex());
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        try {
            return MoreObjects.toStringHelper(this)
                    .add("NameID", SerializeSupport.nodeToString(XMLObjectSupport.marshall(nameID)))
                    .add("SessionIndex", sessionIndex).toString();
        } catch (final MarshallingException e) {
            throw new IllegalArgumentException("Error marshalling NameID", e);
        }
    }

}