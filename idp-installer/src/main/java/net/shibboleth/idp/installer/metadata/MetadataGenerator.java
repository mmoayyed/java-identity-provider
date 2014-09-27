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

package net.shibboleth.idp.installer.metadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.xmlobject.ExtensionsConstants;
import net.shibboleth.idp.saml.xmlobject.Scope;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.common.Extensions;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.AttributeService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import com.google.common.collect.ImmutableSet;

/**
 * This class gathers information which it then uses to generate IdP Metadata. Loosely based on the SP metadata
 * generator, and the V2 metadata.
 */
// Checkstyle: HideUtilityClassConstructor OFF
public class MetadataGenerator {

    /**
     * The end points we understand.
     */
    enum Endpoints {
        /** IDPSSODescriptor. Artfact */
        SAML1Artifact, SAML2Artifact,
        /** IDPSSODescriptor. SLO */
        RedirectSLO, POSTSLO, SOAPSLO,
        /** IDPSSODescriptor. SSO */
        ShibbolethSSO, POSTSSO, POSTSimpleSignSSO, RedirectSSO,

        /** AttributeAuthorityDescriptor. */
        SAML1Query, SAML2Query,
    }

    /**
     * Those endpoints which require a backchannel.
     */
    static final ImmutableSet<Endpoints> BACKCHANNEL_ENDPOINTS = ImmutableSet.copyOf(EnumSet.of(
            Endpoints.SAML1Artifact, Endpoints.SAML2Artifact, Endpoints.SOAPSLO, Endpoints.SAML1Query,
            Endpoints.SAML2Query));

    /**
     * the Artifact endpoints.
     */
    static final ImmutableSet<Endpoints> ARTIFACT_ENDPOINTS = ImmutableSet.copyOf(EnumSet.of(Endpoints.SAML1Artifact,
            Endpoints.SAML2Artifact));

    /**
     * the SLO endpoints.
     */
    static final ImmutableSet<Endpoints> SLO_ENDPOINTS = ImmutableSet.copyOf(EnumSet.of(Endpoints.ShibbolethSSO,
            Endpoints.POSTSSO, Endpoints.POSTSimpleSignSSO, Endpoints.RedirectSSO));

    /**
     * the SSO endpoints.
     */
    static final ImmutableSet<Endpoints> SSO_ENDPOINTS = ImmutableSet.copyOf(EnumSet.of(Endpoints.RedirectSLO,
            Endpoints.POSTSLO, Endpoints.SOAPSLO));

    /**
     * AttributeAuthority endpoints.
     */
    static final ImmutableSet<Endpoints> AA_ENDPOINTS = ImmutableSet.copyOf(EnumSet.of(Endpoints.SAML1Query,
            Endpoints.SAML2Query));

    /**
     * Which endpoints to generate.
     */
    private EnumSet<Endpoints> endpoints;

    /**
     * The EntityID.
     */
    private String entityID;

    /**
     * The Dns Name.
     */
    private String dnsName;

    /**
     * The Scope.
     */
    private String scope;

    /**
     * The signing certificates.
     */
    private List<List<String>> signingCerts;

    /**
     * The encryption certificates.
     */
    private List<List<String>> encryptionCerts;

    /**
     * Where to write to.
     */
    private final BufferedWriter writer;

    /**
     * Constructor.
     * 
     * @param file file to output to.
     * @throws FileNotFoundException if the file cannot be found.
     */
    public MetadataGenerator(@Nonnull final File file) throws FileNotFoundException {
        final File nonnullFile = Constraint.isNotNull(file, "provided file must be nonnull");
        final FileOutputStream outStream = new FileOutputStream(nonnullFile);
        writer = new BufferedWriter(new OutputStreamWriter(outStream));
        endpoints = EnumSet.allOf(Endpoints.class);
    }

    /**
     * Get the entityID.
     * 
     * @return Returns the entityID.
     */
    public String getEntityID() {
        return entityID;
    }

    /**
     * Set the entityID.
     * 
     * @param id what to set.
     */
    public void setEntityID(String id) {
        entityID = id;
    }

    /**
     * Get the Scope.
     * 
     * @return Returns the Scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Set the Scope.
     * 
     * @param id what to set.
     */
    public void setScope(String id) {
        scope = id;
    }

    /**
     * Get the DNSName.
     * 
     * @return Returns the DNSName.
     */
    public String getDNSName() {
        return dnsName;
    }

    /**
     * Set the DNSName.
     * 
     * @param id what to set.
     */
    public void setDNSName(String id) {
        dnsName = id;
    }

    /**
     * Get the signingCerts.
     * 
     * @return Returns the signingCerts.
     */
    public List<List<String>> getSigningCerts() {
        return signingCerts;
    }

    /**
     * Set the signingCerts.
     * 
     * @param certs what to set.
     */
    public void setSigningCerts(List<List<String>> certs) {
        signingCerts = certs;
    }

    /**
     * Get the encryptionCerts.
     * 
     * @return Returns the signingCerts.
     */
    public List<List<String>> getEncryptionCerts() {
        return encryptionCerts;
    }

