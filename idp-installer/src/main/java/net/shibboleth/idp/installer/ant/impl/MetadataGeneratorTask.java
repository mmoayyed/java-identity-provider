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

package net.shibboleth.idp.installer.ant.impl;

import java.io.File;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.installer.metadata.impl.MetadataGeneratorImpl;
import net.shibboleth.idp.installer.metadata.impl.MetadataGeneratorParametersImpl;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;

/**
 * Task to generate metadata.
 */
public class MetadataGeneratorTask extends Task {

    /** Where to put the data. */
    private File outputFile;

    /** Where idp.home is. */
    @Nullable private String idpHome;

    /** Ant level override for the back channel certificate. */
    @Nullable private File backchannelCert;

    /** Ant level override for the DNS name. */
    @Nullable private String dnsName;

    /**
     * Whether to comment out the SAML2 AA port.
     */
    private boolean saml2AttributeQueryCommented = true;

    /**
     * Whether to comment out the SAML2 SLO endpoints.
     */
    private boolean saml2LogoutCommented = true;

    /**
     * Where is idp.home.
     * 
     * @return Returns idpHome.
     */
    @Nullable public String getIdpHome() {
        return idpHome;
    }

    /**
     * Set where where is idp.home.
     * 
     * @param home The idpHome to set.
     */
    public void setIdpHome(@Nullable final String home) {
        idpHome = home;
    }

    /**
     * Set the output file.
     * 
     * @param file what to set.
     */
    public void setOutput(final File file) {

        outputFile = file;
    }

    /**
     * Set the Backchannel Certificate file.
     * 
     * @param file what to set.
     */
    public void setBackchannelCert(final File file) {
        backchannelCert = file;
    }

    /**
     * Sets the dns name.
     * 
     * @param name what to set.
     */
    public void setDnsName(final String name) {
        dnsName = name;
    }

    /**
     * Returns whether to comment the SAML2 AA endpoint.
     * 
     * @return Returns when to comment the SAML2 AA endpoint.
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

    /** {@inheritDoc} */
    @Override public void execute() {
        try {
            final MetadataGeneratorParametersImpl parameters;

            final Resource resource = new ClassPathResource("net/shibboleth/idp/installer/metadata-generator-ant.xml");

            final GenericApplicationContext context = new ApplicationContextBuilder()
                    .setName(MetadataGeneratorTask.class.getName())
                    .setServiceConfigurations(Collections.singletonList(resource))
                    .setContextInitializer(new Initializer())
                    .build();
            
            parameters = context.getBean("IdPConfiguration", MetadataGeneratorParametersImpl.class);

            parameters.setBackchannelCert(backchannelCert);
            parameters.setDnsName(dnsName);
            parameters.initialize();

            final MetadataGeneratorImpl generator = new MetadataGeneratorImpl();
            generator.setSAML2AttributeQueryCommented(saml2AttributeQueryCommented);
            generator.setSAML2LogoutCommented(saml2LogoutCommented);
            generator.setParameters(parameters);
            generator.setOutput(outputFile);
            generator.initialize();
            generator.generate();

        } catch (final Exception e) {
            log("Build failed", e, Project.MSG_ERR);
            throw new BuildException(e);
        }
    }

    // Checkstyle: CyclomaticComplexity ON

    /**
     * An initializer which knows about our idp.home.
     * 
     */
    public class Initializer extends IdPPropertiesApplicationContextInitializer {

        /** {@inheritDoc} */
        @Override @Nonnull public String selectSearchLocation(
                @Nonnull final ConfigurableApplicationContext applicationContext) {
            if (null == idpHome) {
                return super.selectSearchLocation(applicationContext);
            }
            return idpHome;
        }

        /** {@inheritDoc} */
        @Override @Nonnull public String getSearchLocation() {
            if (null == idpHome) {
                return super.getSearchLocation();
            }
            return idpHome;
        }

    }
}
