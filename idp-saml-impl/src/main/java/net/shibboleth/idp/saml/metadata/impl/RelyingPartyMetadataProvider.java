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

package net.shibboleth.idp.saml.metadata.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * This class exists primarily to allow the parsing of relying-party.xml to create a distinct implementation of
 * {@link MetadataResolver}. This will allow a service to look for this top level element.
 * 
 */
public class RelyingPartyMetadataProvider extends AbstractIdentifiedInitializableComponent implements MetadataResolver {

    /** The embedded resolver. */
    private final MetadataResolver resolver;

    /**
     * Constructor.
     * 
     * @param child The {@link MetadataResolver} to embed.
     */
    public RelyingPartyMetadataProvider(MetadataResolver child) {
        setId(child.getId());
        resolver = child;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Iterable<EntityDescriptor> resolve(CriteriaSet criteria) throws ResolverException {

        return resolver.resolve(criteria);
    }

    /** {@inheritDoc} */
    @Override @Nullable public EntityDescriptor resolveSingle(CriteriaSet criteria) throws ResolverException {

        return resolver.resolveSingle(criteria);
    }

    /** {@inheritDoc} */
    @Override public boolean isRequireValidMetadata() {
        return resolver.isRequireValidMetadata();
    }

    /** {@inheritDoc} */
    @Override public void setRequireValidMetadata(boolean requireValidMetadata) {
        resolver.setRequireValidMetadata(requireValidMetadata);

    }

    /** {@inheritDoc} */
    @Override public MetadataFilter getMetadataFilter() {
        return resolver.getMetadataFilter();
    }

    /** {@inheritDoc} */
    @Override public void setMetadataFilter(MetadataFilter newFilter) {
        resolver.setMetadataFilter(newFilter);
    }
}
