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
import net.shibboleth.idp.saml.nameid.NameIDDecoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a transient {@link NameID}, checks that its {@link NameID#getNameQualifier()} and
 * {@link NameID#getSPNameQualifier()} are correct, and decodes {@link NameID#getValue()} 
 *  via the base class (reversing the work done by
 * {@link net.shibboleth.idp.attribute.resolver.impl.ad.CryptoTransientIdAttributeDefinition}).
 */
public class CryptoTransientNameIDDecoder extends BaseCryptoTransientDecoder implements NameIDDecoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CryptoTransientNameIDDecoder.class);

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String decode(@Nonnull final NameID nameID, @Nullable final String responderId,
            @Nullable final String requesterId) throws SubjectCanonicalizationException, NameDecoderException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final String spNameQualifier = nameID.getSPNameQualifier();

        if (null != spNameQualifier && null != requesterId && !spNameQualifier.equals(requesterId)) {
            log.debug("{} SPNameQualifier '{}' does not match requesterId '{}'",
                    new Object[] {getLogPrefix(), spNameQualifier, requesterId,});
            throw new SubjectCanonicalizationException("SPNameQualifier does not match requesterId");
        }

        final String nameQualifier = nameID.getNameQualifier();

        if (null != nameQualifier && null != responderId && !nameQualifier.equals(responderId)) {
            log.debug("{} NameQualifier '{}' does not match responderId '{}'",
                    new Object[] {getLogPrefix(), nameQualifier, responderId,});
            throw new SubjectCanonicalizationException("NameQualifier does not match responderId");
        }

        return super.decode(nameID.getValue(), responderId, requesterId);
    }

}