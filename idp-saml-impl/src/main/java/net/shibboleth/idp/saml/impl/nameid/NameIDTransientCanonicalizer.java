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

import net.shibboleth.idp.saml.nameid.NameCanonicalizationException;
import net.shibboleth.idp.saml.nameid.NameCanonicalizer;

import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Canonicalizer for NameID which were generated as by the TransientIdAttributeDefinition.
 */
public class NameIDTransientCanonicalizer extends AbstractTransientCanonicalizer implements NameCanonicalizer<NameID> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(NameIDTransientCanonicalizer.class);

    /**
     * {@inheritDoc}. We only deal with {@link NameID} and requesters that match.
     */
    @Override public boolean isApplicable(SAMLObject identifier, String requestedId) {
        if (!isSpApplicable(requestedId)) {
            return false;
        }

        if (!(identifier instanceof NameID)) {
            return false;
        }

        final NameID nameID = (NameID) identifier;

        return isFormatApplicable(nameID.getFormat());
    }

    /** {@inheritDoc} */
    @Override public String canonicalize(NameID identifier, String requesterId, String responderId)
            throws NameCanonicalizationException {
        return decode(identifier.getValue(), requesterId);
    }
}
