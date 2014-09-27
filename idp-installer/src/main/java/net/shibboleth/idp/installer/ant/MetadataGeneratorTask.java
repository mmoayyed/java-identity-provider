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

package net.shibboleth.idp.installer.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.installer.metadata.MetadataGenerator;
import net.shibboleth.idp.installer.metadata.MetadataGeneratorParameters;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Task to generate metadata.
 */
public class MetadataGeneratorTask extends Task {

    /** Where we collect the parameters. */
    private final MetadataGeneratorParameters parameters;

    /** where to put the data. */
    private File outputFile;
    
    /**
     * Constructor.
     */
    public MetadataGeneratorTask() {

        final Resource resource = new ClassPathResource("net/shibboleth/idp/installer/metadata-generator.xml");

        final ApplicationContextInitializer initializer = new IdPPropertiesApplicationContextInitializer();

        final GenericApplicationContext context =
                SpringSupport.newContext(MetadataGeneratorTask.class.getName(), Collections.singletonList(resource),
                        Collections.<BeanPostProcessor> emptyList(), Collections.singletonList(initializer), null);

        parameters = context.getBean("IdPConfiguration", MetadataGeneratorParameters.class);
    }

    /**
     * Set the output file.
     * 
     * @param file what to set.
     */
    public void setOutput(File file) {

        outputFile = file;
    }

    /**
     * Set the encryption Certificate file. Overrides the Spring definition.
     * 
     * @param file what to set.
     */
    public void setEncryptionCert(File file) {

        parameters.setEncryptionCert(file);
    }

    /**
     * Set the signing Certificate file. Overrides the Spring definition.
     * 
     * @param file what to set.
     */
    public void setSigningCert(File file) {

        parameters.setSigningCert(file);
    }

    /**
     * Set the Backchannel Certificate file.
     * 
     * @param file what to set.
     */
    public void setBackchannelCert(File file) {

        parameters.setBackchannelCert(file);
    }

    /**
     * Sets the entityID. Overrides the Spring definition.
     * 
     * @param id what to set.
     */
    public void setEntityID(String id) {
        parameters.setEntityID(id);
    }

    /**
     * Sets the dns name.
     * 
     * @param name what to set.
     */
    public void setDnsName(String name) {
        parameters.setDnsName(name);
    }

    /**
     * Sets the scope. Overrides the Spring definition.
     * 
     * @param value what to set.
     */
    public void setScope(String value) {
        parameters.setScope(value);
    }

    /** {@inheritDoc} */
    @Override public void execute() {

        try {
            final MetadataGenerator generator = new MetadataGenerator(outputFile);
            final List<List<String>> signing = new ArrayList<>(2);
            List<String> value = parameters.getBackchannelCert();
            if (null != value) {
                signing.add(value);
            }
            value = parameters.getSigningCert();
            if (null != value) {
                signing.add(value);
            }
            generator.setSigningCerts(signing);
            value = parameters.getEncryptionCert();
            if (null != value) {
                generator.setEncryptionCerts(Collections.singletonList(value));
            }
            generator.setDNSName(parameters.getDnsName());
            generator.setEntityID(parameters.getEntityID());
            generator.setScope(parameters.getScope());
            generator.generate();
            
        } catch (Exception e) {
            log("Build failed", e, Project.MSG_ERR);
            throw new BuildException(e);
        }
    }

}
