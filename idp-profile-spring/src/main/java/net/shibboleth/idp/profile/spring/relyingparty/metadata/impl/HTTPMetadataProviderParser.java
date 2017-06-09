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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver;
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
 * Parser for a FilesystemMetadataProvider.
 */
public class HTTPMetadataProviderParser extends AbstractReloadingMetadataProviderParser {

    /** Element name. */
    @Nonnull public static final QName ELEMENT_NAME = new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE,
            "HTTPMetadataProvider");

    /** Default caching type. */
    @Nonnull @NotEmpty private static final String DEFAULT_CACHING = "none";

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTTPMetadataProviderParser.class);

    /** {@inheritDoc} */
    @Override protected Class<? extends HTTPMetadataResolver> getNativeBeanClass(final Element element) {
        return HTTPMetadataResolver.class;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doNativeParse(final Element element, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        super.doNativeParse(element, parserContext, builder);

        if (element.hasAttributeNS(null, "cacheDuration")) {
            log.error("{}: cacheDuration is not supported",
                    parserContext.getReaderContext().getResource().getDescription());
            throw new BeanDefinitionParsingException(new Problem("cacheDuration is not supported", new Location(
                    parserContext.getReaderContext().getResource())));
        }

        if (element.hasAttributeNS(null, "maintainExpiredMetadata")) {
            log.error("{}: maintainExpiredMetadata is not supported", parserContext.getReaderContext().getResource()
                    .getDescription());
            throw new BeanDefinitionParsingException(new Problem("maintainExpiredMetadata is not supported",
                    new Location(parserContext.getReaderContext().getResource())));
        }

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
        } else if (tlsTrustEngine != null || tlsTrustEngineRef != null)  {
            httpClientSecurityParameters = HTTPMetadataProvidersParserSupport.parseTLSTrustEngine(tlsTrustEngineRef,
                    tlsTrustEngine, parserContext);
            builder.addPropertyValue("httpClientSecurityParameters", httpClientSecurityParameters);
        }

        if (element.hasAttributeNS(null, "httpClientRef")) {
            builder.addConstructorArgReference(StringSupport.trimOrNull(element.getAttributeNS(null, "httpClientRef")));
            if (element.hasAttributeNS(null, "requestTimeout")
                    || element.hasAttributeNS(null, "connectionTimeout")
                    || element.hasAttributeNS(null, "connectionRequestTimeout")
                    || element.hasAttributeNS(null, "socketTimeout")
                    || element.hasAttributeNS(null, "disregardSslCertificate")
                    || element.hasAttributeNS(null, "disregardTLSCertificate")
                    || element.hasAttributeNS(null, "proxyHost") || element.hasAttributeNS(null, "proxyPort")
                    || element.hasAttributeNS(null, "proxyUser") || element.hasAttributeNS(null, "proxyPassword")) {
                log.warn("httpClientRef overrides settings for requestTimeout, connectionTimeout, " 
                    + "connectionRequestTimeout, socketTimeout, disregardSslCertificate, disregardTLSCertificate, "
                    + " proxyHost, proxyPort, proxyUser and proxyPassword");
            }
        } else {
            builder.addConstructorArgValue(buildHttpClient(element, parserContext,
                    httpClientSecurityParametersRef, httpClientSecurityParameters));
        }
        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null,
                HTTPMetadataProvidersParserSupport.METADATA_URL)));

        if (element.hasAttributeNS(null, HTTPMetadataProvidersParserSupport.BASIC_AUTH_USER)
                || element.hasAttributeNS(null, HTTPMetadataProvidersParserSupport.BASIC_AUTH_PASSWORD)) {
            builder.addPropertyValue("basicCredentials",
                    HTTPMetadataProvidersParserSupport.buildBasicCredentials(element, parserContext));
        }

    }
// Checkstyle: CyclomaticComplexity ON

    /**
     * Build the definition of the HTTPClientBuilder which contains all our configuration.
     * 
     * @param element the HTTPMetadataProvider parser.
     * @param parserContext context
     * @param httpClientSecurityParametersRef the client security parameters ref to be used
     * @param httpClientSecurityParameters the client security parameters to be used
     * @return the bean definition with the parameters.
     * 
     * Either httpClientSecurityParametersRef or httpClientSecurityParameters can be present, not both.
     */
    private BeanDefinition buildHttpClient(final Element element, final ParserContext parserContext,
            @Nullable final String httpClientSecurityParametersRef,
            @Nullable final BeanDefinition httpClientSecurityParameters) {

        return HTTPMetadataProvidersParserSupport.buildCommonClientBuilder(element, parserContext,
                HTTPMetadataProviderParser.DEFAULT_CACHING, httpClientSecurityParametersRef,
                httpClientSecurityParameters).getBeanDefinition();
    }
    
}