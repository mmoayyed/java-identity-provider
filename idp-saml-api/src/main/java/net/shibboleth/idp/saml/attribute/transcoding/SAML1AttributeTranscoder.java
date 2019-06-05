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

package net.shibboleth.idp.saml.attribute.transcoding;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeDesignator;

/**
 * Marker interface for transcoders that operate on a SAML 1 {@link Attribute} or {@link AttributeDesignator}.
 * 
 * @param <EncodedType> the type of data that can be handled by the transcoder
 */
public interface SAML1AttributeTranscoder<EncodedType extends IdPAttributeValue> extends
        AttributeTranscoder<AttributeDesignator> {

    /** The attribute name. */
    @Nonnull @NotEmpty static final String PROP_NAME = "saml1.name";

    /** Whether to encode the xsi:type. */
    @Nonnull @NotEmpty static final String PROP_ENCODE_TYPE = "saml1.encodeType";

    /** The namespace of the attribute name. */
    @Nonnull @NotEmpty static final String PROP_NAMESPACE = "saml1.namespace";

}