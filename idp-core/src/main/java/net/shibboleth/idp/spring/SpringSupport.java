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

package net.shibboleth.idp.spring;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.resource.ResourceException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.w3c.dom.Element;

/**
 * Helper class for performing some common Spring-related functions.
 */
public final class SpringSupport {

    /** Constructor. */
    private SpringSupport() {
    }

    /**
     * Creates a new, started, application context from the given configuration resources.
     * 
     * @param name name of the application context
     * @param configurationResources configuration resources
     * @param parentContext parent context, or null if there is no parent
     * 
     * @return the created context
     */
    public static GenericApplicationContext newContext(String name, List<Resource> configurationResources,
            ApplicationContext parentContext) {
        GenericApplicationContext context = new GenericApplicationContext(parentContext);
        context.setDisplayName("ApplicationContext:" + name);

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        // TODO change opensaml resources in to Spring resource
        // beanDefinitionReader.loadBeanDefinitions(configurationResources.toArray(new Resource[] {}));
        try {
            for (Resource configurationResource : configurationResources) {

                // TODO initialize resources here ?
                configurationResource.initialize();

                beanDefinitionReader.loadBeanDefinitions(new InputStreamResource(
                        configurationResource.getInputStream(), configurationResource.getLocation()));
            }
        } catch (BeanDefinitionStoreException | ComponentInitializationException | ResourceException e) {
            // TODO fix exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        context.refresh();
        return context;
    }

    /**
     * Creates a new, started, application context from the given configuration resources.
     * 
     * @param name name of the application context
     * @param configurationResources configuration resources
     * @param parentContext parent context, or null if there is no parent
     * 
     * @return the created context
     */
    public static GenericApplicationContext newContextSpring(String name,
            List<org.springframework.core.io.Resource> configurationResources, ApplicationContext parentContext) {
        GenericApplicationContext context = new GenericApplicationContext(parentContext);
        context.setDisplayName("ApplicationContext:" + name);

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(configurationResources
                .toArray(new org.springframework.core.io.Resource[] {}));

        context.refresh();
        return context;
    }

    /**
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @return list of bean definitions
     */
    // TODO better javadoc, annotations
    @Nullable public static ManagedList<BeanDefinition> parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements, @Nonnull final ParserContext parserContext) {
        if (elements == null) {
            return null;
        }

        ManagedList<BeanDefinition> definitions = new ManagedList<BeanDefinition>(elements.size());
        for (Element e : elements) {
            // TODO null check e
            definitions.add(parserContext.getDelegate().parseCustomElement(e));
        }

        return definitions;
    }
}