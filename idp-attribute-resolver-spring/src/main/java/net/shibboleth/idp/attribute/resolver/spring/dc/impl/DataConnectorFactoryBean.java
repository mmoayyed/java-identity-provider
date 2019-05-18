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

package net.shibboleth.idp.attribute.resolver.spring.dc.impl;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.impl.AbstractResolverPluginFactoryBean;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A factory bean to collect the parameterization that goes onto a {@link AbstractDataConnector}.
 * 
 * It is specifically aimed at the implementations where contents are plugged in via external resources.
 * 
 */
public class DataConnectorFactoryBean extends AbstractResolverPluginFactoryBean<AbstractDataConnector> implements
        ApplicationContextAware {

    /** Log4j logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DataConnectorFactoryBean.class);

    /** The class that we are implementing. */
    @Nonnull private Class<? extends AbstractDataConnector> connectorClass;

    /** The resources we are importing. */
    @Nullable private List<Resource> resources;

    /** Our parent context. */
    @Nullable private ApplicationContext parentContext;

    /** The context we fired up. */
    private GenericApplicationContext appContext;

    /** List of bean factory post processors for this connector's content. */
    @Nonnull @NonnullElements private List<BeanFactoryPostProcessor> factoryPostProcessors = Collections.emptyList();

    /** List of bean post processors for this connector's content. */
    @Nonnull @NonnullElements private List<BeanPostProcessor> postProcessors = Collections.emptyList();

    /** Data Connector property "failoverDataConnectorId". */
    @Nullable private String failoverDataConnectorId;

    /** Data Connector property "noRetryDelay". */
    @Nullable private Duration noRetryDelay;

    /** Do we release all attributes?. */
    private Boolean exportAllAttributes;

    /** Which named attributes do we release?. */
    @Nonnull @NonnullElements private Collection<String> exportAttributes = Collections.EMPTY_SET;

    /**
     * Data Connector property "failoverDataConnectorId".
     *
     * @return the value of property to set or null if never set
     */
    @Nullable public String getFailoverDataConnectorId() {
        return failoverDataConnectorId;
    }

    /**
     * Data Connector property "failoverDataConnectorId".
     *
     * @param id the value to set
     */
    public void setFailoverDataConnectorId(@Nullable final String id) {
        failoverDataConnectorId = id;
    }
    
    /**
     * Data Connector property "noRetryDelay".
     *
     * @return the value of property to set or null if never set
     */
    @Nullable public Duration getNoRetryDelay() {
        return noRetryDelay;
    }
    
    /**
     * Data Connector property "noRetryDelay".
     *
     * @param delay the value to set
     */
    public void setNoRetryDelay(@Nullable final Duration delay) {
        noRetryDelay = delay;
    }

    /**
     * The resources to use.
     *
     * @param theResources the resources to look at
     */
    public void setResources(@Nonnull @NonnullElements final List<Resource> theResources) {
        resources =
                ImmutableList.<Resource> builder().addAll(Iterables.filter(theResources, Predicates.notNull())).build();
    }

    /**
     * The resources to use.
     *
     * @return the resources to look at
     */
    @Nonnull @NonnullElements public List<Resource> getResources() {
        return resources;
    }

    /**
     * Sets the list of attribute names to export during resolution.
     *
     * @param what the list
     */
    public void setExportAttributes(@Nonnull final Collection<String> what) {
        exportAttributes = what;
    }

   /**
     * Set whether we export all attributes.
     *
     * @param what whether we export all attributes
     */
    public void setExportAllAttributes(final boolean what) {
        exportAllAttributes = what;
    }

    /**
     * Set the list of bean factory post processors for this connector.
     *
     * @param processors bean factory post processors to apply
     */
    public void setBeanFactoryPostProcessors(@Nonnull @NonnullElements 
            final List<BeanFactoryPostProcessor> processors) {
        factoryPostProcessors = new ArrayList<>(Collections2.filter(processors, Predicates.notNull()));
    }

    /**
     * Get the post processors.
     *
     * @return the bean factory post processors
     */
    @Nonnull @NonnullElements public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
        return factoryPostProcessors;
    }

    /**
     * Set the list of bean post processors for this connector.
     *
     * @param processors bean post processors to apply
     */
    public void setBeanPostProcessors(@Nonnull @NonnullElements final List<BeanPostProcessor> processors) {
        postProcessors = new ArrayList<>(Collections2.filter(processors, Predicates.notNull()));
    }

    /**
     * Get the list of bean post processors for this connector.
     *
     * @return processors bean post processors to apply
     */
    @Nonnull @NonnullElements public List<BeanPostProcessor> getBeanPostProcessors() {
        return postProcessors;
    }

    /** {@inheritDoc} */
    @Override protected void setValues(final AbstractDataConnector what) {
        super.setValues(what);
        if (null != getFailoverDataConnectorId()) {
            what.setFailoverDataConnectorId(getFailoverDataConnectorId());
        }
        if (null != exportAllAttributes) {
            what.setExportAllAttributes(exportAllAttributes);
        } else if (!exportAttributes.isEmpty()) {
            what.setExportAttributes(exportAttributes);
        }
    }

    /** {@inheritDoc} We do not allow non-singleton beans, if we did then we loose constructability. */
    @Override public void setSingleton(final boolean singleton) {
        Constraint.isTrue(singleton, "Can only be singleton");
        super.setSingleton(singleton);
    }

    /** Set the class we are going to build.
     * @param claz the class.
     */
    public void setObjectType(@Nonnull final Class<? extends AbstractDataConnector> claz) {
        connectorClass = Constraint.isNotNull(claz, "Injected class must be non-null");
    }

    /** {@inheritDoc} */
    @Override public Class<? extends AbstractDataConnector> getObjectType() {
        return connectorClass;
    }

    /**
     * Returns the parentContext.
     * 
     * @return Returns the parentContext.
     */
    public ApplicationContext getParentContext() {
        return parentContext;
    }

    /** {@inheritDoc} */
    @Override public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        parentContext = applicationContext;
    }

    /** {@inheritDoc} */
    @Override protected void destroyInstance(final AbstractDataConnector instance) throws Exception {
        super.destroyInstance(instance);
        appContext.close();
    }

    /**
     * {@inheritDoc}. <br/>
     * In order to create the bean we introspect with respect to contents of the Spring resources and inject as
     * required.
     */
    @Override protected AbstractDataConnector doCreateInstance() throws Exception {

        Constraint.isNotNull(getObjectType(), "Injected class must be non-null");
        log.debug("Creating a DataConnector of type {} from resources {}", getObjectType(), resources);

        final Constructor<? extends AbstractDataConnector> constructor = getObjectType().getConstructor();
        final AbstractDataConnector result = constructor.newInstance();
        if (null != getFailoverDataConnectorId()) {
            result.setFailoverDataConnectorId(getFailoverDataConnectorId());
        }
        if (null != getNoRetryDelay()) {
            result.setNoRetryDelay(getNoRetryDelay());
        }
        setValues(result);

        appContext = new ApplicationContextBuilder()
                .setName("HybridSpringDataConnector")
                .setServiceConfigurations(getResources())
                .setBeanFactoryPostProcessors(getBeanFactoryPostProcessors())
                .setBeanPostProcessors(getBeanPostProcessors())
                .setParentContext(getParentContext()).build();
        
        final PropertyDescriptor[] descriptors =
                Introspector.getBeanInfo(getObjectType(), AbstractDataConnector.class).getPropertyDescriptors();

        for (final PropertyDescriptor descriptor : descriptors) {
            log.debug("Parsing property descriptor: {}", descriptor);
            final Map<String, ?> beans = appContext.getBeansOfType(descriptor.getPropertyType());

            if (null == beans || beans.isEmpty() || null == descriptor.getWriteMethod() ) {
                continue;
            }
            if (beans.size() > 1) {
                log.warn("Too many beans of type {} found, only the first will be used", descriptor.getPropertyType());
            }
            final Object bean = beans.values().iterator().next();
            log.debug("Added property value: {}", bean);
            descriptor.getWriteMethod().invoke(result, bean);
        }
        return result;
    }
    
}