    /**
     * Set the encryptionCerts.
     * 
     * @param certs what to set.
     */
    public void setEncryptionCerts(List<List<String>> certs) {
        encryptionCerts = certs;
    }

    /**
     * remove back channel endpoints.
     */
    public void removeBackChannel() {
        endpoints.removeAll(BACKCHANNEL_ENDPOINTS);
    }

    /**
     * Get the Endpoints.
     * 
     * @return Returns the Endpoints
     */
    public EnumSet<Endpoints> getEndpoints() {
        return endpoints;
    }

    /**
     * Set the Endpoints.
     * 
     * @param points what to set.
     */
    public void setEndpoints(@Nonnull EnumSet<Endpoints> points) {
        endpoints = Constraint.isNotNull(points, "supplied endpoints should not be null");
    }

    /**
     * Generate the metadata.
     * 
     * @throws IOException if we have a failure.
     */

    public void generate() throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.newLine();
        writer.write("<");writer.write(EntityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);writer.write(' ');
        writeNameSpace(null, SAMLConstants.SAML20MD_NS);
        writeNameSpace(SignatureConstants.XMLSIG_PREFIX, SignatureConstants.XMLSIG_NS);
        writeNameSpace(ExtensionsConstants.SHIB_MDEXT10_PREFIX, ExtensionsConstants.SHIB_MDEXT10_NS);
        writeNameSpace(XMLConstants.XML_PREFIX, XMLConstants.XML_NS);

        writer.write(" entityID=\"");
        writer.write(getEntityID());
        writer.write("\">");
        writer.newLine();
        writer.newLine();

