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

package net.shibboleth.attribute.filtering.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringEngine;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.service.AbstractSpringService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

//TODO incomplete
/**
 * The attribute filter service manages the lifecycle of an attribute filtering engine, where the lifecycle comprises
 * starting, stopping, and configuration reloading.
 */
public class AttributeFilterService extends AbstractSpringService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterService.class);

    /** The attribute filtering engine. */
    private AttributeFilteringEngine attributeFilteringEngine;

    public void filterAttributes(@Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        attributeFilteringEngine.filterAttributes(filterContext);
    }

    protected void doPreStart(HashMap context) throws ServiceException {

        // TODO do we have to init resources here ?
        log.debug("getServiceConfigurations() '{}'", getServiceConfigurations());
        for (Resource resource : this.getServiceConfigurations()) {
            try {
                log.debug("initializing resource '{}'", resource);
                resource.initialize();
            } catch (ComponentInitializationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new ServiceException(e);
            }
        }

        super.doPreStart(context);
    }

    /** {@inheritDoc} */
    protected void doStart(@Nonnull final HashMap context) throws ServiceException {

        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);

        log.info("appCtx '{}'", appCtx);

        List<AttributeFilterPolicy> policies = new ArrayList<AttributeFilterPolicy>();
        String[] beanNames = appCtx.getBeanNamesForType(AttributeFilterPolicy.class);
        for (String beanName : beanNames) {
            log.debug("policy bean name '{}'", beanName);
            policies.add((AttributeFilterPolicy) appCtx.getBean(beanName));
        }
        log.debug("instantiating AttributeFilteringEngine with id '{}' and policies '{}'", getId(), policies);
        attributeFilteringEngine = new AttributeFilteringEngine(getId(), policies);

        try {
            attributeFilteringEngine.initialize();
        } catch (ComponentInitializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ServiceException(e);
        }
    }

}
