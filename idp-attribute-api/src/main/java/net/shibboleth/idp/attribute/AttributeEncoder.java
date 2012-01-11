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

package net.shibboleth.idp.attribute;

import javax.annotation.Nonnull;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Attribute encoders convert {@link Attribute}s into protocol specific representations.
 * 
 * Encoders MUST be thread-safe and stateless.
 * 
 * @param <EncodedType> the type of object created by encoding the attribute
 */
@ThreadSafe
public interface AttributeEncoder<EncodedType> {

    /**
     * Gets the identifier of the protocol targeted by this encoder. Note, some protocols may have different types of
     * encoders that are used to encode attributes in to different parts of the protocol message. This identifier should
     * not be used to distinguish between the different message structure, it should only identify the protocol itself.
     * 
     * @return identifier of the protocol targeted by this encounter
     */
    @Nonnull @NotEmpty public String getProtocol();

    /**
     * Encodes the attribute into a protocol specific representations.
     * 
     * @param attribute the attribute to encode
     * 
     * @return the Object the attribute was encoded into
     * 
     * @throws AttributeEncodingException if unable to successfully encode attribute
     */
    @Nonnull public EncodedType encode(@Nonnull final Attribute<?> attribute) throws AttributeEncodingException;
}