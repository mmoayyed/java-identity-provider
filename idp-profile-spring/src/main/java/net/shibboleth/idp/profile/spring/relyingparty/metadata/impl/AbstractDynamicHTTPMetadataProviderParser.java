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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.impl;

import java.util.List;

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.FileCachingHttpClientFactoryBean;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.HttpClientFactoryBean;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.InMemoryCachingHttpClientFactoryBean;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for abstract dynamic HTTP metadata resolvers.
 */
public abstract class AbstractDynamicHTTPMetadataProviderParser extends AbstractDynamicMetadataProviderParser {

    /** Default caching type . */
    private static final String DEFAULT_CACHING = "memory";
    
    /** Default max total connections. */
    private static final Integer DEFAULT_MAX_CONNECTIONS_TOTAL = 100;
    
    /** Default max connections per route. */
    private static final Integer DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 100;
        
    /** Default request connection timeout. */
    private static final String DEFAULT_CONNECTION_REQUEST_TIMEOUT = "PT5S";
    
    /** Default connection timeout. */
    private static final String DEFAULT_CONNECTION_TIMEOUT = "PT5S";
    
    /** Default socket timeout. */
    private static final String DEFAULT_SOCKET_TIMEOUT = "PT5S";

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractDynamicHTTPMetadataProviderParser.class);

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF -- more readable not split up
    @Override protected void doNativeParse(final Element element, final ParserContext parserContext, 
            final BeanDefinitionBuilder builder) {
        super.doNativeParse(element, parserContext, builder);

        final String tlsTrustEngineRef = StringSupport.trimOrNull(element.getAttributeNS(null, "tlsTrustEngineRef"));
        final Element tlsTrustEngine = ElementSupport.getFirstChildElement(element, HTTPMetadataProvidersParserSupport.TLS_TRUST_ENGINE_ELEMENT_NAME);
        final String httpClientSecurityParametersRef = StringSupport.trimOrNull(element.getAttributeNS(null, "httpClientSecurityParametersRef"));
        BeanDefinition httpClientSecurityParameters = null;
        
        if (httpClientSecurityParametersRef != null) {
            if (tlsTrustEngine != null || tlsTrustEngineRef != null) {
                log.warn("httpClientSecurityParametersRef overrides setting of tlsTrustEngineRef or of <TrustEngine> subelement");
            }
            builder.addPropertyReference("httpClientSecurityParameters", httpClientSecurityParametersRef);
        } else if (tlsTrustEngine != null || tlsTrustEngineRef != null)  {
            httpClientSecurityParameters =  HTTPMetadataProvidersParserSupport.parseTLSTrustEngine(tlsTrustEngineRef, tlsTrustEngine, parserContext);
            builder.addPropertyValue("httpClientSecurityParameters", httpClientSecurityParameters);
        }
      
        if (element.hasAttributeNS(null, "httpClientRef")) {
            builder.addConstructorArgReference(StringSupport.trimOrNull(element.getAttributeNS(null, "httpClientRef")));
            if (element.hasAttributeNS(null, "requestTimeout")
                    || element.hasAttributeNS(null, "connectionTimeout")
                    || element.hasAttributeNS(null, "connectionRequestTimeout")
                    || element.hasAttributeNS(null, "socketTimeout")
                    || element.hasAttributeNS(null, "maxConnectionsTotal")
                    || element.hasAttributeNS(null, "maxConnectionsPerRoute")
                    || element.hasAttributeNS(null, "disregardSslCertificate")
                    || element.hasAttributeNS(null, "disregardTLSCertificate")
                    || element.hasAttributeNS(null, "proxyHost") || element.hasAttributeNS(null, "proxyPort")
                    || element.hasAttributeNS(null, "proxyUser") || element.hasAttributeNS(null, "proxyPassword")) {
                log.warn("httpClientRef overrides settings for requestTimeout, connectionTimeout, " 
                        + "connectionRequestTimeout, socketTimeout, maxConnectionsTotal, maxConnectionsPerRoute, " 
                        + "disregardSslCertificate, disregardTLSCertificate, proxyHost, proxyPort, " 
                        + "proxyUser and proxyPassword");
            }
        } else {
            builder.addConstructorArgValue(buildHttpClient(element, parserContext,
                    httpClientSecurityParametersRef, httpClientSecurityParameters));
        }

        if (element.hasAttributeNS(null, HTTPMetadataProvidersParserSupport.BASIC_AUTH_USER) ||
           element.hasAttributeNS(null, HTTPMetadataProvidersParserSupport.BASIC_AUTH_PASSWORD)) {
            builder.addPropertyValue("basicCredentials", HTTPMetadataProvidersParserSupport.buildBasicCredentials(element));
        }

        if (element.hasAttributeNS(null, "supportedContentTypes")) {
            final List<String> supportedContentTypes =
                    StringSupport.stringToList(
                            StringSupport.trimOrNull(element.getAttributeNS(null, "supportedContentTypes")),
                            XMLConstants.LIST_DELIMITERS);
            builder.addPropertyValue("supportedContentTypes", supportedContentTypes);
        }

    }

    // Checkstyle: CyclomaticComplexity ON

    /**
     * Build the definition of the HTTPClientBuilder which contains all our configuration.
     * 
     * @param element the HTTPMetadataProvider parser.
     * @param parserContext the context
     * @param httpClientSecurityParametersRef the client security parameters ref to be used
     * @param httpClientSecurityParameters the client security parameters to be used
     * @return the bean definition with the parameters.
     * 
     * Either httpClientSecurityParametersRef or httpClientSecurityParameters can be present, not both.
     */
    // Checkstyle: CyclomaticComplexity OFF
    // Checkstyle: MethodLength OFF
    private BeanDefinition buildHttpClient(final Element element,
            final ParserContext parserContext,
            @Nullable final String httpClientSecurityParametersRef,
            @Nullable final BeanDefinition httpClientSecurityParameters) {

        String caching = DEFAULT_CACHING;
        if (element.hasAttributeNS(null, "httpCaching")) {
            caching = StringSupport.trimOrNull(element.getAttributeNS(null, "httpCaching"));
        }

        BeanDefinitionBuilder clientBuilder = null;
        switch (caching) {
            case "none":
                clientBuilder = BeanDefinitionBuilder.genericBeanDefinition(HttpClientFactoryBean.class);
                break;
            case "file":
                clientBuilder = BeanDefinitionBuilder.genericBeanDefinition(FileCachingHttpClientFactoryBean.class);
                if (element.hasAttributeNS(null, "httpCacheDirectory")) {
                    clientBuilder.addPropertyValue("cacheDirectory",
                            StringSupport.trimOrNull(element.getAttributeNS(null, "httpCacheDirectory")));
                }
                if (element.hasAttributeNS(null, "httpMaxCacheEntries")) {
                    clientBuilder.addPropertyValue("maxCacheEntries",
                            StringSupport.trimOrNull(element.getAttributeNS(null, "httpMaxCacheEntries")));
                }
                if (element.hasAttributeNS(null, "httpMaxCacheEntrySize")) {
                    clientBuilder.addPropertyValue("maxCacheEntrySize",
                            StringSupport.trimOrNull(element.getAttributeNS(null, "httpMaxCacheEntrySize")));
                }
                break;
            case "memory":
                clientBuilder = BeanDefinitionBuilder.genericBeanDefinition(InMemoryCachingHttpClientFactoryBean.class);
                if (element.hasAttributeNS(null, "httpMaxCacheEntries")) {
                    clientBuilder.addPropertyValue("maxCacheEntries",
                            StringSupport.trimOrNull(element.getAttributeNS(null, "httpMaxCacheEntries")));
                }
                if (element.hasAttributeNS(null, "httpMaxCacheEntrySize")) {
                    clientBuilder.addPropertyValue("maxCacheEntrySize",
                            StringSupport.trimOrNull(element.getAttributeNS(null, "httpMaxCacheEntrySize")));
                }
                break;
            default:
                throw new BeanDefinitionParsingException(new Problem(String.format("Caching value '%s' is unsupported",
                        caching), new Location(parserContext.getReaderContext().getResource())));
        }

        clientBuilder.setLazyInit(true);

        //Note: 'requestTimeout' is deprecated in favor of 'connectionTimeout'.
        if (element.hasAttributeNS(null, "requestTimeout") || element.hasAttributeNS(null, "connectionTimeout")) {
            if (element.hasAttributeNS(null, "requestTimeout")) {
                clientBuilder.addPropertyValue("connectionTimeout",
                        StringSupport.trimOrNull(element.getAttributeNS(null, "requestTimeout")));
            }
            if (element.hasAttributeNS(null, "connectionTimeout")) {
                clientBuilder.addPropertyValue("connectionTimeout",
                        StringSupport.trimOrNull(element.getAttributeNS(null, "connectionTimeout")));
            }
        } else {
            clientBuilder.addPropertyValue("connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        }
        
        if (element.hasAttributeNS(null, "connectionRequestTimeout")) {
            clientBuilder.addPropertyValue("connectionRequestTimeout",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "connectionRequestTimeout")));
        } else {
            clientBuilder.addPropertyValue("connectionRequestTimeout", DEFAULT_CONNECTION_REQUEST_TIMEOUT);
        }
        if (element.hasAttributeNS(null, "socketTimeout")) {
            clientBuilder.addPropertyValue("socketTimeout",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "socketTimeout")));
        } else {
            clientBuilder.addPropertyValue("socketTimeout", DEFAULT_SOCKET_TIMEOUT);
        }
        
        if (element.hasAttributeNS(null, "maxConnectionsTotal")) {
            clientBuilder.addPropertyValue("maxConnectionsTotal",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "maxConnectionsTotal")));
        } else {
            clientBuilder.addPropertyValue("maxConnectionsTotal", DEFAULT_MAX_CONNECTIONS_TOTAL);
        }
        if (element.hasAttributeNS(null, "maxConnectionsPerRoute")) {
            clientBuilder.addPropertyValue("maxConnectionsPerRoute",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "maxConnectionsPerRoute")));
        } else {
            clientBuilder.addPropertyValue("maxConnectionsPerRoute", DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        }
        
        clientBuilder.addPropertyValue("tLSSocketFactory", HTTPMetadataProvidersParserSupport.buildTLSSocketFactory(element, parserContext, 
                httpClientSecurityParametersRef, httpClientSecurityParameters));

        if (element.hasAttributeNS(null, "proxyHost")) {
            clientBuilder.addPropertyValue("connectionProxyHost",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "proxyHost")));
        }

        if (element.hasAttributeNS(null, "proxyPort")) {
            clientBuilder.addPropertyValue("connectionProxyPort",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "proxyPort")));
        }

        if (element.hasAttributeNS(null, "proxyUser")) {
            clientBuilder.addPropertyValue("connectionProxyUsername",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "proxyUser")));
        }

        if (element.hasAttributeNS(null, "proxyPassword")) {
            clientBuilder.addPropertyValue("connectionProxyPassword", element.getAttributeNS(null, "proxyPassword"));
        }

        return clientBuilder.getBeanDefinition();
    }

}
