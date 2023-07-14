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

package net.shibboleth.idp.installer.impl;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.impl.LogoBuilder;
import org.opensaml.saml.metadata.generator.impl.ArtifactResolutionServiceConverter;
import org.opensaml.saml.metadata.generator.impl.SingleLogoutServiceConverter;
import org.opensaml.saml.metadata.generator.impl.SingleSignOnServiceConverter;
import org.opensaml.saml.metadata.generator.impl.TemplateMetadataGeneratorParameters;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.IndexedEndpoint;
import org.opensaml.security.x509.X509Support;
import org.slf4j.Logger;

import net.shibboleth.idp.saml.xmlobject.ExtensionsConstants;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.EncodingException;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resource.Resource;

/**
 * Parameters to metadata generation
 */
public class InstalledMetadataParameters extends AbstractInitializableComponent implements TemplateMetadataGeneratorParameters {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(InstalledMetadataParameters.class);

    /**
     * The file with the certificate the IDP uses to encrypt.
     */
    @Nullable private File encryptionCert;

    /**
     * The file with the certificate that TLS uses to 'sign'.
     */
    @Nullable private File backChannelCert;

    /**
     * The file with the certificate the IDP uses to sign.
     */
    @Nullable private File signingCert;

    /** The entityID. */
    @NonnullAfterInit private String entityID;

    /** The DNS name. */
    @NonnullAfterInit private String dnsName;

    /** The scope. */
    @Nullable private String scope;

    /*
     * Static settings.
     */
    /** logout services. */
    final @Nonnull List<Pair<String, String>> logoutServices = CollectionSupport.singletonList(
            new Pair<>("SOAP/","/idp/profile/SAML2/SOAP/ArtifactResolution"));

    /** sso services. */
    final @Nonnull List<Pair<String, String>> ssoServices = CollectionSupport.listOf(
            new Pair<>("SimpleSign/","/idp/profile/SAML2/POST-SimpleSign/SSO"),
            new Pair<>("Redirect/","/idp/profile/SAML2/Redirect/SSO"),
            new Pair<>("POST/","idp/profile/SAML2/POST/SSO"));

