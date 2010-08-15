/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.attribute.filtering;

import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.Context;
import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.collections.LazyMap;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;

/** Context used to collect data as attributes are filtered. */
public final class AttributeFilterContext implements Context, Subcontext {

    /** Context which acts as the owner or parent of this context. */
    private SubcontextContainer parentContext;

    /** Unique identifier of the context. */
    private String id;

    /** Instant this context was created. */
    private DateTime creationInstant;

    /** Attributes which have been resolved. */
    private Map<String, Attribute<?>> resolvedAttributes;

    /** Attributes which have been resolved. */
    private Map<String, Attribute<?>> filteredAttributes;

    /**
     * Constructor.
     * 
     * @param subject the subject for whom attributes will be resolved
     * @param additionalData additional data about the context in which the resolution is taking place
     */
    public AttributeFilterContext(SubcontextContainer parent) {
        id = UUID.randomUUID().toString();
        creationInstant = new DateTime(ISOChronology.getInstanceUTC());

        parentContext = parent;
        parent.addSubcontext(this);

        resolvedAttributes = new LazyMap<String, Attribute<?>>();
        filteredAttributes = new LazyMap<String, Attribute<?>>();
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return parentContext;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public DateTime getCreationTime() {
        return creationInstant;
    }

    /**
     * Gets the attributes that have been resolved, indexed by attribute ID.
     * 
     * @return attributes that have been resolved, never null
     */
    public Map<String, Attribute<?>> getResolvedAttributes() {
        return resolvedAttributes;
    }

    /**
     * Gets the attributes, indexed by ID, left after the filtering process has run.
     * 
     * @return attributes left after the filtering process has run, never null
     */
    public Map<String, Attribute<?>> getFilteredAttributes() {
        return filteredAttributes;
    }
}