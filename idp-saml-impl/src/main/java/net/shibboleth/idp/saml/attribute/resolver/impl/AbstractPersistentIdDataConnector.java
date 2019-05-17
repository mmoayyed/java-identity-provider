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

package net.shibboleth.idp.saml.attribute.resolver.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * The basis of a {@link net.shibboleth.idp.attribute.resolver.DataConnector} that handles persistent IDs that depend on
 * a source {@link IdPAttribute}.
 */
public abstract class AbstractPersistentIdDataConnector extends AbstractDataConnector {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractPersistentIdDataConnector.class);

    /** ID of the attribute generated by this data connector. */
    @NonnullAfterInit private String generatedAttribute;

    /** Information about the dependency. */
    @NonnullAfterInit private String sourceInformation;

    /**
     * Get Information about the attribute whose first value is used when generating the computed ID.
     * This is derived from the sourceID (if present) and/or the dependencies.  
     * Public purely as an aid to testing.
     *
     * @return log-friend information.
     */
    @Nullable @NonnullAfterInit public String getSourceAttributeInformation() {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return sourceInformation;
    }

    /**
     * Get the ID of the attribute generated by this connector.
     * 
     * @return ID of the attribute generated by this connector
     */
    @NonnullAfterInit public String getGeneratedAttributeId() {
        return generatedAttribute;
    }

    /**
     * Set the ID of the attribute generated by this connector.
     * 
     * @param newAttributeId what to set.
     */
    public void setGeneratedAttributeId(@Nullable final String newAttributeId) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        generatedAttribute = newAttributeId;
    }
    
    /**
     * Do the dance with dependencies.
     * 
     * Old style ones get the sourceId added (failing if it isn't there).
     * New style ones get their names added to the information string.
     *
     * @throws ComponentInitializationException if the dependencies are not aligned correctly
     */
    private void doDependencyInformation() throws ComponentInitializationException {
        final StringBuilder dependencyInformation = new StringBuilder();
        boolean seenAttribute = false;

        for (final ResolverAttributeDefinitionDependency attrDep : getAttributeDependencies()) {
            if (seenAttribute) {
                dependencyInformation.append(", ");
            }
            dependencyInformation.append(attrDep.getDependencyPluginId());
            seenAttribute = true;
        }

        for (final ResolverDataConnectorDependency dataConnectorDependency : getDataConnectorDependencies()) {
            if (seenAttribute) {
                dependencyInformation.append(", ");
            }
            if (dataConnectorDependency.isAllAttributes()) {
                dependencyInformation.append(dataConnectorDependency.getDependencyPluginId()).append("/*");
            } else if (dataConnectorDependency.getAttributeNames().isEmpty()) {
                throw new ComponentInitializationException(getLogPrefix() + " No source attribute present.");
            } else if (dataConnectorDependency.getAttributeNames().size() == 1) {
                dependencyInformation.append(dataConnectorDependency.getDependencyPluginId()).
                                      append('/').
                                      append(dataConnectorDependency.getAttributeNames().iterator().next());
            } else {
                dependencyInformation.append(dataConnectorDependency.getDependencyPluginId()).
                                      append('/').
                                      append(dataConnectorDependency.getAttributeNames().toString());
            }
            seenAttribute = true;
        }
        
        if (!seenAttribute) {
            throw new ComponentInitializationException(getLogPrefix() +
                    " No source attribute present in the supplied Dependencies");
        }
        sourceInformation = dependencyInformation.toString();
        log.debug("{} Source for definition: {}", getLogPrefix(), sourceInformation);
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException { 

        // Set up the dependencies first. Then the initialize in the parent
        // will correctly rehash the dependencies.
        doDependencyInformation();
        super.doInitialize();

        if (null == generatedAttribute) {
            generatedAttribute = getId();
            log.info("{} No generated attribute ID supplied, using ID of connector: {}", getLogPrefix(),
                    generatedAttribute);
        }
    }

    /**
     * Helper function to locate the source Attribute in the dependencies.
     * 
     * @param workContext the context to look in
     * @return the value, or null in any of the failure cases.
     */
    @Nullable protected String resolveSourceAttribute(@Nonnull final AttributeResolverWorkContext workContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final List<IdPAttributeValue> attributeValues =
                PluginDependencySupport.getMergedAttributeValues(workContext,
                        getAttributeDependencies(),
                        getDataConnectorDependencies(),
                        getId());
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("{} Source attribute {} for connector {} provide no values", getLogPrefix(),
                    getSourceAttributeInformation(), getId());
            return null;
        }

        if (attributeValues.size() > 1) {
            log.warn("{} Source attribute {} for connector {} has more than one value, only one value is used",
                    getLogPrefix(), getSourceAttributeInformation(), getId());
        }

        final IdPAttributeValue attributeValue = attributeValues.iterator().next();

        final String val;

        if (attributeValue instanceof StringAttributeValue) {
            val = ((StringAttributeValue) attributeValue).getValue();
            if (StringSupport.trimOrNull(val) == null) {
                log.warn("{} Source attribute {} for connector {} was all-whitespace", getLogPrefix(),
                        getSourceAttributeInformation(), getId());
                return null;
            }
        } else if (attributeValue instanceof EmptyAttributeValue) {
            final EmptyAttributeValue emptyVal = (EmptyAttributeValue) attributeValue;
            log.warn("{} Source attribute {} value for connector {} was an empty value of type {}", getLogPrefix(),
                    getSourceAttributeInformation(), getId(), emptyVal.getDisplayValue());
            return null;
        } else {
            log.warn("{} Source attribute {} for connector {} was of an unsupported type: {}", getLogPrefix(),
                    getSourceAttributeInformation(), getId(), attributeValue.getClass().getName());
            return null;
        }

        if (val == null) {
            log.warn("{} Attribute value {} for connector {} resolved as empty or null", getLogPrefix(),
                    getSourceAttributeInformation(), getId());
        }
        return val;
    }

    /**
     * Encode the provided string.
     * 
     * @param value the value to encode or null if that failed
     * @return null or the attribute.
     */
    @Nullable protected Map<String, IdPAttribute> encodeAsAttribute(@Nullable final String value) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        if (null == value) {
            // The message will have been logged above
            return null;
        }
        final IdPAttribute attribute = new IdPAttribute(getGeneratedAttributeId());
        attribute.setValues(Collections.singletonList(StringAttributeValue.valueOf(value)));
        return Collections.singletonMap(getGeneratedAttributeId(), attribute);
    }

}
