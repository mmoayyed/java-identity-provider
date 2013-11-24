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

package net.shibboleth.idp.spring.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.component.AbstractDestructableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.resource.ResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * A simple implemention of {@link import net.shibboleth.utilities.java.support.resource.Resource} which takes a spring
 * {@link Resource} as input.
 * 
 * TODO - the interface we are implementing will move and this needs to reflect that.
 */
public class ShibbolethResourceImpl extends AbstractDestructableInitializableComponent implements
        net.shibboleth.utilities.java.support.resource.Resource {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ShibbolethResourceImpl.class);


    /**
     * The Spring resource.
     */
    private final Resource resource;

    /**
     * Constructor.
     * 
     * @param theResource what to encapsulate
     */
    public ShibbolethResourceImpl(@Nonnull Resource theResource) {
        resource = theResource;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        throw new ComponentValidationException();
    }

    /** {@inheritDoc} */
    @Nonnull public String getLocation() {
        try {
            return resource.getURL().toExternalForm();
        } catch (IOException e) {
            log.error("Couldn't find location {}", e);
            //TODO
            return "";
        }
    }

    /** {@inheritDoc} */
    public boolean exists() throws ResourceException {
        return resource.exists();
    }

    /** {@inheritDoc} */
    @Nonnull public InputStream getInputStream() throws ResourceException {
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    /** {@inheritDoc} */
    public long getLastModifiedTime() throws ResourceException {
        try {
            return resource.lastModified();
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

}
