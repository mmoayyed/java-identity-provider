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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * An {@link AttributeDefinition} that creates an attribute whose values are the values
 * of all its dependencies, passed through or converted into a {@link DateTimeAttributeValue}.
 * 
 * <p>Values already of this type are simple passed through, while {@link StringAttributeValue} objects
 * are converted. It is optional whether to omit incompatible values or raise an error.</p>
 * 
 * @since 4.3.0
 */
@ThreadSafe
public class DateTimeAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(DateTimeAttributeDefinition.class);

    /** Formatter for string to date/time conversion. */
    @Nullable private DateTimeFormatter formatter;
    
    /** Convert numeric strings into epoch as seconds. */
    private boolean epochInSeconds;
    
    /** Do we ignore converstion failures? */
    private boolean ignoreConversionErrors;

    /** Constructor. */
    public DateTimeAttributeDefinition() {
        epochInSeconds = true;
    }
    
    /**
     * Set whether to convert numeric string data into an epoch using seconds instead of milliseconds.
     * 
     * <p>Defaults to true.</p>
     * 
     * @param flag
     */
    public void setEpochInSeconds(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        epochInSeconds = flag;
    }
    
    /**
     * Get whether to convert numeric string data into an epoch using seconds instead of milliseconds.
     * 
     * @return true iff epoch conversion should be based on seconds
     */
    public boolean isEpochInSeconds() {
        return epochInSeconds;
    }
    
    /**
     * Set whether to ignore conversion failures.
     * 
     * @param flag flag to set
     */
    public void setIgnoreConversionErrors(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        ignoreConversionErrors = flag;
    }

   /** 
     * Get whether to ignore conversion failures.
     * 
     * @return whether to ignore conversion failures
     */
    public boolean isIgnoreConversionErrors() {
        return ignoreConversionErrors;
    }
    
    /**
     * Set a formatter to use to convert string data into an {@link Instant}.
     * 
     * @param f formatter
     */
    public void setDateTimeFormatter(@Nullable final DateTimeFormatter f) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        formatter = f;
    }
    
    /**
     * Get the formatter to use to convert string data into an {@link Instant}.
     * 
     * @return formatter
     */
    @Nullable public DateTimeFormatter getDateTimeFormatter() {
        return formatter;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (getDataConnectorDependencies().isEmpty() && getAttributeDependencies().isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + " no dependencies were configured");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        Constraint.isNotNull(workContext, "AttributeResolverWorkContext cannot be null");

        final IdPAttribute result = new IdPAttribute(getId());
        
        final List<IdPAttributeValue> values = PluginDependencySupport.getMergedAttributeValues(workContext,
                getAttributeDependencies(), 
                getDataConnectorDependencies(), 
                getId());

        final List<IdPAttributeValue> converted = values.stream()
                .map(v -> convert(v))
                .filter(Predicates.notNull())
                .collect(Collectors.toUnmodifiableList());
        
        if (!ignoreConversionErrors && converted.size() != values.size()) {
            throw new ResolutionException("Unable to convert all inputs to date/time values.");
        }

        result.setValues(converted);
        return result;
    }
    
    /**
     * Convert an input value into a {@link DateTimeAttributeValue} if possible.
     * 
     * @param input input value
     * 
     * @return converted value or null
     */
    @Nullable protected DateTimeAttributeValue convert(@Nonnull final IdPAttributeValue input) {
        
        if (input instanceof DateTimeAttributeValue) {
            return (DateTimeAttributeValue) input;
        } else if (!(input instanceof StringAttributeValue)) {
            log.info("{} Ignoring unsupported IdPAttributeValue type: {}", getLogPrefix(), input.getClass().getName());
            return null;
        }

        final String stringValue = ((StringAttributeValue) input).getValue();
        
        try {
            final Long longValue = Long.valueOf(stringValue);
            return new DateTimeAttributeValue(
                    epochInSeconds ? Instant.ofEpochSecond(longValue) : Instant.ofEpochMilli(longValue));
        } catch (final DateTimeException e) {
            log.info("{} Epoch value was out of range", getLogPrefix(), e);
            return null;
        } catch (final NumberFormatException e) {
            // Nothing to do, we'll just try and convert via formatter.
        }
        
        if (formatter == null) {
            log.info("{} No DateTimeFormatter installed, unable to convert string value", getLogPrefix());
            return null;
        }
        
        try {
            return new DateTimeAttributeValue(formatter.parse(stringValue, Instant::from));
        } catch (final DateTimeException e) {
            log.info("{} Error converting input value '{}' into Instant", getLogPrefix(), stringValue, e);
            return null;
        }
    }
  
}