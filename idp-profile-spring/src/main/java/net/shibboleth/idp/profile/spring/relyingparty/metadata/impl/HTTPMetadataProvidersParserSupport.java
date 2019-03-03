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
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
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
 * Helper class for Spring configuration of HTTP metadata providers.
 */
public final class HTTPMetadataProvidersParserSupport {
    
    /** BASIC auth username. */
    @Nonnull @NotEmpty public static final String BASIC_AUTH_USER = "basicAuthUser";

    /** BASIC auth password. */
    @Nonnull @NotEmpty public static final String BASIC_AUTH_PASSWORD = "basicAuthPassword";

    /** The URL for the metadata. */
    @Nonnull @NotEmpty public static final String METADATA_URL = "metadataURL";

    /** TLSTrustEngine element name. */
    @Nonnull public static final QName TLS_TRUST_ENGINE_ELEMENT_NAME =
            new QName(AbstractMetadataProviderParser.METADATA_NAMESPACE, "TLSTrustEngine");

    /** Class logger. */
    @Nonnull private static final Logger LOG = LoggerFactory.getLogger(HTTPMetadataProvidersParserSupport.class);

    /** Constructor. */
    private HTTPMetadataProvidersParserSupport() {
    }
    
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
            @Nonnull final Element element, @Nonnull final ParserContext parserContext,
            @Nullable final String httpClientSecurityParametersRef,
            @Nullable final BeanDefinition httpClientSecurityParameters) {

        Constraint.isNotNull(LOG, "LOG must be present");
        final BeanDefinitionBuilder tlsSocketFactoryBuilder = 
                BeanDefinitionBuilder.genericBeanDefinition(TLSSocketFactoryFactoryBean.class);

        if (element.hasAttributeNS(null, "disregardTLSCertificate")) {
            tlsSocketFactoryBuilder.addPropertyValue("connectionDisregardTLSCertificate",
                    StringSupport.trimOrNull(element.getAttributeNS(null, "disregardTLSCertificate")));
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
     * @param element the HTTPMetadataProvider element
     * @param parserContext parser context
     * 
     * @return the bean definition with the username and password.
     */
    @Nonnull protected static BeanDefinition buildBasicCredentials(final Element element,
            @Nonnull final ParserContext parserContext) {
        final BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(UsernamePasswordCredentials.class);

        DeprecationSupport.warn(ObjectType.ATTRIBUTE, BASIC_AUTH_USER,
                parserContext.getReaderContext().getResource().getDescription(), "httpClientSecurityParametersRef");
        DeprecationSupport.warn(ObjectType.ATTRIBUTE, BASIC_AUTH_PASSWORD,
                parserContext.getReaderContext().getResource().getDescription(), "httpClientSecurityParametersRef");

        builder.setLazyInit(true);

        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, BASIC_AUTH_USER)));
        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, BASIC_AUTH_PASSWORD)));

        return builder.getBeanDefinition();
    }

    /**
     * Build the BeanDefinition of the {@link HttpClientSecurityParameters} which contains the TLS trust engine
     * provided.
     * 
     * <p>One of the first two parameters must be non-null.</p>
     * 
     * @param tlsTrustEngineRef if present, the reference 
     * @param tlsTrustEngine if present, the TLSTrustEngine element
     * @param parserContext context
     * 
     * @return the bean definition
     */
    @Nullable protected static BeanDefinition parseTLSTrustEngine(@Nullable final String tlsTrustEngineRef,
            @Nullable final Element tlsTrustEngine, @Nonnull final ParserContext parserContext) {

        final BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(HttpClientSecurityParameters.class);
        if (tlsTrustEngine != null) {
            if (tlsTrustEngineRef != null) {
                LOG.warn("<TLSTrustEngine> subelement overrides setting of tlsTrustEngineRef ");
            }
            final Element trustEngine = ElementSupport.getFirstChildElement(tlsTrustEngine,
                            AbstractMetadataProviderParser.TRUST_ENGINE_ELEMENT_NAME);

            if (trustEngine == null) {
                // This should be schema-invalid, but LOG a warning just in case.
                LOG.warn("{}:, Element {} did not contain a {} child element", 
                        parserContext.getReaderContext().getResource().getDescription(), 
                        TLS_TRUST_ENGINE_ELEMENT_NAME,
                        AbstractMetadataProviderParser.TRUST_ENGINE_ELEMENT_NAME);
                return null;
            }
            builder.addPropertyValue("tLSTrustEngine", SpringSupport.parseCustomElement(trustEngine, parserContext));
        } else if (tlsTrustEngineRef == null) {
            LOG.error("Internal error: tlsTrustEngineRef or TlsTrustEngine required");
            return null;
        } else {
            DeprecationSupport.warn(ObjectType.ATTRIBUTE, "tlsTrustEngineRef",
                    parserContext.getReaderContext().getResource().getDescription(),
                    "inline <TrustEngine> element or httpClientSecurityParametersRef attribute");
            builder.addPropertyReference("tLSTrustEngine", tlsTrustEngineRef); 
        }

        return builder.getBeanDefinition();
    }

// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /**
     * Build a {@link BeanDefinitionBuilder} for a {@link HttpClientFactoryBean} and populate it from the "standard"
     * attributes which are shared between the Dynamic and Static providers. non standard defaults are applied by the
     * caller.
     * 
     * @param element the configuration
     * @param parserContext context
     * @param clientBuildClass the type of builder to create.
     * @param httpClientSecurityParametersRef the client security parameters ref to be used
     * @param httpClientSecurityParameters the client security parameters to be used
     * 
     * @return an appropriate builder
     */
    @Nonnull protected static BeanDefinitionBuilder buildCommonClientBuilder(@Nonnull final Element element,
            @Nonnull final ParserContext parserContext, @Nonnull final Class<?> clientBuildClass,
            @Nullable final String httpClientSecurityParametersRef,
            @Nullable final BeanDefinition httpClientSecurityParameters) {

        final BeanDefinitionBuilder clientBuilder = BeanDefinitionBuilder.genericBeanDefinition(clientBuildClass);
        clientBuilder.setLazyInit(true);

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

        clientBuilder.addPropertyValue("tLSSocketFactory",
                HTTPMetadataProvidersParserSupport.buildTLSSocketFactory(
                        element, parserContext, httpClientSecurityParametersRef, httpClientSecurityParameters));

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
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
}