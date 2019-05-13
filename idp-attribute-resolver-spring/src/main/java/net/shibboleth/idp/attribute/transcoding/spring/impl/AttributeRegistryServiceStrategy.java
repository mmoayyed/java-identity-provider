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

package net.shibboleth.idp.attribute.transcoding.spring.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.attribute.transcoding.impl.TranscodingRuleLoader;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ServiceException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Strategy for summoning up an {@link AttributeTranscoderRegistryImpl} from a populated {@link ApplicationContext}.
 */
public class AttributeRegistryServiceStrategy extends AbstractIdentifiableInitializableComponent implements
        Function<ApplicationContext,ServiceableComponent<AttributeTranscoderRegistry>> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeRegistryServiceStrategy.class);

    /** Name of bean to supply naming function registry property. */
    @Nullable @NotEmpty private String namingRegistry;
    
    /**
     * Set the name of the bean providing the {@link AttributeTranscoderRegistryImpl#setNamingRegistry()} value.
     * 
     * @param beanName name of bean of type {@link Map}
     */
    public void setNamingRegistry(@Nullable @NotEmpty final String beanName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        namingRegistry = StringSupport.trimOrNull(beanName);
    }
    
    /** {@inheritDoc} */
    @Nullable public ServiceableComponent<AttributeTranscoderRegistry> apply(
            @Nullable final ApplicationContext appContext) {

        final Map<Class<?>,Function<?,String>> namingRegistryBean = appContext.getBean(namingRegistry, Map.class);
        
        final Collection<TranscodingRule> mappingBeans = appContext.getBeansOfType(TranscodingRule.class).values();
        final Collection<TranscodingRuleLoader> loaderBeans =
                appContext.getBeansOfType(TranscodingRuleLoader.class).values();

        final Collection<TranscodingRule> holder = new ArrayList<>();
        if (mappingBeans != null) {
            holder.addAll(mappingBeans);
        }
        if (loaderBeans != null) {
            loaderBeans.forEach(loader -> holder.addAll(loader.getRules()));
        }
        
        final AttributeTranscoderRegistryImpl registry = new AttributeTranscoderRegistryImpl();
        registry.setId(getId());
        registry.setApplicationContext(appContext);
        registry.setNamingRegistry(namingRegistryBean);
        registry.setTranscoderRegistry(holder);

        try {
            registry.initialize();
        } catch (final ComponentInitializationException e) {
            throw new ServiceException("Unable to initialize attribute transcoder registry for "
                    + appContext.getDisplayName(), e);
        }
        return registry;
    }
    
}