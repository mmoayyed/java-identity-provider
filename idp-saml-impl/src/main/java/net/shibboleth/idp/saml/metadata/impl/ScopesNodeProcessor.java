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

import java.util.HashSet;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;

import net.shibboleth.idp.saml.metadata.ScopesContainer;
import net.shibboleth.idp.saml.xmlobject.Scope;

/**
 * An implementation of {@link MetadataNodeProcessor} which extracts {@link Scope}s from any
 * {@link AttributeConsumingService} or {@link EntityDescriptor}.  They are accumulated and stored
 * back in as a {@link ScopesContainer}.
 */
@NotThreadSafe
public class ScopesNodeProcessor implements MetadataNodeProcessor {
  
    /** {@inheritDoc} */
    @Override public void process(final XMLObject metadataNode) throws FilterException {
        
        final Extensions extensions;
        if (metadataNode instanceof EntityDescriptor) {
           extensions = ((EntityDescriptor) metadataNode).getExtensions();
        } else if  (metadataNode instanceof RoleDescriptor) {
            extensions = ((RoleDescriptor) metadataNode).getExtensions();
        } else {
            return;
        }
        if (extensions == null) {
            return;
        }

        final List<XMLObject> scopes = extensions.getUnknownXMLObjects(Scope.DEFAULT_ELEMENT_NAME);
        if (scopes.isEmpty()) {
            return;
        }

        final HashSet<String> nonRegexScopes = new HashSet<>(scopes.size());
        final HashSet<String> regexScopes = new HashSet<>(scopes.size());
        for (final XMLObject object: scopes) {
            final Scope scope = (Scope) object;
            if (scope.getRegexp() != null && scope.getRegexp().booleanValue()) {
                regexScopes.add(scope.getValue());
            } else {
                nonRegexScopes.add(scope.getValue());
            }
        }

        final ScopesContainer container = new ScopesContainer();
        container.setRegexpScopes(regexScopes);
        container.setSimpleScopes(nonRegexScopes);
        metadataNode.getObjectMetadata().put(container);
    }

}