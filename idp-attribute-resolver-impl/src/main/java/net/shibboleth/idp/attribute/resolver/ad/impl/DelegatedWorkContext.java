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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolvedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolvedDataConnector;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import org.opensaml.messaging.context.BaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A delegate for {@link AttributeResolverWorkContext}.
 * 
 * <p>This is only here because in a moment of madness we pushed the work context into
 * the scripted definitions. This preserves the function while making it obvious that
 * we don't want people to use it.</p>
 *   
 * @deprecated
 */
@Deprecated public class DelegatedWorkContext extends BaseContext {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DelegatedWorkContext.class);

    /** The delegate. */
    @Nonnull private final AttributeResolverWorkContext delegate;

    /** The log prefix. */
    @Nullable private final String logPrefix;

    /**
     * Constructor.
     * 
     * @param parent the value to be delegated.
     * @param prefix the log prefix of the calling scripted definition.
     */
    public DelegatedWorkContext(@Nonnull final AttributeResolverWorkContext parent, @Nullable final String prefix) {
        delegate = parent;
        setParent(delegate.getParent());
        setAutoCreateSubcontexts(parent.isAutoCreateSubcontexts());
        logPrefix = prefix;
    }

    /**
     * see {@link AttributeResolverWorkContext#getResolvedIdPAttributeDefinitions()}.
     * 
     * @return what the delegate does.
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, ResolvedAttributeDefinition>
            getResolvedIdPAttributeDefinitions() {
        DeprecationSupport.warnOnce(ObjectType.CLASS, AttributeResolverWorkContext.class.getName(), null, null);
        log.error("'{}' Use of workContext.getResolvedIdPAttributeDefinitions()"
                + " is deprecated and will cause instability", logPrefix);
        return delegate.getResolvedIdPAttributeDefinitions();
    }

    /**
     * see {@link AttributeResolverWorkContext#recordAttributeDefinitionResolution(AttributeDefinition,IdPAttribute)}.
     * 
     * @param definition as the delegate does
     * @param attribute as the delegate does
     * @throws ResolutionException as the delegate does
     */
    public void recordAttributeDefinitionResolution(@Nonnull final AttributeDefinition definition,
            @Nullable final IdPAttribute attribute) throws ResolutionException {
        DeprecationSupport.warnOnce(ObjectType.CLASS, AttributeResolverWorkContext.class.getName(), null, null);
        log.error("'{}' Use of workContext.getResolvedIdPAttributeDefinitions()"
                + " is deprecated and will cause instability", logPrefix);
        delegate.recordAttributeDefinitionResolution(definition, attribute);
    }

    /**
     * see {@link AttributeResolverWorkContext#getResolvedDataConnectors()}.
     * 
     * @return what the delegate does.
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, ResolvedDataConnector> getResolvedDataConnectors() {
        DeprecationSupport.warnOnce(ObjectType.CLASS, AttributeResolverWorkContext.class.getName(), null, null);
        log.error("'{}' Use of workContext.getResolvedDataConnectors() is deprecated and will cause instability",
                logPrefix);
        return delegate.getResolvedDataConnectors();
    }

    /**
     * see {@link AttributeResolverWorkContext#recordDataConnectorResolution(DataConnector,Map)}.
     * 
     * @param connector as the delegate does
     * @param attributes as the delegate does
     * @throws ResolutionException as the delegate does
     */
    public void recordDataConnectorResolution(@Nonnull final DataConnector connector,
            @Nullable final Map<String, IdPAttribute> attributes) throws ResolutionException {
        DeprecationSupport.warnOnce(ObjectType.CLASS, AttributeResolverWorkContext.class.getName(), null, null);
        log.error(
                "'{}' Use of workContext.recordDataConnectorResolution() is deprecated and will cause instability",
                logPrefix);
        delegate.recordDataConnectorResolution(connector, attributes);
    }

    /**
     * see {@link AttributeResolverWorkContext#recordFailoverResolution}.
     * 
     * @param failedConnector as the delegate does
     * @param failoverConnector as the delegate does
     * @throws ResolutionException as the delegate does
     */
    public void recordFailoverResolution(@Nonnull final DataConnector failedConnector,
            @Nonnull final DataConnector failoverConnector) throws ResolutionException {
        DeprecationSupport.warnOnce(ObjectType.CLASS, AttributeResolverWorkContext.class.getName(), null, null);
        log.error("'{}' Use of workContext.recordFailoverResolution() is deprecated and will cause instability",
                logPrefix);
        delegate.recordFailoverResolution(failedConnector, failoverConnector);

    }
    
}