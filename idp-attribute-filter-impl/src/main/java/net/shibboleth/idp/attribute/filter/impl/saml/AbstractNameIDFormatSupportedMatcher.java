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

package net.shibboleth.idp.attribute.filter.impl.saml;

import java.util.List;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractComparisonMatcher;

import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * Base class for matchers that check whether a particular entity attribute is present and contains a given
 * value.
 */
public abstract class AbstractNameIDFormatSupportedMatcher extends AbstractComparisonMatcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractNameIDFormatSupportedMatcher.class);

    /** The NameID format that needs to be supported by the entity. */
    private String nameIdFormat;

    /** Constructor. */
    public AbstractNameIDFormatSupportedMatcher() {
        setPolicyPredicate(new Predicate<AttributeFilterContext>() {

            public boolean apply(@Nullable AttributeFilterContext input) {
                return isNameIDFormatSupported(input);
            }
        });
    }

    /**
     * Get the NameID format that needs to be supported by the entity.
     * 
     * @return NameID format that needs to be supported by the entity
     */
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the NameID format that needs to be supported by the entity.
     * 
     * @param format NameID format that needs to be supported by the entity
     */
    public void setNameIdFormat(String format) {
        nameIdFormat = format;
    }

    /**
     * Checks to see if the metadata for the entity supports the required NameID format.
     * 
     * @param filterContext current filter context
     * 
     * @return true if the entity supports the required NameID format, false otherwise
     */
    protected boolean isNameIDFormatSupported(AttributeFilterContext filterContext) {
        SSODescriptor role = getEntitySSODescriptor(filterContext);
        if (role == null) {
            log.debug("entity does contain an appropriate SSO role descriptor");
            return false;
        }

        List<NameIDFormat> supportedFormats = role.getNameIDFormats();
        if (supportedFormats == null || supportedFormats.isEmpty()) {
            log.debug("entity SSO role descriptor does not list any supported NameID formats");
            return false;
        }

        for (NameIDFormat supportedFormat : supportedFormats) {
            if (nameIdFormat.equals(supportedFormat.getFormat())) {
                log.debug("entity does support the NameID format '{}'", nameIdFormat);
                return true;
            }
        }

        log.debug("entity does not support the NameID format '{}'", nameIdFormat);
        return false;
    }

    /**
     * Gets the SSO role descriptor for the entity to be checked.
     * 
     * @param filterContext current filtering context
     * 
     * @return the SSO role descriptor of the entity or null if the entity does not have such a descriptor
     */
    protected abstract SSODescriptor getEntitySSODescriptor(AttributeFilterContext filterContext);

}
