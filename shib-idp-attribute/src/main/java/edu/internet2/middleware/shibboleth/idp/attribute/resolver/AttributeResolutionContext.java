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

package edu.internet2.middleware.shibboleth.idp.attribute.resolver;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;

import net.jcip.annotations.NotThreadSafe;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.Context;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.messaging.context.impl.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.Strings;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;

/** A context which carries and collects information through an attribute resolution. */
@NotThreadSafe
public final class AttributeResolutionContext extends AbstractSubcontextContainer implements Context,
        SubcontextContainer {

    /** Unique identifier of the context. */
    private String id;

    /** Instant this context was created. */
    private DateTime creationInstant;

    /** IDs of attributes that have been requested to be resolved. */
    private Set<String> requestAttributes;

    /** Subject to whom the resolved attributes belong. */
    private Subject requestSubject;

    /** Attributes which have been resolved. */
    private Map<String, Attribute<?>> resolvedAttributes;

    /** Constructor. Generates a random ID for the context and sets creation time to now. */
    public AttributeResolutionContext(Subject subject) {
        this(subject, null);
    }
    
    /**
     * Constructor.
     *
     * @param subject the subject for whom attributes will be resolved
     * @param additionalData additional data about the context in which the resolution is taking place
     */
    public AttributeResolutionContext(Subject subject, Context additionalData){
        setId(UUID.randomUUID().toString());
        setCreationInstant(new DateTime());
        setRequestSubject(subject);
        addAdditionalInformation(additionalData);
        requestAttributes = new LazySet<String>();
        resolvedAttributes = new LazyMap<String, Attribute<?>>();
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of this context.
     * 
     * @param contextId ID of the context, never null or empty
     */
    private void setId(String contextId) {
        String trimmedId = Strings.trimOrNull(contextId);
        Assert.isNotNull(trimmedId, "Attribute resolution context may not be null or empty");
        id = trimmedId;
    }

    /** {@inheritDoc} */
    public DateTime getCreationTime() {
        return creationInstant;
    }

    /**
     * Sets the creation instant for this context.
     * 
     * @param instant creation instant for this context, never null
     */
    private void setCreationInstant(DateTime instant) {
        Assert.isNotNull(instant, "Attribute resolution context may not be null or empty");
        creationInstant = instant.toDateTime(ISOChronology.getInstanceUTC());
    }

    /**
     * Gets the subject whose attributes are resolved.
     * 
     * @return subject whose attributes are resolved, never null
     */
    public Subject getRequestSubject() {
        return requestSubject;
    }

    /**
     * Sets the subject whose attributes are resolved.
     * 
     * @param subject subject whose attributes are resolved, never null
     */
    private void setRequestSubject(Subject subject) {
        Assert.isNotNull(subject, "Request subject may not be null");
        requestSubject = subject;
    }

    /**
     * Gets the set of attributes requested to be resolved.
     * 
     * @return set of attributes requested to be resolved, never null
     */
    public Set<String> getRequestAttributes() {
        return requestAttributes;
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
     * Adds data contained 
     * @param data
     */
    private void addAdditionalInformation(Context data){
        //TODO
    }
}