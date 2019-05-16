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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.IdentifiedComponent;


/**
 * The transcoder registry provides access to "instructions" for converting between
 * the {@link IdPAttribute} "neutral" representation within the IdP and protocol-specific
 * forms such as SAML Attributes or OIDC claims.
 */
@ThreadSafe
public interface AttributeTranscoderRegistry extends IdentifiedComponent {
    
    /** Property name for accessing the name of the {@link IdPAttribute} to decode into. */
    @Nonnull @NotEmpty static final String PROP_ID = "id";

    /** Property name for accessing {@link AttributeTranscoder} object to use. */
    @Nonnull @NotEmpty static final String PROP_TRANSCODER = "transcoder";

    /** Property name for identifying an {@link AttributeTranscoder} class to build. */
    @Nonnull @NotEmpty static final String PROP_TRANSCODER_CLASS = "transcoderClass";
    
    /** Property name for accessing an activation condition object to apply. */
    @Nonnull @NotEmpty static final String PROP_CONDITION = "activationCondition";

    /** Property name for accessing relying parties to wrap an activation condition around. */
    @Nonnull @NotEmpty static final String PROP_RELYINGPARTIES = "relyingParties";
    
    /**
     * Obtains a set of instructions for encoding an input {@link IdPAttribute} into a target type.
     * 
     * <p>The principal property useful to a caller is {@link #PROP_TRANSCODER} to obtain an instance
     * of the appropriate {@link AttributeTranscoder} to call, passing in the properties to drive that
     * call.</p>
     * 
     * @param from the input object to encode
     * @param to class of object being encoded
     * 
     * @return a collection of {@link TranscodingRule} objects, possibly empty
     */
    @Nonnull @NonnullElements @Unmodifiable
    Collection<TranscodingRule> getTranscodingRules(@Nonnull final IdPAttribute from, @Nonnull final Class<?> to);

    /**
     * Obtains a set of instructions for decoding an input object into an {@link IdPAttribute}.
     * 
     * <p>The principal property useful to a caller is {@link #PROP_TRANSCODER} to obtain an instance
     * of the appropriate {@link AttributeTranscoder} to call, passing in the properties to drive that
     * call.</p>
     * 
     * @param <T> the type of object to decode
     * @param from object to decode
     * 
     * @return a collection of {@link TranscodingRule} objects, possibly empty
     */
    @Nonnull @NonnullElements @Unmodifiable <T> Collection<TranscodingRule> getTranscodingRules(
            @Nonnull final T from);
        
}