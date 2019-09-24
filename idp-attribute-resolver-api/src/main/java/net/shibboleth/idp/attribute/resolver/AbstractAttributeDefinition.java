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

package net.shibboleth.idp.attribute.resolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/** Base class for attribute definition resolver plugins. */
@ThreadSafe
public abstract class AbstractAttributeDefinition extends AbstractResolverPlugin<IdPAttribute> implements
        AttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractAttributeDefinition.class);

    /** Whether this attribute definition is only a dependency and thus its values should never be released. */
    private boolean dependencyOnly;

    /** Whether this attribute definition is to be pre-resolved. */
    private boolean preRequested;

    /** cache for the log prefix - to save multiple recalculations. */
    @Nullable private String logPrefix;

    /**
     * Gets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @return true if this attribute is only used as a dependency, false otherwise
     */
    @Override
    public boolean isDependencyOnly() {
        return dependencyOnly;
    }

    /**
     * Sets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @param isDependencyOnly whether this attribute definition is only a dependency
     */
    public void setDependencyOnly(final boolean isDependencyOnly) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        dependencyOnly = isDependencyOnly;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPreRequested() {
        return preRequested;
    }

    /** Sets whether this definition (and its dependencies) are to be pre-resolved.
     * @param value what to set
     */
    public void setPreRequested(final boolean value) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        preRequested = value;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {

        // Set up the dependencies first. Then the initialize in the parent
        // will correctly rehash the dependencies.
        super.doInitialize();
        // The Id is now definitive. Just in case it was used prior to that, reset the getPrefixCache
        logPrefix = null;
        
        if (IdPAttribute.isInvalidId(getId())) {
            throw new ComponentInitializationException(
                    "Invalid Attribute Definitions name (" + getId() + ")");
        }
        if (IdPAttribute.isDeprecatedId(getId())) {
            DeprecationSupport.warnOnce(
                    ObjectType.CONFIGURATION,
                    "Use of Attributes definition with invalid characters",
                    getLogPrefix(),
                    null);
            log.debug("{} : Deprecated characters in Attribute Defintion id.", getLogPrefix());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This method delegates the actual resolution of the attribute's values to the
     * {@link #doAttributeDefinitionResolve(AttributeResolutionContext, AttributeResolverWorkContext)} method.
     * Afterwards, if null was not returned, this method will attach the registered display names, descriptions,
     * and encoders to the resultant attribute.
     */
    @Override
    @Nullable protected IdPAttribute doResolve(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        final IdPAttribute resolvedAttribute = doAttributeDefinitionResolve(resolutionContext, workContext);

        if (null == resolvedAttribute) {
            log.debug("{} no attribute was produced during resolution", getLogPrefix());
            return null;
        }

        if (resolvedAttribute.getValues().isEmpty()) {
            log.debug("{} produced an attribute with no values", getLogPrefix());
        } else {
            log.debug("{} produced an attribute with the following values {}", getLogPrefix(),
                    resolvedAttribute.getValues());
        }

        if (resolutionContext.getTranscoderRegistry() != null) {
            ServiceableComponent<AttributeTranscoderRegistry> component = null;
            try {
                component = resolutionContext.getTranscoderRegistry().getServiceableComponent();
                if (component != null) {
                    
                    if (resolvedAttribute.getDisplayNames().isEmpty()) {
                        resolvedAttribute.setDisplayNames(
                                component.getComponent().getDisplayNames(resolvedAttribute));
                        log.trace("{} associated display names with the resolved attribute: {}", getLogPrefix(),
                                resolvedAttribute.getDisplayNames());
                    }

                    if (resolvedAttribute.getDisplayDescriptions().isEmpty()) {
                        resolvedAttribute.setDisplayDescriptions(
                                component.getComponent().getDescriptions(resolvedAttribute));
                        log.trace("{} associated descriptions with the resolved attribute: {}", getLogPrefix(),
                                resolvedAttribute.getDisplayDescriptions());
                    }

                } else {
                    log.warn("No transcoder registry available, unable to attach displayName/description metadata");
                }
            } finally {
                if (component != null) {
                    component.unpinComponent();
                }
            }
        } else {
            log.debug("No transcoder registry supplied, unable to attach displayName/description metadata");
        }

        return resolvedAttribute;
    }

    /**
     * Creates and populates the values for the resolved attribute. Implementations should <strong>not</strong> set, or
     * otherwise manage, the resolved attribute's display name, description or encoders. Nor should the resultant
     * attribute be recorded in the given resolution context.
     * 
     * @param resolutionContext current attribute resolution context
     * @param workContext current resolver work context
     * 
     * @return resolved attribute or null if nothing to resolve.
     * @throws ResolutionException thrown if there is a problem resolving and creating the attribute
     */
    @Nullable protected abstract IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException;

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Definition '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            final StringBuilder builder = new StringBuilder("Attribute Definition '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }
    
}