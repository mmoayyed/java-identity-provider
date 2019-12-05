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

package net.shibboleth.idp.attribute.filter.policyrule.saml.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;

import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A matcher that evaluates to true if attribute issuer's metadata matches the provided entity group name,
 * or a valid metadata-sourced affiliation of entities.
 * 
 * @since 4.0.0
 */
public class AttributeIssuerInEntityGroupPolicyRule extends AbstractEntityGroupPolicyRule {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeIssuerInEntityGroupPolicyRule.class);

    /**
     * Gets the entity descriptor for the entity to check.
     * 
     * @param filterContext current filter request context
     * 
     * @return entity descriptor for the entity to check
     */
    @Nullable protected EntityDescriptor getEntityMetadata(@Nonnull final AttributeFilterContext filterContext) {
        final SAMLMetadataContext metadataContext = filterContext.getIssuerMetadataContext();

        if (null == metadataContext) {
            log.debug("{} No issuer metadata found", getLogPrefix());
            return null;
        }
        return metadataContext.getEntityDescriptor();
    }

}