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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.w3c.dom.Element;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/** Utility class for parsing v2 cache configuration. */
public class CacheConfigParser {

    /** Base XML element. */
    private final Element configElement;

    /**
     * Creates a new cache config parser with the supplied ResultsCache element.
     * 
     * @param config LDAPDirectory element
     */
    public CacheConfigParser(@Nonnull final Element config) {
        Constraint.isNotNull(config, "Element cannot be null");
        configElement = config;
    }

    /**
     * Creates a new cache from a v2 XML configuration.
     * 
     * @return cache
     */
    @Nullable public Cache<String, Map<String, IdPAttribute>> createCache() {
        final Element cacheElement =
                ElementSupport.getFirstChildElement(configElement, new QName(
                        DataConnectorNamespaceHandler.NAMESPACE, "ResultCache"));
        if (cacheElement == null) {
            final Boolean cacheResults =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement,
                            new QName("cacheResults")));
            if (cacheResults != null && cacheResults.booleanValue()) {
                return CacheBuilder.newBuilder().maximumSize(500).expireAfterAccess(4 * 60 * 60, TimeUnit.SECONDS)
                        .build();
            } else {
                return null;
            }
        }

        final Long timeToLive =
                AttributeSupport.getDurationAttributeValueAsLong(AttributeSupport.getAttribute(cacheElement,
                        new QName("elementTimeToLive")));
        final String maximumSize =
                AttributeSupport.getAttributeValue(cacheElement, new QName("maximumCachedElements"));
        return CacheBuilder.newBuilder().maximumSize(Long.parseLong(maximumSize))
                .expireAfterAccess(timeToLive, TimeUnit.MILLISECONDS).build();
    }
}
