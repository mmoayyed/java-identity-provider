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

package net.shibboleth.idp.attribute.filter.spring;

import java.util.Collection;
import java.util.HashMap;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterImpl;
import net.shibboleth.idp.attribute.filter.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.service.AbstractSpringService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

//TODO incomplete
/** This service wraps an {@link AttributeFilter}. */
public class AttributeFilterService extends AbstractSpringService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterService.class);

    /** The wrapped attribute filtering engine. */
    private AttributeFilter attributeFilter;

    /**
     * Return the configured filter. TODO - do we want to return this
     * 
     * @return the filter.
     */
    public AttributeFilter getAttributeFilter() {
        return attributeFilter;
    }
    
    /**
     * Filters attributes and values. This filtering process may remove attributes and values but must never add them.
     * 
     * @param filterContext context containing the attributes to be filtered and collecting the results of the filtering
     *            process
     * @throws AttributeFilterException thrown if there is a problem retrieving or applying the attribute filter
     *             policy
     * @see AttributeFilter#filterAttributes(AttributeFilterContext)
     */
    public void filterAttributes(@Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilterException {
        // TODO readlock ?
        attributeFilter.filterAttributes(filterContext);
    }

    /**
     * {@inheritDoc}
     * 
     * This method instantiates and initializes the wrapped {@link AttributeFilter} with engine ID the same as the
     * service ID and {@link AttributeFilterPolicy}s from the service specific Spring application context.
     */
    protected void doPreStart(@SuppressWarnings("rawtypes") HashMap context) throws ServiceException {
        super.doPreStart(context);

        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);

        Collection<AttributeFilterPolicy> policies = appCtx.getBeansOfType(AttributeFilterPolicy.class).values();

        attributeFilter = new AttributeFilterImpl(getId(), policies);
        try {
            ComponentSupport.initialize(attributeFilter);
        } catch (ComponentInitializationException e) {
            throw new ServiceException("Unable to initialize the attribute filtering engine", e);
        }

        log.debug("Attribute Filter Service '{}' : Initialized new attribute filtering engine.", getId());
    }
}
