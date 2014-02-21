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

package net.shibboleth.idp.saml.impl.nameid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.saml.saml1.core.NameIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to look inside a {@link NameIdentifier} check that its {@link NameIdentifier#getNameQualifier()} are correct
 * and the decode the {@link NameIdentifier#getNameIdentifier()} with help of the base class (reversing the work done by
 * {@link net.shibboleth.idp.attribute.resolver.impl.ad.CryptoTransientIdAttributeDefinition}).
 */
public class CryptoTransientNameIdentifierDecoder extends BaseCryptoTransientDecoder implements NameIdentifierDecoder {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(CryptoTransientNameIdentifierDecoder.class);

    /** {@inheritDoc} */
    @Override @Nonnull public String decode(@Nonnull NameIdentifier nameIdentifier, @Nullable String responderId,
            @Nullable String requesterId) throws SubjectCanonicalizationException, NameDecoderException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final String nameQualifier = nameIdentifier.getNameQualifier();

        if (null != nameQualifier && null != responderId && !nameQualifier.equals(responderId)) {
            log.debug("NameQualifier '{}' does not match responderId'{}'", nameQualifier, requesterId);
            throw new SubjectCanonicalizationException("NameQualifier does not match responderId");
        }

        return super.decode(nameIdentifier.getNameIdentifier(), responderId, requesterId);
    }

}
