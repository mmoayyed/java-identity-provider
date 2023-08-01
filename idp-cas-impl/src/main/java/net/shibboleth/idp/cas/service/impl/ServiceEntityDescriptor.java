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

package net.shibboleth.idp.cas.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.core.xml.AbstractXMLObject;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.AttributeMap;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.EntityGroupName;
import org.opensaml.saml.saml2.metadata.AdditionalMetadataLocation;
import org.opensaml.saml.saml2.metadata.AffiliationDescriptor;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.AuthnAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.PDPDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xmlsec.signature.Signature;

/**
 * Adapts CAS protocol service metadata onto SAML metadata.
 * 
 * <p>Note that this is not a "usable" object in the sense that it raises exceptions on
 * many operations and returns immutable collections that will throw if modified.</p>
 */
public class ServiceEntityDescriptor extends AbstractXMLObject implements EntityDescriptor {

    /** Underlying CAS service. */
    @Nonnull private final Service svc;
    
    /**
     * Creates a new instance that wraps the given CAS service.
     *
     * @param service CAS service metadata object.
     */
    public ServiceEntityDescriptor(@Nonnull final Service service) {
        super(SAMLConstants.SAML20MD_NS, EntityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20MD_PREFIX);
        
        svc = Constraint.isNotNull(service, "Service cannot be null");
        final String group = service.getGroup();
        if (StringSupport.trimOrNull(group) != null) {
            assert group != null;
            getObjectMetadata().put(new EntityGroupName(group));
        }
    }

    /** {@inheritDoc} */
    @Nullable public String getEntityID() {
        return svc.getName();
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * 
     * {@inheritDoc}
     */
    public void setEntityID(@Nullable final String id) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nullable public String getID() {
        return null;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * 
     * {@inheritDoc}
     */
    public void setID(@Nullable final String newID) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nullable public Extensions getExtensions() {
        return null;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * 
     * {@inheritDoc}
     */
    public void setExtensions(@Nullable final Extensions extensions) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nonnull @Live public List<RoleDescriptor> getRoleDescriptors() {
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull @Live public List<RoleDescriptor> getRoleDescriptors(@Nonnull final QName typeOrName) {
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull @Live public List<RoleDescriptor> getRoleDescriptors(@Nonnull final QName typeOrName,
            @Nonnull final String supportedProtocol) {
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    @Nullable public IDPSSODescriptor getIDPSSODescriptor(@Nonnull final String supportedProtocol) {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable public SPSSODescriptor getSPSSODescriptor(@Nonnull final String supportedProtocol) {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable public AuthnAuthorityDescriptor getAuthnAuthorityDescriptor(@Nonnull final String supportedProtocol) {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable public AttributeAuthorityDescriptor getAttributeAuthorityDescriptor(
            @Nonnull final String supportedProtocol) {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable public PDPDescriptor getPDPDescriptor(@Nonnull final String supportedProtocol) {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable public AffiliationDescriptor getAffiliationDescriptor() {
        return null;
    }

    /** {@inheritDoc} */
    public void setAffiliationDescriptor(@Nullable final AffiliationDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nullable public Organization getOrganization() {
        return null;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * 
     * {@inheritDoc}
     */
    public void setOrganization(@Nullable final Organization organization) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nonnull @Live public List<ContactPerson> getContactPersons() {
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull @Live public List<AdditionalMetadataLocation> getAdditionalMetadataLocations() {
        return CollectionSupport.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull public AttributeMap getUnknownAttributes() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nullable public Duration getCacheDuration() {
        return null;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * 
     * {@inheritDoc}
     */
    public void setCacheDuration(@Nullable final Duration duration) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Nullable public String getSignatureReferenceID() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isSigned() {
        return false;
    }

    /** {@inheritDoc} */
    @Nullable public Signature getSignature() {
        return null;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * 
     * {@inheritDoc}
     */
    public void setSignature(@Nullable final Signature newSignature) {
        throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc} */
    public boolean isValid() {
        return true;
    }

    /** {@inheritDoc} */
    @Nullable public Instant getValidUntil() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    /** {@inheritDoc} */
    public void setValidUntil(@Nullable final Instant validUntil) {
        throw new UnsupportedOperationException();        
    }

    /** {@inheritDoc} */
    @Nullable @Unmodifiable @NotLive public List<XMLObject> getOrderedChildren() {
        return null;
    }

}