        writeIDPSSO();
        writer.newLine();
        writer.newLine();
        writeAttributeAuthorityDescriptor();
        writer.newLine();
        writer.write("</EntityDescriptor>");
        writer.newLine();
        writer.flush();
        writer.close();
    }

    /**
     * Writeout a prefix/namespace pair.
     * 
     * @param prefix the prefix, or null
     * @param name the namespace
     * @throws IOException if badness happens
     */
    protected void writeNameSpace(@Nullable String prefix, @Nonnull String name) throws IOException {
        writer.write(" xmlns");
        if (null != prefix) {
            writer.write(':');
            writer.write(prefix);
        }
        writer.write("=\"");
        writer.write(name);
        writer.write("\"");
    }

    /**
     * Write the &lt;IDPSSODescriptor&gt;.
     * 
     * @throws IOException if badness happens
     */
    protected void writeIDPSSO() throws IOException {

        writeRoleDescriptor(IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME,
                Arrays.asList(SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS, "urn:mace:shibboleth:1.0"));
        writer.newLine();
        writeExtensions();
        writer.newLine();
        writeKeyDescriptors();
        for (Endpoints endpoint : ARTIFACT_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        for (Endpoints endpoint : SLO_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        writeNameIdFormat(net.shibboleth.idp.saml.xml.SAMLConstants.SAML1_NAMEID_TRANSIENT);
        writeNameIdFormat(NameIDType.TRANSIENT);
        writer.newLine();

        for (Endpoints endpoint : SSO_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        writer.write("    </");writer.write(IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME);writer.write(">");
        writer.newLine();
    }

    /** write out support for a specific name format.
     * @param format what to support
     * @throws IOException  when badness occurrs
     */
    protected void writeNameIdFormat(String format) throws IOException {
        writer.write("        <");writer.write(NameIDFormat.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.write(format);
        writer.write("</");writer.write(NameIDFormat.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

    }

    /**
     * Write the &lt;AttributeAuthorityDescriptor&gt;.
     * 
     * @throws IOException if badness happens
     */
    private void writeAttributeAuthorityDescriptor() throws IOException {
        writeRoleDescriptor(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME,
                Arrays.asList(SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS));
        writer.newLine();
        writeExtensions();
        writer.newLine();
        writeKeyDescriptors();
        for (Endpoints endpoint : AA_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        writer.write("    </");writer.write(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();
    }

    /**
     * Write out an role descriptor.
     * 
     * @param name the name
     * @param protocols the supported protocols
     * @throws IOException when badness happebns
     */
    protected void writeRoleDescriptor(String name, List<String> protocols) throws IOException {
        writer.write("    <");
        writer.write(name);
        writer.write(" protocolSupportEnumeration=\"");
        boolean first = true;
        for (final String protocol : protocols) {
            if (!first) {
                writer.write(" ");
            }
            writer.write(protocol);
            first = false;
        }
        writer.write("\">)");
        writer.newLine();
    }
    
    /**
     * Write out any &lt;Extensions&ht;Elements. Currently this is just the scope TODO: mdui TODO: entityAttributes
     * 
     * @throws IOException if badness happens
     */
    protected void writeExtensions() throws IOException {
        if (null == getScope() || getScope().isEmpty()) {
            return;
        }
        writer.write("        <");writer.write(Extensions.LOCAL_NAME);writer.write('>');
        writer.newLine();

        writer.write("            <");
        writer.write(ExtensionsConstants.SHIB_MDEXT10_PREFIX);
        writer.write(':');
        writer.write(Scope.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write(" regexp=\"false\">");
        writer.write(getScope());
        writer.write("</");
        writer.write(ExtensionsConstants.SHIB_MDEXT10_PREFIX);
        writer.write(':');
        writer.write(Scope.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

        writer.write("        </");writer.write(Extensions.LOCAL_NAME);writer.write('>');
        writer.newLine();
    }

    /**
     * Write out any &lt;KeyDescriptor&ht;Elements.
     * 
     * @throws IOException if badness happens
     */
    protected void writeKeyDescriptors() throws IOException {
        writeKeyDescriptors(getSigningCerts(), "signing");
        writeKeyDescriptors(getEncryptionCerts(), "encryption");
        writer.newLine();
    }

    /**
     * Write out &lt;KeyDescriptor&ht;Elements. of a specific type
     * 
     * @param certs the certificates
     * @param use the type - signing or encryption
     * @throws IOException if badness happens
     */
    protected void writeKeyDescriptors(@Nullable final List<List<String>> certs, @Nonnull @NotEmpty final String use)
            throws IOException {

        if (null == certs || certs.isEmpty()) {
            return;
        }
        for (List<String> cert : certs) {
            writer.write("        <");writer.write(KeyDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write(" use=\"");
            writer.write(use);
            writer.write("\">");
            writer.newLine();
            writer.write("            <"); 
            writer.write(SignatureConstants.XMLSIG_PREFIX);
            writer.write(':');writer.write(KeyInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("                    <");
            writer.write(SignatureConstants.XMLSIG_PREFIX);
            writer.write(':');writer.write(X509Data.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("                        <");
            writer.write(SignatureConstants.XMLSIG_PREFIX);
            writer.write(':');writer.write(X509Certificate.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            for (String certLine : cert) {
                writer.write(certLine);
                writer.newLine();
            }
            writer.write("                        </");
            writer.write(SignatureConstants.XMLSIG_PREFIX);
            writer.write(':');writer.write(X509Certificate.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("                    </");
            writer.write(SignatureConstants.XMLSIG_PREFIX);
            writer.write(':');writer.write(X509Data.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("            </"); 
            writer.write(SignatureConstants.XMLSIG_PREFIX);
            writer.write(':');writer.write(KeyInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.newLine();
            writer.write("        </");writer.write(KeyDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
        }
    }

    /**
     * Output the SAML for a single endpoint.
     * 
     * @param endpoint the type
     * @throws IOException if badness happens.
     */
    // Checkstyle: MethodLength|CyclomaticComplexity OFF
    protected void outputEndpoint(Endpoints endpoint) throws IOException {
        switch (endpoint) {
            case SAML1Artifact:
                writer.write("        ");
                writer.write("<");writer.write(ArtifactResolutionService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML1_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write(":8443/idp/profile/SAML1/SOAP/ArtifactResolution\"");
                writer.write(" index=\"1\"/>");
                writer.newLine();
                break;

            case SAML2Artifact:
                writer.write("        ");
                writer.write("<");writer.write(ArtifactResolutionService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write(":8443/idp/profile/SAML2/SOAP/ArtifactResolution\"");
                writer.write(" index=\"2\"/>");
                writer.newLine();
                break;

            case RedirectSLO:
                writer.write("        ");
                writer.write("<");writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write("/idp/profile/SAML2/Redirect/SLO\"/>");
                writer.newLine();
                break;

            case POSTSLO:
                writer.write("        ");
                writer.write("<");writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_POST_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write("/idp/profile/SAML2/POST/SLO\"/>");
                writer.newLine();
                break;

            case SOAPSLO:
                writer.write("        ");
                writer.write("<");writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write(":8443/idp/profile/SAML2/SOAP/SLO\"/>");
                writer.newLine();
                break;

            case ShibbolethSSO:
                writer.write("        ");
                writer.write("<");writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"urn:mace:shibboleth:1.0:profiles:AuthnRequest\"");
                writer.write(" Location=\"https://");
                writer.write(getDNSName());
                writer.write("/idp/profile/Shibboleth/SSO\"/>");
                writer.newLine();
                break;

            case POSTSSO:
                writer.write("        ");
                writer.write("<");writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_POST_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write("/idp/profile/SAML2/POST/SSO\"/>");
                writer.newLine();
                break;

            case POSTSimpleSignSSO:
                writer.write("        ");
                writer.write("<");writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write("/idp/profile/SAML2/POST/SSO-SimpleSign\"/>");
                writer.newLine();
                break;

            case RedirectSSO:
                writer.write("        ");
                writer.write("<");writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write("/idp/profile/SAML2/Redirect/SSO\"/>");
                writer.newLine();
                break;

            case SAML1Query:
                writer.write("        ");
                writer.write("<");writer.write(AttributeService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML1_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write(":8443/idp/profile/SAML1/SOAP/AttributeQuery\"/>");
                writer.newLine();
                break;

            case SAML2Query:
                writer.write("        ");
                writer.write("<");writer.write(AttributeService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");writer.write(SAMLConstants.SAML2_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(getDNSName());
                writer.write(":8443/idp/profile/SAML2/SOAP/AttributeQuery\"/>");
                writer.newLine();
                break;

            default:
                break;
        }
    }

}
