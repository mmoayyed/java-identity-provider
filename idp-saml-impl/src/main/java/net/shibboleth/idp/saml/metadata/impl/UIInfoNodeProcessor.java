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

import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;

import net.shibboleth.idp.saml.metadata.IdPUIInfo;

/**
 * An implementation of {@link MetadataNodeProcessor} which processes any {@link UIInfo}s from any
 * and processes them into an {@link IdPUIInfo}.
 */
@NotThreadSafe
public class UIInfoNodeProcessor implements MetadataNodeProcessor {
  
    /** {@inheritDoc} */
    @Override public void process(final XMLObject metadataNode) throws FilterException {
        
        if (metadataNode instanceof UIInfo) {
            metadataNode.getObjectMetadata().put(new IdPUIInfo((UIInfo) metadataNode));
        }
    }

}