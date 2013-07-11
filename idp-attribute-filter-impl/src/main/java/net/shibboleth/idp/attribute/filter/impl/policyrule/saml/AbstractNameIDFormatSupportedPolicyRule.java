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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.impl.policyrule.AbstractPolicyRule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for matching functions that check if an SAML entity supports a particular NameID format type.<br/>
 * 
 *  Classes wishing to implement NameId matchers implement {@link #getEntitySSODescriptor(AttributeFilterContext)}
 * to navigate to the SSO endpoint, this class does the rest.
 */
public abstract class AbstractNameIDFormatSupportedPolicyRule extends AbstractPolicyRule {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractNameIDFormatSupportedPolicyRule.class);

    /** The NameID format that needs to be supported by the entity. */
    private String nameIdFormat;

    /**
     * Get the NameID format that needs to be supported by the entity.
     * 
     * @return NameID format that needs to be supported by the entity
     */
    @NonnullAfterInit public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the NameID format that needs to be supported by the entity.
     * 
     * @param format NameID format that needs to be supported by the entity
     */
    public void setNameIdFormat(final String format) {
        nameIdFormat = StringSupport.trimOrNull(format);
    }

    /**
     * Checks to see if the metadata for the entity supports the required NameID format.
     * 
     * @param filterContext current filter context
     * 
     * @return true if the entity supports the required NameID format, false otherwise
     *         {@inheritDoc}
     */
    public Tristate matches(@Nonnull AttributeFilterContext filterContext) throws AttributeFilterException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final SSODescriptor role = getEntitySSODescriptor(filterContext);
        if (role == null) {
            log.debug("entity does contain an appropriate SSO role descriptor");
            return Tristate.FALSE;
        }

        final List<NameIDFormat> supportedFormats = role.getNameIDFormats();
        if (supportedFormats == null || supportedFormats.isEmpty()) {
            log.debug("entity SSO role descriptor does not list any supported NameID formats");
            return Tristate.FALSE;
        }

        for (NameIDFormat supportedFormat : supportedFormats) {
            if (nameIdFormat.equals(supportedFormat.getFormat())) {
                log.debug("entity does support the NameID format '{}'", nameIdFormat);
                return Tristate.TRUE;
            }
        }

        log.debug("entity does not support the NameID format '{}'", nameIdFormat);
        return Tristate.FALSE;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == nameIdFormat) {
            throw new ComponentInitializationException(getLogPrefix() + " No NameId format specified");
        }
    }

    /**
     * Gets the SSO role descriptor for the entity to be checked.
     * 
     * @param filterContext current filtering context
     * 
     * @return the SSO role descriptor of the entity or null if the entity does not have such a descriptor
     */
    @Nullable protected abstract SSODescriptor getEntitySSODescriptor(AttributeFilterContext filterContext);

}
