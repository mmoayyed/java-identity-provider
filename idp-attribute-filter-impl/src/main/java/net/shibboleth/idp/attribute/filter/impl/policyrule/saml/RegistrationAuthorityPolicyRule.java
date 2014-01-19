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

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.policyrule.AbstractPolicyRule;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.ext.saml2mdrpi.RegistrationInfo;
import org.opensaml.saml.saml2.common.Extensions;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter filters on mdrpi in the SP's metadata.
 */
public class RegistrationAuthorityPolicyRule extends AbstractPolicyRule {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RegistrationAuthorityPolicyRule.class);

    /** The issuers to match against. */
    private String[] issuers = new String[0];

    /** What to say if no MDRPI is present. */
    private boolean matchIfMetadataSilent;

    /**
     * get the issuers.
     * 
     * @return Returns the issuers.
     */
    public String[] getIssuers() {
        return issuers;
    }

    /**
     * Set the issuers.
     * 
     * @param theIssuers The issuers to set.
     */
    public void setIssuers(String[] theIssuers) {
        issuers = theIssuers;
    }

    /**
     * Get what to do if there is no mdrpi/extensions.
     * 
     * @return Returns the matchIfMetadataSilent.
     */
    public boolean isMatchIfMetadataSilent() {
        return matchIfMetadataSilent;
    }

    /**
     * Set what to do if there is no mdrpi/extensions.
     * 
     * @param value The matchIfMetadataSilent to set.
     */
    public void setMatchIfMetadataSilent(boolean value) {
        matchIfMetadataSilent = value;
    }

    /**
     * Look for the {@link RegistrationInfo} inside the peer's entity description.
     * 
     * @param filterContext the context of the operation
     * @return The registration info for the SP in the context
     */
    private RegistrationInfo getRegistrationInfo(AttributeFilterContext filterContext) {

        final SAMLMetadataContext metadataContext = filterContext.getRequesterMetadataContext();
        if (null == metadataContext) {
            log.info("{} Filtering on registration, but no metadata context available", getLogPrefix());
            return null;
        }

        final EntityDescriptor spEntity = metadataContext.getEntityDescriptor();
        if (null == spEntity) {
            log.info("Filtering on registration, but no peer metadata available");
            return null;
        }

        final Extensions extensions = spEntity.getExtensions();
        if (null == extensions) {
            log.info("Filtering on registration, but no peer extensions available");
            return null;
        }

        for (XMLObject object : extensions.getUnknownXMLObjects()) {
            if (object instanceof RegistrationInfo) {
                return (RegistrationInfo) object;
            }
        }
        log.info("Filtering on registration, but RegistrationInfo available");
        return null;
    }

    /** {@inheritDoc} */
    @Override public Tristate matches(@Nonnull AttributeFilterContext filterContext) {
        final RegistrationInfo info = getRegistrationInfo(filterContext);

        if (info == null) {
            log.debug("The peer's metadata did not contain a RegistrationInfo descriptor");
            if (matchIfMetadataSilent) {
                return Tristate.TRUE;
            } else {
                return Tristate.FALSE;
            }
        }

        final String authority = info.getRegistrationAuthority();
        log.debug("Peer's metadata has registration authority: {}", authority);
        for (String issuer : issuers) {
            if (issuer.matches(authority)) {
                log.debug("Peer's metadata registration authority matches");
                return Tristate.TRUE;
            }
        }
        log.debug("Peer's metadata registration authority does not match");
        return Tristate.FALSE;
    }

}
