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

package net.shibboleth.idp.installer.metadata.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.opensaml.core.xml.LangBearing;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.reqattr.RequestedAttributes;
import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.AttributeService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import net.shibboleth.idp.saml.xmlobject.ExtensionsConstants;
import net.shibboleth.idp.saml.xmlobject.Scope;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.xml.DOMTypeSupport;
import net.shibboleth.shared.xml.XMLConstants;

/**
 * This class gathers information which it then uses to generate IdP Metadata. Loosely based on the SP metadata
 * generator, and the V2 metadata.
 */
public class MetadataGeneratorImpl extends AbstractInitializableComponent {

    /**
     * The end points we understand.
     */
    enum Endpoints {
        /** IDPSSODescriptor. Artifact (SAML1). */
        SAML1Artifact,
        /** IDPSSODescriptor. Artifact (SAML2). */
        SAML2Artifact,
        /** IDPSSODescriptor. SLO (Redirect) */
        RedirectSLO,
        /** IDPSSODescriptor. SLO (Post) */
        POSTSLO,
        /** IDPSSODescriptor. SLO (Post Simple Sign) */
        POSTSimpleSignSLO,
        /** IDPSSODescriptor. SLO (Soap) */
        SOAPSLO,
        /** IDPSSODescriptor. SSO (Shibboleth protocol)*/
        ShibbolethSSO,
        /** IDPSSODescriptor. SSO (SAML2 Post)*/
        POSTSSO,
        /** IDPSSODescriptor. SSO (SAML2 Post Simple Sign)*/
        POSTSimpleSignSSO,
        /** IDPSSODescriptor. SSO (SAML2 Redirectr)*/
        RedirectSSO,
        /** AttributeAuthorityDescriptor. (SAML1=)*/
        SAML1Query,
        /** AttributeAuthorityDescriptor. (SAML2) */
        SAML2Query,
    }

    /**
     * Those endpoints which require a backchannel.
     */
    static final Set<Endpoints> BACKCHANNEL_ENDPOINTS = Set.copyOf(EnumSet.of(
            Endpoints.SAML1Artifact, Endpoints.SAML2Artifact, Endpoints.SOAPSLO, Endpoints.SAML1Query,
            Endpoints.SAML2Query));

    /**
     * the Artifact endpoints.
     */
    static final Set<Endpoints> ARTIFACT_ENDPOINTS = Set.copyOf(EnumSet.of(Endpoints.SAML1Artifact,
            Endpoints.SAML2Artifact));

    /**
     * the SSO endpoints.
     */
    static final Set<Endpoints> SSO_ENDPOINTS = Set.copyOf(EnumSet.of(Endpoints.ShibbolethSSO,
            Endpoints.POSTSSO, Endpoints.POSTSimpleSignSSO, Endpoints.RedirectSSO));

    /**
     * the SLO endpoints.
     */
    static final Set<Endpoints> SLO_ENDPOINTS = Set.copyOf(EnumSet.of(Endpoints.RedirectSLO,
            Endpoints.POSTSLO, Endpoints.POSTSimpleSignSLO, Endpoints.SOAPSLO));

    /**
     * AttributeAuthority endpoints.
     */
    static final Set<Endpoints> AA_ENDPOINTS = Set.copyOf(EnumSet.of(Endpoints.SAML1Query,
            Endpoints.SAML2Query));

    /**
     * Which endpoints to generate.
     */
    private EnumSet<Endpoints> endpoints;

    /**
     * Whether to comment out the SAML2 AA endpoint.
     */
    private boolean saml2AttributeQueryCommented = true;

    /**
     * Whether to comment out the SAML2 SLO endpoints.
     */
    private boolean saml2LogoutCommented = true;

    /** Whether SAML1 is commented out. */
    private boolean saml1Commented = true;

    /** Comment depth. */
    private int commentDepth;

    /**
     * Where to write to - as {@link BufferedWriter}.
     */
    @NonnullAfterInit private BufferedWriter writer;

    /**
     * Where to write to - as {@link File}.
     */
    private File output;

