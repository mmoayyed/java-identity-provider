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

package net.shibboleth.idp.saml.attribute;

import net.shibboleth.idp.attribute.AttributeEncoder;

import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;

/**
 * Base class for encoders that produce SAML attributes.
 * 
 * @param <AttributeType> type of attribute produced
 */
public abstract class AbstractSamlAttributeEncoder<AttributeType> implements AttributeEncoder<AttributeType>,
        UnmodifiableComponent, InitializableComponent {

    /** Whether this encoder has been initialized. */
    private boolean initialized;

    /** The name of the attribute. */
    private String name;

    /** The namespace in which the attribute name is interpreted. */
    private String namespace;

    /**
     * Gets the name of the attribute.
     * 
     * @return name of the attribute, never null after initialization
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the name of the attribute.
     * 
     * @param attributeName name of the attribute
     */
    public final synchronized void setName(String attributeName) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute name can not be changed after encoder has been initialized");
        }
        name = StringSupport.trimOrNull(attributeName);
    }

    /**
     * Gets the namespace in which the attribute name is interpreted.
     * 
     * @return namespace in which the attribute name is interpreted, never null after initialization
     */
    public final String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace in which the attribute name is interpreted.
     * 
     * @param attributeNamespace namespace in which the attribute name is interpreted
     */
    public final synchronized void setNamespace(String attributeNamespace) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute name format can not be changed after encoder has been initialized");
        }
        namespace = StringSupport.trimOrNull(attributeNamespace);
    }

    /** {@inheritDoc} */
    public final boolean isInitialized() {
        return initialized;
    }

    /** {@inheritDoc} */
    public final synchronized void initialize() throws ComponentInitializationException {
        if (name == null) {
            throw new ComponentInitializationException("Attribute name can not be null or empty");
        }

        if (namespace == null) {
            throw new ComponentInitializationException("Attribute namespace can not be null or empty");
        }

        doInitialize();

        initialized = true;
    }

    /**
     * Performs additional component initialization. This method is called after the checks ensuring the attribute name
     * and namespace are populated.
     * 
     * Default implementation of this method is a no-op
     * 
     * @throws ComponentInitializationException thrown if there is a problem initializing this encoder
     */
    protected void doInitialize() throws ComponentInitializationException {

    }
}