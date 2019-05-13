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

package net.shibboleth.idp.attribute.resolver.spring.enc.impl;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;

import net.shibboleth.idp.attribute.resolver.spring.enc.BaseSAML2AttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.saml.attribute.transcoding.impl.SAML2ByteAttributeTranscoder;

/**
 * Spring Bean Definition Parser for {@link SAML2ByteAttributeTranscoder}.
 */
public class SAML2Base64AttributeEncoderParser extends BaseSAML2AttributeEncoderParser {

    /** Schema type name.. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER = new QName(AttributeResolverNamespaceHandler.NAMESPACE, 
            "SAML2Base64");

    /** {@inheritDoc} */
    @Override
    protected BeanReference buildTranscoder() {
        return new RuntimeBeanReference("SAML2ByteTranscoder");
    }

}