    /** The parameters. */
    private MetadataGeneratorParametersImpl params;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        try {
            final FileOutputStream outStream;
            outStream = new FileOutputStream(output);
            writer = new BufferedWriter(new OutputStreamWriter(outStream));
        } catch (final FileNotFoundException e) {
            throw new ComponentInitializationException(e);
        }
        endpoints = EnumSet.allOf(Endpoints.class);
    }

    /** Set where to write the metadata.
     * @param file what to set.
     */
    public void setOutput(@Nonnull final File file) {
        checkSetterPreconditions();
        output = Constraint.isNotNull(file, "provided file must be nonnull");
    }

    /** Set a description of the IdP.
     * @param what what to set.  This component does not have to be initialized.
     */
    public void setParameters(@Nonnull final MetadataGeneratorParametersImpl what) {
        checkSetterPreconditions();
        params = Constraint.isNotNull(what, "provided params must be nonnull");
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
    public void setEndpoints(@Nonnull final EnumSet<Endpoints> points) {
        endpoints = Constraint.isNotNull(points, "supplied endpoints should not be null");
    }

    /**
     * Returns whether to comment the SAML2 AA endpoint.
     *
     * @return whether to comment the SAML2 AA endpoint
     */
    public boolean isSAML2AttributeQueryCommented() {
        return saml2AttributeQueryCommented;
    }

    /**
     * Sets whether to comment the SAML2 AA endpoint.
     *
     * @param asComment whether to comment or not.
     */
    public void setSAML2AttributeQueryCommented(final boolean asComment) {
        saml2AttributeQueryCommented = asComment;
    }

    /**
     * Returns whether to comment SAML1 endpoints.
     *
     * @return whether to comment SAML1 endpoints
     */
    public boolean isSAML1Commented() {
        return saml1Commented;
    }

    /**
     * Sets whether to comment the comment SAML1 endpoints.
     *
     * @param asComment whether to comment or not.
     */
    public void setSAML1Commented(final boolean asComment) {
        saml1Commented= asComment;
    }


    /**
     * Returns whether to comment the SAML2 Logout endpoints.
     *
     * @return whether to comment the SAML2 Logout endpoints
     */
    public boolean isSAML2LogoutCommented() {
        return saml2LogoutCommented;
    }

    /**
     * Sets whether to comment the SAML2 Logout endpoints.
     *
     * @param asComment whether to comment or not
     */
    public void setSAML2LogoutCommented(final boolean asComment) {
        saml2LogoutCommented = asComment;
    }

    /** Generate the metadata given the parameters.
     * @throws BuildException if badness occurs.
     */
    public void generate() throws BuildException {
        checkComponentActive();
        try {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writeComments();
            writer.write("<");
            writer.write(EntityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write(' ');
            writeNameSpace(null, SAMLConstants.SAML20MD_NS);
            writeNameSpace(SignatureConstants.XMLSIG_PREFIX, SignatureConstants.XMLSIG_NS);
            writeNameSpace(ExtensionsConstants.SHIB_MDEXT10_PREFIX, ExtensionsConstants.SHIB_MDEXT10_NS);
            writeNameSpace(XMLConstants.XML_PREFIX, XMLConstants.XML_NS);
            writeNameSpace(SAMLConstants.SAML20MDUI_PREFIX, SAMLConstants.SAML20MDUI_NS);
            writeNameSpace(SAMLConstants.SAML20PREQ_ATTRR_PREFIX, SAMLConstants.SAML20PREQ_ATTR_NS);

            writer.write(" validUntil=\"" + DOMTypeSupport.instantToString(Instant.now()) + "\"");

            writer.write(" entityID=\"");
            writer.write(params.getEntityID());
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
        } catch (final IOException e) {
            try {
                writer.close();
            } catch (final IOException e1) {
                // Ignore
            }
            throw new BuildException(e);
        }
    }

    /**
     * Add appropriate comments to metadata header.
     *
     * @throws IOException if badness occurs in the writer
     */
    protected void writeComments() throws IOException {
        openComment();
        writer.newLine();
        writer.write("     This is example metadata only. Do *NOT* supply it as is without review,");
        writer.newLine();
        writer.write("     and do *NOT* provide it in real time to your partners.");
        writer.newLine();
        writer.newLine();
        writer.write("     This metadata is not dynamic - it will not change as your configuration changes.");
        writer.write("     On Demand Metadata Generation available from the metadatagen plugin.");
        writer.newLine();
        closeComment();
        writer.newLine();
    }

    /**
     * Writeout a prefix/namespace pair.
     *
     * @param prefix the prefix, or null
     * @param name the namespace
     * @throws IOException if badness happens
     */
    protected void writeNameSpace(@Nullable final String prefix, @Nonnull final String name) throws IOException {
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
        final List<String> protocols;
        if (isSAML1Commented()) {
            protocols = CollectionSupport.singletonList(SAMLConstants.SAML20P_NS);
        } else {
            protocols = CollectionSupport.listOf(SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS, "urn:mace:shibboleth:1.0");
        }

        writeRoleDescriptor(IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME, protocols); 
        writer.newLine();
        openExtensions();
        writeScope();
        writeMDUI();
        closeExtensions();
        writer.newLine();
        writeKeyDescriptors();
        for (final Endpoints endpoint : ARTIFACT_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        if (isSAML2LogoutCommented()) {
            openComment();
            writer.newLine();
        }
        for (final Endpoints endpoint : SLO_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        if (isSAML2LogoutCommented()) {
            closeComment();
            writer.newLine();
        }

        writer.newLine();
        for (final Endpoints endpoint : SSO_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        writer.write("    </");
        writer.write(IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write(">");
        writer.newLine();
    }

    /**
     * Write the &lt;AttributeAuthorityDescriptor&gt;.
     *
     * @throws IOException if badness happens
     */
    private void writeAttributeAuthorityDescriptor() throws IOException {
        if (isSAML2AttributeQueryCommented() && isSAML1Commented()) {
            openComment();
        }
        final List<String> protocols;
        if (isSAML2AttributeQueryCommented()) {
            protocols = CollectionSupport.singletonList(SAMLConstants.SAML11P_NS);
        } else {
            protocols = CollectionSupport.listOf(SAMLConstants.SAML20P_NS, SAMLConstants.SAML11P_NS);
        }
        writeRoleDescriptor(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME, protocols);
        writer.newLine();
        openExtensions();
        writeScope();
        closeExtensions();
        writer.newLine();
        writeKeyDescriptors();
        for (final Endpoints endpoint : AA_ENDPOINTS) {
            if (getEndpoints().contains(endpoint)) {
                outputEndpoint(endpoint);
            }
        }
        writer.newLine();
        writer.write("    </");
        writer.write(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        if (isSAML2AttributeQueryCommented() && isSAML1Commented()) {
            closeComment();
        }
        writer.newLine();
    }

    /**
     * Write out an role descriptor.
     *
     * @param name the name
     * @param protocols the supported protocols
     * @throws IOException when badness happebns
     */
    protected void writeRoleDescriptor(final String name, final List<String> protocols) throws IOException {
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
        writer.write("\">");
        writer.newLine();
    }

    /**
     * Write the open &lt;Extensions&gt; elements.
     *
     * @throws IOException if badness happens
     */
    protected void openExtensions() throws IOException {

        writer.write("        <");
        writer.write(Extensions.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();
    }

    /**
     * Write out the close &lt;\Extensions&gt; Element.
     *
     * @throws IOException if badness happens
     */
    protected void closeExtensions() throws IOException {

        writer.write("        </");
        writer.write(Extensions.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();
    }

    /**
     * Write out the &lt;shibmd:Scope&gt; element.
     *
     * @throws IOException if badness happens
     */
    protected void writeScope() throws IOException {
        if (null == params.getScope() || params.getScope().isEmpty()) {
            return;
        }

        writer.write("            <");
        writeNameSpaceQualified(ExtensionsConstants.SHIB_MDEXT10_PREFIX, Scope.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write(" regexp=\"false\">");
        writer.write(params.getScope());
        writer.write("</");
        writeNameSpaceQualified(ExtensionsConstants.SHIB_MDEXT10_PREFIX, Scope.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();
    }

    /**
     * Write out the &lt;mdui:UIINFO&gt; element and children.
     *
     * @throws IOException if badness happens
     */
    protected void writeMDUI() throws IOException {
        openComment();
        writer.newLine();
        writer.write("    Fill in the details for your IdP here ");
        writer.newLine();
        writer.newLine();

        writer.write("            <");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, UIInfo.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

        // DisplayName
        writer.write("                <");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, DisplayName.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write(' ');
        writeLangAttribute("en");
        writer.write('>');
        writer.write("A Name for the IdP at ");
        writer.write(params.getDnsName());
        writer.write("</");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, DisplayName.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

        // Description
        writer.write("                <");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, Description.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write(' ');
        writeLangAttribute("en");
        writer.write('>');
        writer.write("Enter a description of your IdP at ");
        writer.write(params.getDnsName());
        writer.write("</");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, Description.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

        // Logo
        writer.write("                <");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, Logo.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write(" height=\"80\" width=\"80\">");
        writer.write("https://");
        writer.write(params.getDnsName());
        writer.write("/Path/To/Logo.png");
        writer.write("</");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, Logo.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

        writer.write("            </");
        writeNameSpaceQualified(SAMLConstants.SAML20MDUI_PREFIX, UIInfo.DEFAULT_ELEMENT_LOCAL_NAME);
        writer.write('>');
        writer.newLine();

        closeComment();
        writer.newLine();
    }

    /**
     * Write the language attribute.
     *
     * @param language which languages
     * @throws IOException if badness happens
     */
    protected void writeLangAttribute(final String language) throws IOException {
        writeNameSpaceQualified(XMLConstants.XML_PREFIX, LangBearing.XML_LANG_ATTR_LOCAL_NAME);
        writer.write("=\"");
        writer.write(language);
        writer.write('"');
    }

    /**
     * Write out any &lt;KeyDescriptor&gt;Elements.
     *
     * @throws IOException if badness happens
     */
    protected void writeKeyDescriptors() throws IOException {
        final List<List<String>> signing = new ArrayList<>(2);
        final List<String> backchannelCert = params.getBackchannelCert();
        if (backchannelCert != null && !backchannelCert.isEmpty()) {
            writer.write("        ");
            openComment();
            writer.write(" First signing certificate is BackChannel, the Second is FrontChannel");
            closeComment();
            writer.newLine();
            signing.add(backchannelCert);
        }
        final List<String> signingCert = params.getSigningCert();
        if (signingCert!= null && !signingCert.isEmpty()) {
            signing.add(signingCert);
        }
        writeKeyDescriptors(signing, "signing");
        
        final List<String> encryption = params.getEncryptionCert();
        if (encryption != null) {
            writeKeyDescriptors(CollectionSupport.singletonList(encryption), "encryption");
        }
        writer.newLine();
    }

    /**
     * Write out &lt;KeyDescriptor&gt;Elements. of a specific type
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
        for (final List<String> cert : certs) {
            writer.write("        <");
            writer.write(KeyDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write(" use=\"");
            writer.write(use);
            writer.write("\">");
            writer.newLine();
            writer.write("            <");
            writeNameSpaceQualified(SignatureConstants.XMLSIG_PREFIX, KeyInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("                    <");
            writeNameSpaceQualified(SignatureConstants.XMLSIG_PREFIX, X509Data.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("                        <");
            writeNameSpaceQualified(SignatureConstants.XMLSIG_PREFIX, X509Certificate.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            for (final String certLine : cert) {
                writer.write(certLine);
                writer.newLine();
            }
            writer.write("                        </");
            writeNameSpaceQualified(SignatureConstants.XMLSIG_PREFIX, X509Certificate.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("                    </");
            writeNameSpaceQualified(SignatureConstants.XMLSIG_PREFIX, X509Data.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.write("            </");
            writeNameSpaceQualified(SignatureConstants.XMLSIG_PREFIX, KeyInfo.DEFAULT_ELEMENT_LOCAL_NAME);
            writer.write('>');
            writer.newLine();
            writer.newLine();
            writer.write("        </");
            writer.write(KeyDescriptor.DEFAULT_ELEMENT_LOCAL_NAME);
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
    protected void outputEndpoint(final Endpoints endpoint) throws IOException {
        switch (endpoint) {
            case SAML1Artifact:
                writer.write("        ");
                if (isSAML1Commented()) {
                    openComment();
                }
                writer.write("<");
                writer.write(ArtifactResolutionService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML1_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write(":8443/idp/profile/SAML1/SOAP/ArtifactResolution\"");
                writer.write(" index=\"1\"/>");
                writer.newLine();
                if (isSAML1Commented()) {
                    closeComment();
                }
                break;

            case SAML2Artifact:
                writer.write("        ");
                writer.write("<");
                writer.write(ArtifactResolutionService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write(":8443/idp/profile/SAML2/SOAP/ArtifactResolution\"");
                writer.write(" index=\"2\"/>");
                writer.newLine();
                break;

            case RedirectSLO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/SAML2/Redirect/SLO\"/>");
                writer.newLine();
                break;

            case POSTSLO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_POST_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/SAML2/POST/SLO\"/>");
                writer.newLine();
                break;

            case POSTSimpleSignSLO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/SAML2/POST-SimpleSign/SLO\"/>");
                writer.newLine();
                break;

            case SOAPSLO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleLogoutService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write(":8443/idp/profile/SAML2/SOAP/SLO\"/>");
                writer.newLine();
                break;

            case ShibbolethSSO:
                writer.write("        ");
                if (isSAML1Commented()) {
                    openComment();
                }
                writer.write("<");
                writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"urn:mace:shibboleth:1.0:profiles:AuthnRequest\"");
                writer.write(" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/Shibboleth/SSO\"/>");
                writer.newLine();
                if (isSAML1Commented()) {
                    closeComment();
                }
                break;

            case POSTSSO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_POST_BINDING_URI);
                writer.write("\" ");
                writeNameSpaceQualified(SAMLConstants.SAML20PREQ_ATTRR_PREFIX,
                        RequestedAttributes.SUPPORTS_REQUESTED_ATTRIBUTES_LOCAL_NAME);
                writer.write("=\"true\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/SAML2/POST/SSO\"/>");
                writer.newLine();
                break;

            case POSTSimpleSignSSO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI);
                writer.write("\" ");
                writeNameSpaceQualified(SAMLConstants.SAML20PREQ_ATTRR_PREFIX,
                        RequestedAttributes.SUPPORTS_REQUESTED_ATTRIBUTES_LOCAL_NAME);
                writer.write("=\"true\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/SAML2/POST-SimpleSign/SSO\"/>");
                writer.newLine();
                break;

            case RedirectSSO:
                writer.write("        ");
                writer.write("<");
                writer.write(SingleSignOnService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
                writer.write("\" ");
                writeNameSpaceQualified(SAMLConstants.SAML20PREQ_ATTRR_PREFIX,
                        RequestedAttributes.SUPPORTS_REQUESTED_ATTRIBUTES_LOCAL_NAME);
                writer.write("=\"true\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write("/idp/profile/SAML2/Redirect/SSO\"/>");
                writer.newLine();
                break;

            case SAML1Query:
                writer.write("        ");
                if (isSAML1Commented()) {
                    openComment();
                }
                writer.write("<");
                writer.write(AttributeService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML1_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write(":8443/idp/profile/SAML1/SOAP/AttributeQuery\"/>");
                if (isSAML1Commented()) {
                    closeComment();
                }
                writer.newLine();
                break;

            case SAML2Query:
                writer.write("        ");
                if (isSAML2AttributeQueryCommented()) {
                    openComment();
                }
                writer.write("<");
                writer.write(AttributeService.DEFAULT_ELEMENT_LOCAL_NAME);
                writer.write(" Binding=\"");
                writer.write(SAMLConstants.SAML2_SOAP11_BINDING_URI);
                writer.write("\" Location=\"https://");
                writer.write(params.getDnsName());
                writer.write(":8443/idp/profile/SAML2/SOAP/AttributeQuery\"/>");
                if (isSAML2AttributeQueryCommented()) {
                    closeComment();
                    writer.newLine();
                    writer.write("        ");
                    openComment();
                    writer.write(" If you uncomment the above you should add " + SAMLConstants.SAML20P_NS
                            + " to the protocolSupportEnumeration above");
                    closeComment();
                }
                writer.newLine();
                break;

            default:
                break;
        }
    }

    /**
     * Write a namespace:identifier pair.
     *
     * @param nameSpace the namespace
     * @param what the identifier
     * @throws IOException if badness happens
     */
    protected void writeNameSpaceQualified(@Nonnull final String nameSpace, final String what) throws IOException {
        writer.write(nameSpace);
        writer.write(':');
        writer.write(what);
    }

    /** Add an open comment.  If we are nested closes the previous one.
     * @throws IOException if badness happens
     */
    private synchronized void openComment() throws IOException {
        if (commentDepth > 0) {
            writer.write("--> ");
        }
        writer.write("<!--");
        commentDepth++;
    }

    /** Add a close  comment.  If we are nested reopens the previous one.
     * @throws IOException if badness happens
     */
    private synchronized void closeComment() throws IOException {
        writer.write("--> ");
        commentDepth--;
        if (commentDepth > 0) {
            writer.write(" <!--");
        }
    }
}
