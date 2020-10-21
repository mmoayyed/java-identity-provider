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

/*
 * Derived from work (c) 2015 CSC, see included license.
 */

package net.shibboleth.idp.attribute.resolver.dc.storage.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.NoResultAnErrorResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.idp.attribute.resolver.dc.impl.AbstractSearchDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.storage.StorageMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.storage.StorageServiceSearch;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * This class implements a {@link net.shibboleth.idp.attribute.resolver.DataConnector}
 * that obtains data from a {@link StorageService}.
 * 
 * @since 4.1.0
 */
public class StorageServiceDataConnector
        extends AbstractSearchDataConnector<StorageServiceSearch,StorageMappingStrategy> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageServiceDataConnector.class);

    /** The {@link StorageService} to use. */
    @NonnullAfterInit private StorageService storageService;
    
    /** ID of the attribute generated by this data connector if simple result mapping used. */
    @NonnullAfterInit private String generatedAttributeID;
    
    /** Whether no record is an error. */
    private boolean noResultAnError;
    
    /** Constructor. */
    public StorageServiceDataConnector() {
        setValidator(new Validator() {
            public void validate() throws ValidationException {
            }

            public void setThrowValidateError(final boolean what) {
            }

            public boolean isThrowValidateError() {
                return false;
            }
        });
    }

    /**
     * Set the {@link StorageService} to use.
     * 
     * @param service storage service to use
     */
    public void setStorageService(@Nonnull final StorageService service) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        storageService = Constraint.isNotNull(service, "StorageService cannot be null");
    }
    
    /**
     * Get the ID of the attribute generated by this connector if simple result mapping used.
     * 
     * @return ID of the attribute generated by this connector
     */
    @NonnullAfterInit public String getGeneratedAttributeID() {
        return generatedAttributeID;
    }

    /**
     * Sets whether the lack of a returned record constitutes an error.
     * 
     * @param flag flag to set
     */
    public void setNoResultAnError(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        noResultAnError = flag;
    }
    
    /**
     * Set the ID of the attribute generated by this connector if simple result mapping used.
     * 
     * @param id what to set.
     */
    public void setGeneratedAttributeID(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        generatedAttributeID = StringSupport.trimOrNull(id);
    }
    
    /** {@inheritDoc} */
    public void doInitialize() throws ComponentInitializationException {

        if (storageService == null) {
            throw new ComponentInitializationException(getLogPrefix() + " StorageService cannot be null");
        }

        if (getMappingStrategy() == null) {
            if (generatedAttributeID == null) {
                throw new ComponentInitializationException(
                        getLogPrefix() + " No mapping strategy or generated attribute ID set");
            }
            setMappingStrategy(new SimpleStorageMappingStrategy(generatedAttributeID));
        }
        
        super.doInitialize();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Map<String,IdPAttribute> retrieveAttributes(@Nonnull final StorageServiceSearch executable)
            throws ResolutionException {

        try {
            
            final StorageRecord<?> record = executable.execute(storageService);
            if (record == null) {
                if (noResultAnError) {
                    throw new NoResultAnErrorResolutionException(getLogPrefix() + " No record returned");
                }
                return Collections.emptyMap();
            }
            
            return getMappingStrategy().map(record);
        } catch (final IOException e) {
            throw new ResolutionException(getLogPrefix() + " StorageService read failed", e);
        }
    }

}