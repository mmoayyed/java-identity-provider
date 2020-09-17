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

import java.io.IOException;

import net.shibboleth.idp.attribute.resolver.spring.dc.impl.CacheConfigParser;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.google.common.cache.Cache;

/**
 * Code to exercise the ResultCache Parser outwith the DataConnector parsers.
 * There is no actual testing since we cannot look at what is created.
 */
@SuppressWarnings("javadoc")
public class CacheConfigParserTest extends OpenSAMLInitBaseTestCase {
    //
    
    @Test public void ttl() throws XMLParserException, IOException {
        final ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        final Resource resource= new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ResultCacheElementTimeToLive.xml");
        final Document doc = parserPool.parse(resource.getInputStream());
            final CacheConfigParser ccp = new CacheConfigParser(doc.getDocumentElement());
        
        try (final GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBeanDefinition("ElementTTL", ccp.createCache(null));
            context.refresh();
            context.getBean("ElementTTL", Cache.class);
        }
    }

    @Test public void access() throws XMLParserException, IOException {
        final ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        final Resource resource= new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ResultCacheExpireAfterAccess.xml");
        final Document doc = parserPool.parse(resource.getInputStream());
            final CacheConfigParser ccp = new CacheConfigParser(doc.getDocumentElement());
        
        try (final GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBeanDefinition("Access", ccp.createCache(null));
            context.refresh();
            context.getBean("Access", Cache.class);
        }
    }

    @Test public void write() throws XMLParserException, IOException {
        final ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        final Resource resource= new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ResultCacheExpireAfterWrite.xml");
        final Document doc = parserPool.parse(resource.getInputStream());
            final CacheConfigParser ccp = new CacheConfigParser(doc.getDocumentElement());
        
        try (final GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBeanDefinition("Write", ccp.createCache(null));
            context.refresh();
            context.getBean("Write", Cache.class);
        }
    }

    @Test public void both() throws XMLParserException, IOException {
        final ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        final Resource resource= new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/ResultCacheExpireBoth.xml");
        final Document doc = parserPool.parse(resource.getInputStream());
            final CacheConfigParser ccp = new CacheConfigParser(doc.getDocumentElement());
        
        try (final GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBeanDefinition("Write", ccp.createCache(null));
            context.refresh();
            context.getBean("Write", Cache.class);
        }
    }

}