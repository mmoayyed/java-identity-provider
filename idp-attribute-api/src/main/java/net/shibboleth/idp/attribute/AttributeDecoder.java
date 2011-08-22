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

import java.util.Locale;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

/**
 * Decodes a data item in to an {@link Attribute}.
 * 
 * @param <DecodedType> the data type supported by this decoder
 */
@ThreadSafe
public interface AttributeDecoder<DecodedType> {

    /**
     * Gets the identifier of the protocol targeted by this decoder. Note, some protocols may have different types of
     * decoders that are used to decode information from different parts of the protocol message. This identifier SHOULD
     * NOT be used to distinguish between the different message structure, it should only identify the protocol itself.
     * 
     * @return identifier of the protocol targeted by this encounter, never null
     */
    public String getProtocol();

    /**
     * Gets the ID of the generated {@link Attribute}.
     * 
     * @return the ID of the generated IdP attribute
     */
    public String getId();
    
    /**
     * Gets the display names associated with the generated {@link Attribute}.
     * 
     * @return display names associated with the generated IdP attribute
     */
    public Map<Locale, String> getDisplayNames();
    
    /**
     * Gets the display descriptions associated with the generated {@link Attribute}.
     * 
     * @return display descriptions associated with the generated IdP attribute
     */
    public Map<Locale, String> getDisplayDescriptions();
    
    /**
     * Decodes a given piece of data into an {@link Attribute}.
     * 
     * @param data the data to be decoded, may be null
     * 
     * @return the attribute containing the decoded information, may be null
     * 
     * @throws AttributeDecodingException thrown if there was a problem decoding the given information
     */
    public Attribute decode(DecodedType data) throws AttributeDecodingException;
}