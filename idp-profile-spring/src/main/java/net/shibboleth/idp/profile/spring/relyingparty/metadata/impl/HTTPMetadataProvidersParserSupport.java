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

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.FileCachingHttpClientFactoryBean;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.HttpClientFactoryBean;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.InMemoryCachingHttpClientFactoryBean;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.TLSSocketFactoryFactoryBean;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
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
 */
public class HTTPMetadataProvidersParserSupport {
    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(HTTPMetadataProvidersParserSupport.class);

    /** BASIC auth username. */
    public static final String BASIC_AUTH_USER = "basicAuthUser";

    /** BASIC auth password. */
    public static final String BASIC_AUTH_PASSWORD = "basicAuthPassword";

    /** The URL for the metadata. */
    public static final String METADATA_URL = "metadataURL";

    /** TLSTrustEngine element name. */
    public static final QName TLS_TRUST_ENGINE_ELEMENT_NAME = new QName(
            AbstractMetadataProviderParser.METADATA_NAMESPACE, "TLSTrustEngine");

    private HTTPMetadataProvidersParserSupport() {
    }

    // Checkstyle: CyclomaticComplexity ON
    // Checkstyle: MethodLength OFF

    /**
     * Build the definition of the HTTPClientBuilder which contains all our configuration.
     * 
     * @param element the HTTPMetadataProvider parser.
     * @param parserContext context
     * @param httpClientSecurityParametersRef a reference,
     * @param httpClientSecurityParameters a bean definition
     * @return the bean definition with the parameters.
     * 
     * Either httpClientSecurityParametersRef or httpClientSecurityParameters can be present, not both.
     */
    protected static BeanDefinition buildTLSSocketFactory(
            final Element element,
            final ParserContext parserContext,
            final String httpClientSecurityParametersRef,
            final BeanDefinition httpClientSecurityParameters) {

        Constraint.isNotNull(log, "log must be present");
        final BeanDefinitionBuilder tlsSocketFactoryBuilder = 
                BeanDefinitionBuilder.genericBeanDefinition(TLSSocketFactoryFactoryBean.class);

        if (element.hasAttributeNS(null, "disregardTLSCertificate")) {
            tlsSocketFactoryBuilder.addPropertyValue("connectionDisregardTLSCertificate",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "disregardTLSCertificate")));
        } else if (element.hasAttributeNS(null, "disregardSslCertificate")) {
            log.warn("{}: disregardSslCertificate is deprecated, please switch to disregardTLSCertificate", 
                    parserContext.getReaderContext().getResource().getDescription());
            tlsSocketFactoryBuilder.addPropertyValue("connectionDisregardTLSCertificate",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "disregardSslCertificate")));
        }

        if (httpClientSecurityParametersRef != null) {
            tlsSocketFactoryBuilder.addPropertyReference("httpClientSecurityParameters", 
                    httpClientSecurityParametersRef);
        }
        if (httpClientSecurityParameters != null) {
            tlsSocketFactoryBuilder.addPropertyValue("httpClientSecurityParameters", 
                    httpClientSecurityParameters);
        }

        return tlsSocketFactoryBuilder.getBeanDefinition();
    }

    /**
     * Build the BeanDefinition for the POJO with the username and password.
     * 
     * @param element the HTTPMetadataProvider parser.
     * @return the bean definition with the username and password.
     */
    @Nonnull protected static BeanDefinition buildBasicCredentials(final Element element) {
        final BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(UsernamePasswordCredentials.class);

        log.warn(BASIC_AUTH_USER + " is deprecated, replace with use of httpClientSecurityParametersRef");

        builder.setLazyInit(true);

        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, BASIC_AUTH_USER)));
        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, BASIC_AUTH_PASSWORD)));

        return builder.getBeanDefinition();
    }

    /**
     * Build the BeanDefinition of the {@link HttpClientSecurityParameters} which contains the TLS trust engine
     * provided
     * 
     * @param tlsTrustEngineRef if present, the reference 
     * @param element if present, the TLSTrustEngine element.  Note : either tlsTrustEngineRef should be non null.
     * @param parserContext context
     * @return the bean definition
     */
    @Nullable protected static BeanDefinition parseTLSTrustEngine(@Nullable final String tlsTrustEngineRef, @Nullable final Element tlsTrustEngine, final ParserContext parserContext) {

        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HttpClientSecurityParameters.class);
        if (tlsTrustEngine != null) {
            if (tlsTrustEngineRef != null) {
                log.warn("<TlsTrustEngine> subelement overrides setting of tlsTrustEngineRef ");
            }
            final Element trustEngine = ElementSupport.getFirstChildElement(tlsTrustEngine,
                            AbstractMetadataProviderParser.TRUST_ENGINE_ELEMENT_NAME);

            if (trustEngine == null) {
                // This should be schema-invalid, but log a warning just in case.
                log.warn("{}:, Element {} did not contain a {} child element", 
                        parserContext.getReaderContext().getResource().getDescription(), 
                        TLS_TRUST_ENGINE_ELEMENT_NAME,
                        AbstractMetadataProviderParser.TRUST_ENGINE_ELEMENT_NAME);
                return null;
            }
            builder.addPropertyValue("tLSTrustEngine", SpringSupport.parseCustomElement(trustEngine, parserContext));
        } else if (tlsTrustEngineRef == null) {
            log.error("Internal error: tlsTrustEngineRef or TlsTrustEngine required");
            return null;
        } else {
            log.warn("tlsTrustEngineRef is deprecated");
            builder.addPropertyReference("tLSTrustEngine", tlsTrustEngineRef); 
        }

        return builder.getBeanDefinition();
    }

    /**
     * Build a {@link BeanDefinitionBuilder} for a {@link HttpClientFactoryBean} and populate it from the "standard"
     * attributes which are shared between the Dynamic and Static providers. non standard defaults are applied by the
     * caller.
     * 
     * @param element the configuration
     * @param parserContext context
     * @param defaultCaching what to use if caching not supplied.
     * @param httpClientSecurityParametersRef the client security parameters ref to be used
     * @param httpClientSecurityParameters the client security parameters to be used
     * @return an appropriate builder
     */
    @Nonnull protected static BeanDefinitionBuilder buildCommonClientBuilder(@Nonnull final Element element,
            final ParserContext parserContext,
            @Nonnull final String defaultCaching,
            @Nullable final String httpClientSecurityParametersRef,
            @Nullable final BeanDefinition httpClientSecurityParameters) {
        final String caching;
        if (element.hasAttributeNS(null, "httpCaching")) {
            caching = StringSupport.trimOrNull(element.getAttributeNS(null, "httpCaching"));
        } else {
            caching = defaultCaching;
        }
        
        final BeanDefinitionBuilder clientBuilder;
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
        if (element.hasAttributeNS(null, "requestTimeout")) {
            log.warn("requestTimeout is deprecated, use connectionTimeout");
            clientBuilder.addPropertyValue("connectionTimeout",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "requestTimeout")));
        }
        if (element.hasAttributeNS(null, "connectionTimeout")) {
            clientBuilder.addPropertyValue("connectionTimeout",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "connectionTimeout")));
        }
        if (element.hasAttributeNS(null, "connectionRequestTimeout")) {
            clientBuilder.addPropertyValue("connectionRequestTimeout",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "connectionRequestTimeout")));
        }
        if (element.hasAttributeNS(null, "socketTimeout")) {
            clientBuilder.addPropertyValue("socketTimeout",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "socketTimeout")));
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

        return clientBuilder;
    }
}