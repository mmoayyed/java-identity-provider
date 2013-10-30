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

package net.shibboleth.idp.attribute.resolver;

import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

/** A data connector that just returns a static collection of attributes. */
@ThreadSafe
public class MockDataConnector extends AbstractDataConnector implements ValidatableComponent {

    /** Whether this connector fails validation. */
    private boolean invalid;
    
    /** Number of times {@link #destroy()} was called. */
    private int destroyCount;

    /** Number of times {@link #initialize()} was called. */
    private int initializeCount;

    /** Number of times {@link #validate()} was called. */
    private int validateCount;

    /** Static collection of values returned by this connector. */
    private Map<String, IdPAttribute> values;

    /** Exception thrown by {@link #doDataConnectorResolve(AttributeResolutionContext)}. */
    private ResolutionException resolutionException;

    /**
     * Constructor.
     * 
     * @param id unique ID for this data connector
     * @param connectorValues static collection of values returned by this connector
     */
    public MockDataConnector(String id, Map<String, IdPAttribute> connectorValues) {
        setId(id);
        values = connectorValues;
    }

    /**
     * Constructor.
     * 
     * @param id id of the data connector
     * @param exception exception thrown by {@link #doDataConnectorResolve(AttributeResolutionContext)}
     */
    public MockDataConnector(final String id, final ResolutionException exception) {
        setId(id);
        invalid = false;
        resolutionException = exception;
    }

    /**
     * Sets whether this data connector is considered invalid.
     * 
     * @param isInvalid whether this data connector is considered invalid
     */
    public void setInvalid(boolean isInvalid) {
        invalid = isInvalid;
    }

    /** {@inheritDoc} */
    protected Map<String, IdPAttribute> doDataConnectorResolve(AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        if (resolutionException != null) {
            throw resolutionException;
        }

        return values;
    }

    /** {@inheritDoc} */
    public void doValidate() throws ComponentValidationException {
        if (invalid) {
            throw new ComponentValidationException();
        }
        validateCount += 1;
    }
    
    /** {@inheritDoc} */
    public void doDestroy() {
        super.doDestroy();
        destroyCount += 1;
    }


    /** {@inheritDoc} */
    public boolean isInitialized() {
        return initializeCount > 0;
    }

    /** {@inheritDoc} */
    public void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        initializeCount += 1;
    }

    /**
     * Gets the number of times {@link #destroy()} was called.
     * 
     * @return number of times {@link #destroy()} was called
     */
    public int getDestroyCount() {
        return destroyCount;
    }

    /**
     * Gets the number of times {@link #initialize()} was called.
     * 
     * @return number of times {@link #initialize()} was called
     */
    public int getInitializeCount() {
        return initializeCount;
    }

    /**
     * Gets the number of times {@link #validate()} was called.
     * 
     * @return number of times {@link #validate()} was called
     */
    public int getValidateCount() {
        return validateCount;
    }
}