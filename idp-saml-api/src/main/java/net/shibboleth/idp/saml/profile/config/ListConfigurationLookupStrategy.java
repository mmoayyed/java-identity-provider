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

package net.shibboleth.idp.saml.profile.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A strategy function that examines SAML metadata associated with a relying party and derives List<String>-valued
 * configuration settings based on EntityAttribute extension tags.
 * 
 * @param <T> type of object in list
 * 
 * @since 3.4.0
 */
public class ListConfigurationLookupStrategy<T> extends AbstractCollectionConfigurationLookupStrategy<T,List<T>> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ListConfigurationLookupStrategy.class);

    /** {@inheritDoc} */
    @Override
    @Nullable protected List<T> doTranslate(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final Attribute tag) {

        log.debug("Converting tag '{}' to List<{}> property", tag.getName(), getPropertyType().getSimpleName());
        
        final List<XMLObject> values = tag.getAttributeValues();
        final List<T> result = new ArrayList<>(values.size());
        for (final XMLObject value : values) {
            final String converted = xmlObjectToString(value);
            if (converted != null) {
                try {
                    result.add(createInstanceFromString(converted));
                } catch (final Exception e) {
                    log.error("Error converting tag value into {}", getPropertyType().getSimpleName(), e);
                }
            }
        }
        return result;
    }
    
}