    /** artifact services. */
    final @Nonnull List<Pair<String, String>> artifactServices = CollectionSupport.emptyList();

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (entityID == null || entityID.isEmpty()) {
            throw new ComponentInitializationException("Entity ID not specified");
        }
        if (dnsName == null || dnsName.isEmpty()) {
            throw new ComponentInitializationException("DNS name not specified");
        }
    }

    /**
     * Set the encryption Certificate file.
     *
     * @param resource what to set.
     */
    public void setEncryptionCertResource(@Nonnull final Resource resource) {

        try {
            encryptionCert = resource.getFile();
        } catch (final IOException e) {
            log.error("Could not open encryption  cert", e);
            encryptionCert = null;
        }
    }

    /**
     * Set the signing Certificate file.
     *
     * @param resource what to set.
     */
    public void setSigningCertResource(@Nonnull final Resource resource) {
        try {
            signingCert = resource.getFile();
        } catch (final IOException e) {
            log.error("Could not open signing cert", e);
            signingCert = null;
        }
    }

    /**
     * Set the Backchannel Certificate.
     *
     * @param resource what to set.
     */
    public void setBackchannelCertResource(@Nonnull final Resource resource) {
        try {
            backChannelCert = resource.getFile();
        } catch (final IOException e) {
            log.error("Could not open back channel cert", e);
            backChannelCert = null;
        }
    }

    /**
     * Load a certificate from a file in a standard format and produce a base64-encoded DER string.
     *
     * @param file certificate file
     *
     * @return encoded string
     */
    @Nullable private String getEncodedCertificate(@Nullable final File file) {
        if (file == null) {
            return null;
        }
        try {
            final byte[] cert = X509Support.decodeCertificate(file).getEncoded();
            assert cert != null;
            return Base64Support.encode(cert, true);
        } catch (final CertificateException | EncodingException e) {
            log.warn("Unable to decode and re-encode certificate at path {}", file, e);
            return null;
        }
    }

    /**
     * Sets the entityID.
     *
     * @param id what to set.
     */
    public void setEntityID(@Nonnull final String id) {
        entityID = id;
    }

    /**
     * Sets the dns name.
     *
     * @param name what to set.
     */
    public void setDnsName(@Nonnull final String name) {
        dnsName = name;
    }

    /**
     * Sets the scope.
     *
     * @param value what to set.
     */
    public void setScope(@Nullable final String value) {
        scope = value;
    }

    // Methods for the interface

    /** {@inheritDoc}     */
    @Nullable
    public String getEntityID() {
        return entityID;
    }

    /** {@inheritDoc} */
    @Nullable public Set<Namespace> getAdditionalNamespaces() {
        final Set<Namespace> namespaces = new HashSet<>();
        namespaces.add(new Namespace(SAMLConstants.SAML20MDUI_NS, SAMLConstants.SAML20MDUI_PREFIX));
        if (scope != null) {
            namespaces.add(new Namespace(ExtensionsConstants.SHIB_MDEXT10_NS, ExtensionsConstants.SHIB_MDEXT10_PREFIX));
        }
        return namespaces;
    }

    /**
     * Convert the expressions into endpoints.
     *
     * @param <T> endpoint type
     * @param converter endpoint converter
     * @param protocols accumulator for protocol support values
     * @param input raw argument list
     *
     * @return converted endpoints
     */
    @Nonnull private <T extends Endpoint> Collection<T> convertEndpoints(
            @Nonnull final BiFunction<String,List<String>,T> converter,
            @Nonnull @Live final List<String> protocols,
            @Nonnull final Collection<Pair<String,String>> input) {
        return input
                .stream()
                .map(p -> converter.apply(new StringBuffer(p.getFirst()).append(dnsName).append(p.getSecond()).toString(),
                                          protocols))
                .collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableList())).get();
    }

    /** {@inheritDoc}     */
    @Nullable
    public IDPSSODescriptor getIDPSSODescriptor() {
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<IDPSSODescriptor> idpSSOBuilder =
                (SAMLObjectBuilder<IDPSSODescriptor>) bf.<IDPSSODescriptor>ensureBuilder(
                        IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        final IDPSSODescriptor role = idpSSOBuilder.buildObject();

        final List<String> protocols = new ArrayList<>();

        role.getSingleLogoutServices().addAll(
                convertEndpoints(new SingleLogoutServiceConverter(), protocols, logoutServices));
        role.getSingleSignOnServices().addAll(
                convertEndpoints(new SingleSignOnServiceConverter(), protocols, ssoServices));
        role.getArtifactResolutionServices().addAll(
                convertEndpoints(new ArtifactResolutionServiceConverter(), protocols, artifactServices));

        int index = 1;
        for (final IndexedEndpoint e : role.getArtifactResolutionServices()) {
            e.setIndex(index++);
        }
          protocols.forEach(role::addSupportedProtocol);
        return role;
    }

    /** {@inheritDoc}     */
    @Nonnull @Unmodifiable @NotLive
    public List<String> getSigningCertificates() {
        final List<String> result = new ArrayList<>(2);
        String cert = getEncodedCertificate(backChannelCert);
        if (cert != null) {
            result.add(cert);
        }
        cert = getEncodedCertificate(signingCert);
        if (cert != null) {
            result.add(cert);
        }
        return result;
    }

    /** {@inheritDoc}     */
    @Nonnull @Unmodifiable @NotLive
    public List<String> getEncryptionCertificates() {
        final String cert = getEncodedCertificate(encryptionCert);
        if (cert == null) {
            return CollectionSupport.emptyList();
        }
        return CollectionSupport.singletonList(cert);
    }

    /** {@inheritDoc}     */
    @Nullable
    public String getLang() {
        return "en";
    }

    /** {@inheritDoc}     */
    @Nullable
    public String getDisplayName() {
        return new StringBuffer("A name for the IdP at ").append(dnsName).toString();
    }

    /** {@inheritDoc}     */
    @Nullable
    public String getDescription() {
        return new StringBuffer("Enter a description for the IdP at ").append(dnsName).toString();
    }

    /** {@inheritDoc}     */
    @Nullable
    public Logo getLogo() {
        final Logo result = new LogoBuilder().buildObject();
        result.setHeight(80);
        result.setWidth(80);
        result.setURI(new StringBuffer("https://").append(dnsName).append("/path/to/logo.png").toString());
        return result;
    }

    /** {@inheritDoc} */
    @Nonnull @Unmodifiable @NotLive public List<String> getScopes() {
        final String s = scope;
        if (s == null) {
            return CollectionSupport.emptyList();
        }
        return CollectionSupport.singletonList(s);
    }
}
