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

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
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

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doNativeParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        super.doNativeParse(element, parserContext, builder);

        final String tlsTrustEngineRef = StringSupport.trimOrNull(element.getAttributeNS(null, "tlsTrustEngineRef"));
        final Element tlsTrustEngine = ElementSupport.getFirstChildElement(element,
                HTTPMetadataProvidersParserSupport.TLS_TRUST_ENGINE_ELEMENT_NAME);
        final String httpClientSecurityParametersRef =
                StringSupport.trimOrNull(element.getAttributeNS(null, "httpClientSecurityParametersRef"));
        BeanDefinition httpClientSecurityParameters = null;

        if (httpClientSecurityParametersRef != null) {
            if (tlsTrustEngine != null || tlsTrustEngineRef != null) {
                log.warn("httpClientSecurityParametersRef overrides tlsTrustEngineRef or <TrustEngine> subelement");
            }
            builder.addPropertyReference("httpClientSecurityParameters", httpClientSecurityParametersRef);
        } else if (tlsTrustEngine != null || tlsTrustEngineRef != null) {
            httpClientSecurityParameters = HTTPMetadataProvidersParserSupport.parseTLSTrustEngine(
                    tlsTrustEngineRef, tlsTrustEngine, parserContext);
            builder.addPropertyValue("httpClientSecurityParameters", httpClientSecurityParameters);
        }

        if (element.hasAttributeNS(null, "httpClientRef")) {
            builder.addConstructorArgReference(StringSupport.trimOrNull(element.getAttributeNS(null, "httpClientRef")));
            if (element.hasAttributeNS(null, "connectionTimeout")
                    || element.hasAttributeNS(null, "connectionRequestTimeout")
                    || element.hasAttributeNS(null, "socketTimeout")
                    || element.hasAttributeNS(null, "maxConnectionsTotal")
                    || element.hasAttributeNS(null, "maxConnectionsPerRoute")
                    || element.hasAttributeNS(null, "disregardTLSCertificate")
                    || element.hasAttributeNS(null, "proxyHost") || element.hasAttributeNS(null, "proxyPort")
                    || element.hasAttributeNS(null, "proxyUser") || element.hasAttributeNS(null, "proxyPassword")) {
                log.warn("httpClientRef overrides settings for connectionTimeout, "
                    + "connectionRequestTimeout, socketTimeout, maxConnectionsTotal, maxConnectionsPerRoute, "
                    + "disregardTLSCertificate, proxyHost, proxyPort, "
                    + "proxyUser and proxyPassword");
            }
        } else {
            builder.addConstructorArgValue(buildHttpClient(element, parserContext,
                    httpClientSecurityParametersRef, httpClientSecurityParameters));
        }

        if (element.hasAttributeNS(null, HTTPMetadataProvidersParserSupport.BASIC_AUTH_USER) ||
           element.hasAttributeNS(null, HTTPMetadataProvidersParserSupport.BASIC_AUTH_PASSWORD)) {
            builder.addPropertyValue("basicCredentials",
                    HTTPMetadataProvidersParserSupport.buildBasicCredentials(element, parserContext));
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
    private BeanDefinition buildHttpClient(final Element element, final ParserContext parserContext,
            @Nullable final String httpClientSecurityParametersRef,
            @Nullable final BeanDefinition httpClientSecurityParameters) {

        final BeanDefinitionBuilder clientBuilder = 
                HTTPMetadataProvidersParserSupport.buildCommonClientBuilder(element, parserContext, DEFAULT_CACHING,
                        httpClientSecurityParametersRef, httpClientSecurityParameters);

        // Set up non standard defaults
        if (!element.hasAttributeNS(null, "connectionTimeout")) {
            clientBuilder.addPropertyValue("connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        }

        if (!element.hasAttributeNS(null, "connectionRequestTimeout")) {
            clientBuilder.addPropertyValue("connectionRequestTimeout", DEFAULT_CONNECTION_REQUEST_TIMEOUT);
        }
        if (!element.hasAttributeNS(null, "socketTimeout")) {
            clientBuilder.addPropertyValue("socketTimeout", DEFAULT_SOCKET_TIMEOUT);
        }

        if (element.hasAttributeNS(null, "maxConnectionsTotal")) {
            clientBuilder.addPropertyValue("maxConnectionsTotal",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "maxConnectionsTotal")));
        } else {
            clientBuilder.addPropertyValue("maxConnectionsTotal", DEFAULT_MAX_CONNECTIONS_TOTAL);
        }

        // set up non-common attributes
        if (element.hasAttributeNS(null, "maxConnectionsPerRoute")) {
            clientBuilder.addPropertyValue("maxConnectionsPerRoute",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "maxConnectionsPerRoute")));
        } else {
            clientBuilder.addPropertyValue("maxConnectionsPerRoute", DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        }

        return clientBuilder.getBeanDefinition();
    }

}