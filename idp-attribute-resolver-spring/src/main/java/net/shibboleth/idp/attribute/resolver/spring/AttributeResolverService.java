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
package net.shibboleth.idp.attribute.resolver.spring;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.service.AbstractSpringService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

// TODO incomplete
/**
 * The attribute resolver service manages the lifecycle of an attribute resolver, where the lifecycle comprises
 * starting, stopping, and configuration reloading.
 */
public class AttributeResolverService extends AbstractSpringService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolverService.class);

    /** The attribute resolver. */
    private AttributeResolver attributeResolver;

    /**
     * TODO finish
     * 
     * The service proxies the underlying components functionality.
     * 
     * @param resolutionContext context for the resolution.
     * @throws ResolutionException if the resolution fails.
     */
    public void resolveAttributes(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        attributeResolver.resolveAttributes(resolutionContext);
    }
    
    
    /** Return the configured resolver.  
     * TODO - do we want to return this, or to return the AttributeMapper.
     * @return the resolver.
     */
    public AttributeResolver getAttributeResolver(){
        return attributeResolver;
    }

    // TODO probably wrong
    /** {@inheritDoc} */
    protected void doPreStart(@SuppressWarnings("rawtypes") HashMap context) throws ServiceException {

        // TODO do we have to init resources here ?
        log.debug("getServiceConfigurations() '{}'", getServiceConfigurations());

        super.doPreStart(context);

        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);

        // attribute definitions
        Map<String, AttributeDefinition> adMap = appCtx.getBeansOfType(AttributeDefinition.class);
        log.debug("Loading {} attribute definitions", adMap.size());

        // data connectors
        Map<String, DataConnector> dataConnectorMap = appCtx.getBeansOfType(DataConnector.class);
        log.debug("Loading {} data connectors", dataConnectorMap.size());

        attributeResolver = new AttributeResolverImpl(getId(), adMap.values(), dataConnectorMap.values());

        try {
            attributeResolver.initialize();
        } catch (ComponentInitializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
