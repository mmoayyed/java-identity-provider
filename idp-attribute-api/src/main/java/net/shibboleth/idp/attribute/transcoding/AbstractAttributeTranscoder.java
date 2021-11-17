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

package net.shibboleth.idp.attribute.transcoding;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

/**
 * Base class for transcoders.
 * 
 * @param <T> type of object supported
 */
public abstract class AbstractAttributeTranscoder<T> extends AbstractInitializableComponent
        implements AttributeTranscoder<T> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractAttributeTranscoder.class);

    /** Condition for use of this transcoder. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;
    
    /** Constructor. */
    public AbstractAttributeTranscoder() {
        activationCondition = Predicates.alwaysTrue();
    }
    
    /**
     * Set an activation condition for this transcoder.
     * 
     * @param condition condition to set
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        activationCondition = Constraint.isNotNull(condition, "Activation condition cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public T encode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final Class<? extends T> to,
            @Nonnull final TranscodingRule rule) throws AttributeEncodingException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(attribute, "Attribute to encode cannot be null");

        if (!checkActivation(profileRequestContext, rule)) {
            return null;
        }

        return doEncode(profileRequestContext, attribute, to, rule);
    }
    
   
    /** {@inheritDoc} */
    @Nullable public IdPAttribute decode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final T input, @Nonnull final TranscodingRule rule) throws AttributeDecodingException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(input, "Attribute to decode cannot be null");

        if (!checkActivation(profileRequestContext, rule)) {
            return null;
        }
        
        final IdPAttribute attribute = doDecode(profileRequestContext, input, rule);
        return attribute;
    }
    
    
    /**
     * Encode the supplied attribute into a protocol specific representation.
     * 
     * @param profileRequestContext current profile request context
     * @param attribute the attribute to encode
     * @param to specific type of object to encode
     * @param rule properties governing the encoding process, principally the resulting object's naming
     * 
     * @return the Object the attribute was encoded into
     * 
     * @throws AttributeEncodingException if unable to successfully encode attribute
     */
    @Nullable protected abstract T doEncode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final Class<? extends T> to,
            @Nonnull final TranscodingRule rule) throws AttributeEncodingException;
    

    /**
     * Decode the supplied object into a protocol-neutral representation.
     * 
     * @param profileRequestContext current profile request context
     * @param input the object to decode
     * @param rule properties governing the decoding process, principally the resulting attribute's naming
     * 
     * @return the attribute the object was decoded into
     * 
     * @throws AttributeDecodingException if unable to successfully decode object
     */
    @Nullable protected abstract IdPAttribute doDecode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final T input, @Nonnull final TranscodingRule rule) throws AttributeDecodingException;


    /**
     * 
     * Apply any activation rules to the request.
     * 
     * @param profileRequestContext current profile request context
     * @param rule properties governing the transoding process
     * 
     * @return true iff the process should continue
     */
    private boolean checkActivation(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final TranscodingRule rule) {
        
        if (!activationCondition.test(profileRequestContext)) {
            log.debug("Transcoder inactive");
            return false;
        }

        final Predicate<ProfileRequestContext> condition =
                rule.get(AttributeTranscoderRegistry.PROP_CONDITION, Predicate.class);
        if (condition != null) {
            if (!condition.test(profileRequestContext)) {
                log.debug("Transcoder inactive");
                return false;
            }
        }
        
        return true;
    }

}