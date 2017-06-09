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

package net.shibboleth.idp.attribute.resolver.spring.dc.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/** Utility class for parsing v2 cache configuration. */
public class CacheConfigParser {

    /** ResultCache name - dc: (Legacy). */
    @Nonnull public static final QName RESULT_CACHE_DC =
            new QName(DataConnectorNamespaceHandler.NAMESPACE, "ResultCache");

    /** ResultCacheBean name - dc: (Legacy). */
    @Nonnull public static final QName RESULT_CACHE_BEAN_DC =
            new QName(DataConnectorNamespaceHandler.NAMESPACE, "ResultCacheBean");

    /** ResultCache name - resolver:. */
    @Nonnull public static final QName RESULT_CACHE_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ResultCache");
    
    /** ResultCacheBean name - resolver:. */
    @Nonnull public static final QName RESULT_CACHE_BEAN_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ResultCacheBean");

    /** Documented maximumCachedElements maximum (500).  Unfortunately it has to be here since
     * we do not own the implemented class */
    private static final long DEFAULT_CACHE_ENTRIES = 500;

    /** Documented cache lifetime (4 hours).  Unfortunately it has to be here since
     * we do not own the implemented class */
    private static final long DEFAULT_TTL_MS = 4 * 60 * 60 * 1000;
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CacheConfigParser.class);

    /** Base XML element. */
    @Nonnull private final Element configElement;

    /**
     * Creates a new cache config parser with the supplied ResultsCache element.
     * 
     * @param config LDAPDirectory element
     */
    public CacheConfigParser(@Nonnull final Element config) {
        Constraint.isNotNull(config, "Element cannot be null");
        configElement = config;
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Creates a new cache bean definition from a v2 XML configuration.
     * 
     * @param parserContext bean parser context
     * 
     * @return cache bean definition
     */
    @Nonnull public BeanDefinition createCache(@Nonnull final ParserContext parserContext) {

        final String defaultCache = AttributeSupport.getAttributeValue(configElement, new QName("cacheResults"));
        if (defaultCache != null) {
            DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, "cacheResults",
                    parserContext != null ? parserContext.getReaderContext().getResource().getDescription() : null,
                            RESULT_CACHE_RESOLVER.toString());
            return null;
        }
        
        final List<Element> cacheElements = ElementSupport.getChildElements(configElement, RESULT_CACHE_DC);
        cacheElements.addAll(ElementSupport.getChildElements(configElement, RESULT_CACHE_RESOLVER));
        if (cacheElements.isEmpty()) {
            return null;
        }
        
        if (cacheElements.size() > 1) {
            log.warn("Only one <ResultCache> element can be specified, the first one has been taken");
        }
        
        final Element cacheElement = cacheElements.get(0);
        final String elementTimeToLive =
                AttributeSupport.getAttributeValue(cacheElement, new QName("elementTimeToLive"));
        if (null != elementTimeToLive) {
            DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, "elementTimeToLive",
                    parserContext != null ? parserContext.getReaderContext().getResource().getDescription() : null,
                            "expireAfterAccess");
        }
        final String expireAfterWrite =
                AttributeSupport.getAttributeValue(cacheElement, new QName("expireAfterWrite"));
        final String expireAfterAccess =
                AttributeSupport.getAttributeValue(cacheElement, new QName("expireAfterAccess"));
        
        final BeanDefinitionBuilder cache;
        if (expireAfterWrite != null) {
            if (null != expireAfterAccess || null != elementTimeToLive) {
                log.warn("ResultCache: Attribute 'expireAfterAccess' is mutually exclusive with 'expireAfterWrite'."
                        + " Used 'expireAfterWrite'.");
            }
            cache = BeanDefinitionBuilder.rootBeanDefinition(CacheConfigParser.class, "buildCacheWrite");            
            cache.addConstructorArgValue(expireAfterWrite);            
        } else if (elementTimeToLive != null) {
            cache = BeanDefinitionBuilder.rootBeanDefinition(CacheConfigParser.class, "buildCacheAccess");            
            cache.addConstructorArgValue(elementTimeToLive);
        } else {
            cache = BeanDefinitionBuilder.rootBeanDefinition(CacheConfigParser.class, "buildCacheAccess");            
            cache.addConstructorArgValue(expireAfterAccess);
        } 
        cache.addConstructorArgValue(
                AttributeSupport.getAttributeValue(cacheElement, new QName("maximumCachedElements")));
        return cache.getBeanDefinition();
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** Helper function to return size provided with a suitable default.
     * 
     * @param maximumSize long string
     * @return the input as a long, or DEFAULT_CACHE_ENTRIES
     */
    private static long getMaxSize(@Nullable final String maximumSize) {
        if (maximumSize != null) {
            return Long.parseLong(maximumSize);
        }  else {
            return  DEFAULT_CACHE_ENTRIES;   
        }
    }
    
    /** Helper function to return the TTL with a suitable default.
     * @param timeToLive duration string
     * @return the input as a long, or DEFAULT_TTL_MS
     */
    private static long getTimeToLiveMs(@Nullable final String timeToLive) {
        if (timeToLive != null) {
            return DOMTypeSupport.durationToLong(timeToLive);
        }  else {
            return  DEFAULT_TTL_MS;   
        }
    }
    
    /**
     * Factory method to leverage spring property replacement functionality. The default settings are a max size
     * of {@link #DEFAULT_CACHE_ENTRIES} and an expiration time {@link #DEFAULT_TTL_MS}.
     * 
     * The Cache is set to reset the timer on Access
     * 
     * @param timeToLive duration string
     * @param maximumSize long string
     * 
     * @return cache
     */
    @Nullable public static Cache<String, Map<String, IdPAttribute>> buildCacheAccess(@Nullable final String timeToLive,
            @Nullable final String maximumSize) {
        
        return CacheBuilder.newBuilder()
                    .maximumSize(getMaxSize(maximumSize))
                    .expireAfterAccess(getTimeToLiveMs(timeToLive), TimeUnit.MILLISECONDS)
                    .build();
    }
    
    /**
     * Factory method to leverage spring property replacement functionality. The default settings are a max size
     * of {@link #DEFAULT_CACHE_ENTRIES} and an expiration time {@link #DEFAULT_TTL_MS}.
     * 
     * The Cache is set to set the timer on Populate
     * 
     * @param timeToLive duration string
     * @param maximumSize long string
     * 
     * @return cache
     */
    @Nullable public static Cache<String, Map<String, IdPAttribute>> buildCacheWrite(@Nullable final String timeToLive,
            @Nullable final String maximumSize) {
        
        return CacheBuilder.newBuilder()
                    .maximumSize(getMaxSize(maximumSize))
                    .expireAfterWrite(getTimeToLiveMs(timeToLive), TimeUnit.MILLISECONDS)
                    .build();
    }
    
    /**
     * Get the bean ID of an externally defined result cache.
     * 
     * @param config the config element
     * @return data source bean ID
     */
    @Nullable public static String getBeanResultCacheID(@Nonnull final Element config) {
        
        final List<Element> beanResultCache = ElementSupport.getChildElements(config, RESULT_CACHE_BEAN_DC);
        beanResultCache.addAll(ElementSupport.getChildElements(config, RESULT_CACHE_BEAN_RESOLVER)); 
        
        if (beanResultCache.isEmpty()) {
            return null;
        }
        
        if (beanResultCache.size() > 1) {
            LoggerFactory.getLogger(ManagedConnectionParser.class).
            warn("Only one <ResultCacheBean> should be specified; the first one has been consulted");
        }

        final List<Element> resultCacheElements = ElementSupport.getChildElements(config, RESULT_CACHE_DC);
        resultCacheElements.addAll(ElementSupport.getChildElements(config, RESULT_CACHE_RESOLVER));
        
        if (resultCacheElements.size() > 0) {
            LoggerFactory.getLogger(ManagedConnectionParser.class).
            warn("<ResultCacheBean> is incompatible with <ResultCache>. The <ResultCacheBean> has been used");
        }
        
        return StringSupport.trimOrNull(ElementSupport.getElementContentAsString(beanResultCache.get(0)));
    }

}