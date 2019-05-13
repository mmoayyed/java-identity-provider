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
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

/**
 * Transcoders are objects that support both attribute encoding and decoding for bidirectional
 * translation between {@link IdPAttribute} format and technology-specific formats.
 * 
 * <p>Implementations must take into account values of multiple types. An implementation encountering
 * a value type it does not understand may either decide to ignore it or throw exceptions.</p>
 * 
 * <p>Transcoders implement a {@link Predicate} interface to determine their applicability to a request.</p>
 * 
 * <p>Transcoders <strong>MUST</strong> be thread-safe and stateless.</p>
 * 
 * @param <T> the type of object supported
 */
@ThreadSafe
public interface AttributeTranscoder<T> extends InitializableComponent {

    /**
     * Get the class representing the type of object supported by this transcoder.
     * 
     * @return object type supported
     */
    @Nonnull Class<T> getEncodedType();
    
    /**
     * Get the name of the encoded object that would be created by a given set of
     * instructions.
     * 
     * @param rule properties governing the encoding process
     * 
     * @return a canonical name for objects produced by this transcoder for the
     *  given instructions
     */
    @Nullable @NotEmpty String getEncodedName(@Nonnull final TranscodingRule rule);
    
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
    @Nullable T encode(@Nullable final ProfileRequestContext profileRequestContext,
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
    @Nullable IdPAttribute decode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final T input, @Nonnull final TranscodingRule rule) throws AttributeDecodingException;
    
}