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

package net.shibboleth.idp.attribute.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.messaging.context.BaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A {@link BaseContext} that tracks a set of attributes. Usually the tracked attributes are about a particular user and
 * associated with a particular service request.
 */
@NotThreadSafe
public final class AttributeContext extends BaseContext {

    /** The attributes tracked by this context. */
    @Nonnull private Map<String, IdPAttribute> attributes;
    
    /** The attributes tracked by this context prior to filtering. */
    @Nullable private Map<String, IdPAttribute> unfilteredAttributes;
    
    /** Log. */
    private final Logger log;
    
    /** Constructor. */
    public AttributeContext() {
        attributes = Collections.emptyMap();
        log = LoggerFactory.getLogger(AttributeContext.class);
    }

    /**
     * Gets the map of attributes, indexed by attribute ID, tracked by this context.
     * 
     * @return the collection of attributes indexed by attribute ID
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, IdPAttribute> getIdPAttributes() {
        return attributes;
    }

    /**
     * Sets the attributes tracked by this context.
     * 
     * @param newAttributes the attributes
     */
    public void setIdPAttributes(@Nonnull @NonnullElements final Collection<IdPAttribute> newAttributes) {
        Constraint.isNotNull(newAttributes, "Attributes inserted into AttributeContext should not be null");

        attributes = newAttributes.
                stream().
                collect(Collectors.collectingAndThen(
                            Collectors.toMap(IdPAttribute::getId, a -> a),
                            Collections::unmodifiableMap));
    }
    
    
    /**
     * Gets the map of unfiltered attributes, indexed by attribute ID, tracked by this context.
     * 
     * @return the collection of attributes indexed by attribute ID
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, IdPAttribute> getUnfilteredIdPAttributes() {
        if (null == unfilteredAttributes) {
            log.error("No Attributes have been set in this flow.");
            return Collections.emptyMap();
        }
        return unfilteredAttributes;
    }

    /**
     * Sets the unfiltered attributes tracked by this context.
     * 
     * @param newAttributes the attributes
     */
    public void setUnfilteredIdPAttributes(@Nonnull @NonnullElements final Collection<IdPAttribute> newAttributes) {
        Constraint.isNotNull(newAttributes, "Attributes inserted into AttributeContext should not be null");
        if (null != unfilteredAttributes) {
            log.error("Unfiltered attributes have already been set in this flow.");
        }
        
        unfilteredAttributes = newAttributes.
                stream().
                collect(Collectors.collectingAndThen(
                            Collectors.toMap(IdPAttribute::getId, a -> a),
                            Collections::unmodifiableMap));
    }
}