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

package net.shibboleth.idp.attribute.filter.matcher.saml.impl;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Class to implement a filter of string values against &lt;shibmd:scope&gt;. */
public class AttributeValueMatchesShibMDScope extends AbstractMatchesShibMDScopeMatcher {

    /** Class logger. */
    private static final Logger LOG = LoggerFactory.getLogger(AttributeValueMatchesShibMDScope.class);

    
    /** {@inheritDoc} */
    @Nullable @NotEmpty protected String getCompareString(final IdPAttributeValue value) {
        if (value instanceof ScopedStringAttributeValue) {
            LOG.warn("{} lossy test of AttributeValueMatchesShibMDScope against scoped Attribute value {}",
                    getLogPrefix(), value.getNativeValue());
        }
        if (value instanceof StringAttributeValue) {
            return StringSupport.trimOrNull(((StringAttributeValue)value).getValue());
        }

        LOG.warn("{} value of type {} and value {} not suitable for filtering",
                    getLogPrefix(), value.getClass(), value.getNativeValue());
        return null;
    }
}
