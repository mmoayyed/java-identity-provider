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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.policyrule.impl.AbstractPolicyRule;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2mdrpi.RegistrationInfo;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Base class for rules operating on the RPI extension in metadata.
 */
public abstract class AbstractRegistrationAuthorityPolicyRule extends AbstractPolicyRule {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractRegistrationAuthorityPolicyRule.class);

    /** The registrars to match against. */
    @Nonnull @NonnullElements private Set<String> registrars;

    /** What to say if no MDRPI is present. */
    private boolean matchIfMetadataSilent;

    /**
     * Get the candidate registrars.
     * 
     * @return the issuers
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Set<String> getRegistrars() {
        return ImmutableSet.copyOf(registrars);
    }

    /**
     * Set the candidate registrars.
     * 
     * @param theIssuers candidate registrars
     */
    public void setRegistrars(@Nonnull @NonnullElements final Collection<String> theIssuers) {
        Constraint.isNotNull(theIssuers, "Registrar collection cannot be null");
        
        registrars = new LinkedHashSet<>(theIssuers.size());
        for (final String s : theIssuers) {
            final String trimmed = StringSupport.trimOrNull(s);
            if (trimmed != null) {
                registrars.add(trimmed);
            }
        }
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
    public void setMatchIfMetadataSilent(final boolean value) {
        matchIfMetadataSilent = value;
    }

    /**
     * Gets the entity descriptor for the rule to check.
     * 
     * @param filterContext current filter request context
     * 
     * @return entity descriptor for the entity to check or null if not found
     */
    @Nullable protected abstract EntityDescriptor getEntityMetadata(
            @Nonnull final AttributeFilterContext filterContext);
    
    /**
     * Look for the {@link RegistrationInfo} inside the peer's entity description.
     * 
     * @param filterContext the context of the operation
     * @return The registration info for the SP in the context
     */
    @Nullable private RegistrationInfo getRegistrationInfo(@Nonnull final AttributeFilterContext filterContext) {

        final EntityDescriptor spEntity = getEntityMetadata(filterContext);
        if (null == spEntity) {
            log.debug("{} Filtering on registration, but no peer metadata available", getLogPrefix());
            return null;
        }

        Extensions extensions = spEntity.getExtensions();
        if (null != extensions) {
            for (final XMLObject object : extensions.getUnknownXMLObjects(RegistrationInfo.DEFAULT_ELEMENT_NAME)) {
                if (object instanceof RegistrationInfo) {
                    return (RegistrationInfo) object;
                }
            }
        }
        
        EntitiesDescriptor group = (EntitiesDescriptor) spEntity.getParent();
        while (null != group) {
            extensions = group.getExtensions();
            if (null != extensions) {
                for (final XMLObject object : extensions.getUnknownXMLObjects(RegistrationInfo.DEFAULT_ELEMENT_NAME)) {
                    if (object instanceof RegistrationInfo) {
                        return (RegistrationInfo) object;
                    }
                }
            }
            group = (EntitiesDescriptor) group.getParent();
        }

        log.debug("{} Filtering on registration, but no RegistrationInfo available", getLogPrefix());
        return null;
    }

    /** {@inheritDoc} */
    @Override public Tristate matches(@Nonnull final AttributeFilterContext filterContext) {
        final RegistrationInfo info = getRegistrationInfo(filterContext);

        if (info == null) {
            log.debug("{} The peer's metadata did not contain a RegistrationInfo descriptor", getLogPrefix());
            if (matchIfMetadataSilent) {
                return Tristate.TRUE;
            }
            return Tristate.FALSE;
        }

        final String authority = info.getRegistrationAuthority();
        log.debug("{} Peer's metadata has registration authority: {}", getLogPrefix(), authority);
        if (registrars.contains(authority)) {
            log.debug("{} Peer's metadata registration authority matches", getLogPrefix());
            return Tristate.TRUE;
        }
        log.debug("{} Peer's metadata registration authority does not match", getLogPrefix());
        return Tristate.FALSE;
    }

}