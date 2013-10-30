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

package net.shibboleth.idp.attribute.resolver.spring.dc.ldap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.LdapDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.TemplatedExecutableSearchFilterBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.DataConnectorNamespaceHandler;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchScope;
import org.ldaptive.handler.CaseChangeEntryHandler;
import org.ldaptive.handler.CaseChangeEntryHandler.CaseChange;
import org.ldaptive.handler.MergeAttributeEntryHandler;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.pool.SoftLimitConnectionPool;
import org.ldaptive.provider.ConnectionStrategy;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.SaslConfig;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.CredentialConfigFactory;
import org.ldaptive.ssl.SslConfig;
import org.opensaml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.cache.Cache;

/** Bean definition Parser for a {@link LdapDataConnector}. */
public class LdapDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Attribute");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(LdapDataConnectorParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(final Element element) {
        return LdapDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void
            doParse(final Element config, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        log.debug("doParse {}", config);

        final Element springBeans = getSpringBeansElement(config);
        if (springBeans == null) {
            log.debug("parsing v2 configuration");
            doParseV2(config, parserContext, builder);
        } else {
            doParseInternal(config, parserContext, builder);
        }

    }

    /**
     * Parses a Spring <beans/> configuration.
     * 
     * @param config LDAPDirectory containing Spring configuration
     * @param parserContext bean definition parsing context
     * @param builder to initialize
     */
    protected void doParseInternal(final Element config, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        final Element springBeans = getSpringBeansElement(config);
        final BeanFactory beanFactory = createBeanFactory(springBeans);
        addPropertyDescriptorValues(builder, beanFactory, LdapDataConnector.class);

        final Boolean noResultAnError =
                AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(config, new QName(
                        "noResultIsError")));
        log.debug("parsed noResultAnError {}", noResultAnError);
        if (noResultAnError != null && noResultAnError.booleanValue()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        builder.setInitMethodName("initialize");
    }

    /**
     * Parses a version 2 configuration.
     * 
     * @param config LDAPDirectory containing v2 configuration
     * @param parserContext bean definition parsing context
     * @param builder to initialize
     */
    protected void doParseV2(final Element config, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        final V2Parser v2Parser = new V2Parser(config);

        final ConnectionConfig connectionConfig = v2Parser.createConnectionConfig();
        final DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory(connectionConfig);
        final String connectionStrategy = AttributeSupport.getAttributeValue(config, new QName("connectionStrategy"));
        if (connectionStrategy != null) {
            final ConnectionStrategy strategy = ConnectionStrategy.valueOf(connectionStrategy);
            if (strategy != null) {
                connectionFactory.getProvider().getProviderConfig().setConnectionStrategy(strategy);
            } else {
                connectionFactory.getProvider().getProviderConfig()
                        .setConnectionStrategy(ConnectionStrategy.ACTIVE_PASSIVE);
            }
        }

        final Map<String, Object> props = new HashMap<String, Object>();
        final List<Element> propertyElements =
                ElementSupport.getChildElements(config, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                        "LDAPProperty"));
        for (Element e : propertyElements) {
            props.put(AttributeSupport.getAttributeValue(e, new QName("name")),
                    AttributeSupport.getAttributeValue(e, new QName("value")));
        }
        if (!props.isEmpty()) {
            connectionFactory.getProvider().getProviderConfig().setProperties(props);
        }

        final BeanDefinitionBuilder templateBuilder = v2Parser.createTemplateBuilder();
        builder.addPropertyValue("executableSearchBuilder", templateBuilder.getBeanDefinition());

        final BlockingConnectionPool connectionPool = v2Parser.createConnectionPool();
        if (connectionPool != null) {
            connectionPool.setConnectionFactory(connectionFactory);
            connectionPool.initialize();
            builder.addPropertyValue("connectionFactory", new PooledConnectionFactory(connectionPool));
        } else {
            builder.addPropertyValue("connectionFactory", connectionFactory);
        }

        final SearchExecutor searchExecutor = v2Parser.createSearchExecutor();
        builder.addPropertyValue("searchExecutor", searchExecutor);

        final Cache<String, Map<String, IdPAttribute>> cache = v2Parser.createCache();
        if (cache != null) {
            builder.addPropertyValue("resultsCache", cache);
        }

        if (v2Parser.isNoResultAnError()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        builder.setInitMethodName("initialize");
    }

    /** Utility class for parsing v2 schema configuration. */
    protected class V2Parser {

        /** LDAPDirectory XML element. */
        private final Element configElement;

        /**
         * Creates a new V2Parser with the supplied LDAPDirectory element.
         * 
         * @param config LDAPDirectory element
         */
        public V2Parser(@Nonnull final Element config) {
            Constraint.isNotNull(config, "LDAPDirectory element cannot be null");
            configElement = config;
        }

        /**
         * Returns whether the noResultIsError attribute exists and is true.
         * 
         * @return whether the noResultIsError attribute exists and is true
         */
        public boolean isNoResultAnError() {
            final Boolean noResultAnError =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement, new QName(
                            "noResultIsError")));
            return noResultAnError != null ? noResultAnError.booleanValue() : false;
        }

        /**
         * Creates a connection config from a v2 XML configuration.
         * 
         * @return connection config
         */
        @Nonnull public ConnectionConfig createConnectionConfig() {
            final String url = AttributeSupport.getAttributeValue(configElement, new QName("ldapURL"));
            final Boolean useStartTLS =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement, new QName(
                            "useStartTLS")));
            final String principal = AttributeSupport.getAttributeValue(configElement, new QName("principal"));
            final String principalCredential =
                    AttributeSupport.getAttributeValue(configElement, new QName("principalCredential"));
            final String authenticationType =
                    AttributeSupport.getAttributeValue(configElement, new QName("authenticationType"));

            final ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setLdapUrl(url);
            if (useStartTLS != null && useStartTLS.booleanValue()) {
                connectionConfig.setUseStartTLS(true);
            }
            final SslConfig sslConfig = new SslConfig();
            sslConfig.setCredentialConfig(createCredentialConfig());
            connectionConfig.setSslConfig(sslConfig);
            final BindConnectionInitializer connectionInitializer = new BindConnectionInitializer();
            if (principal != null) {
                connectionInitializer.setBindDn(principal);
            }
            if (principalCredential != null) {
                connectionInitializer.setBindCredential(new Credential(principalCredential));
            }
            if (authenticationType != null) {
                final Mechanism mechanism = Mechanism.valueOf(authenticationType);
                if (mechanism != null) {
                    final SaslConfig config = new SaslConfig();
                    config.setMechanism(mechanism);
                    connectionInitializer.setBindSaslConfig(config);
                }
            }
            if (!connectionInitializer.isEmpty()) {
                connectionConfig.setConnectionInitializer(connectionInitializer);
            }
            return connectionConfig;
        }

        /**
         * Uses {@link X509CredentialSupport} to read StartTLS trust and authentication credentials.
         * 
         * @return credential config
         */
        @Nonnull protected CredentialConfig createCredentialConfig() {
            X509Certificate[] trustCerts = null;
            final X509Credential trustCredential =
                    X509CredentialSupport.parseX509Credential(ElementSupport.getFirstChildElement(configElement,
                            new QName(DataConnectorNamespaceHandler.NAMESPACE, "StartTLSTrustCredential")));
            if (trustCredential != null) {
                trustCerts =
                        trustCredential.getEntityCertificateChain().toArray(
                                new X509Certificate[trustCredential.getEntityCertificateChain().size()]);
            }

            X509Certificate authCert = null;
            PrivateKey authKey = null;
            final X509Credential authCredential =
                    X509CredentialSupport.parseX509Credential(ElementSupport.getFirstChildElement(configElement,
                            new QName(DataConnectorNamespaceHandler.NAMESPACE, "StartTLSAuthenticationCredential")));
            if (authCredential != null) {
                authCert = authCredential.getEntityCertificate();
                authKey = authCredential.getPrivateKey();
            }

            return CredentialConfigFactory.createX509CredentialConfig(trustCerts, authCert, authKey);
        }

        /**
         * Construct the definition of the template driven search builder.
         * 
         * @return the bean definition for the template search builder.
         */
        @Nonnull public BeanDefinitionBuilder createTemplateBuilder() {
            BeanDefinitionBuilder templateBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(TemplatedExecutableSearchFilterBuilder.class);

            String velocityEngineRef = StringSupport.trimOrNull(configElement.getAttribute("templateEngine"));
            if (null == velocityEngineRef) {
                velocityEngineRef = "shibboleth.VelocityEngine";
            }
            templateBuilder.addPropertyReference("velocityEngine", velocityEngineRef);

            templateBuilder.addPropertyValue("v2Compatibility", true);

            String filter = null;
            final Element filterElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "FilterTemplate"));
            if (filterElement != null) {
                filter = StringSupport.trimOrNull(filterElement.getTextContent().trim());
            }

            templateBuilder.addPropertyValue("templateText", filter);

            templateBuilder.setInitMethodName("initialize");
            return templateBuilder;
        }

        /**
         * Creates a new search executor from a v2 XML configuration.
         * 
         * @return search executor
         */
        @Nonnull public SearchExecutor createSearchExecutor() {
            final String baseDn = AttributeSupport.getAttributeValue(configElement, new QName("baseDN"));
            final String searchScope = AttributeSupport.getAttributeValue(configElement, new QName("searchScope"));
            final String searchTimeLimit =
                    AttributeSupport.getAttributeValue(configElement, new QName("searchTimeLimit"));
            final String maxResultSize = AttributeSupport.getAttributeValue(configElement, new QName("maxResultSize"));
            final Boolean mergeResults =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement, new QName(
                            "mergeResults")));
            final Boolean lowercaseAttributeNames =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement, new QName(
                            "lowercaseAttributeNames")));

            final SearchExecutor searchExecutor = new SearchExecutor();
            searchExecutor.setBaseDn(baseDn);
            if (searchScope != null) {
                searchExecutor.setSearchScope(SearchScope.valueOf(searchScope));
            }
            if (searchTimeLimit != null) {
                searchExecutor.setTimeLimit(Long.valueOf(searchTimeLimit));
            } else {
                searchExecutor.setTimeLimit(3000);
            }
            if (maxResultSize != null) {
                searchExecutor.setSizeLimit(Long.valueOf(maxResultSize));
            } else {
                searchExecutor.setSizeLimit(1);
            }
            if (mergeResults != null && mergeResults.booleanValue()) {
                searchExecutor.setSearchEntryHandlers(new MergeAttributeEntryHandler());
            }
            if (lowercaseAttributeNames != null && lowercaseAttributeNames.booleanValue()) {
                final CaseChangeEntryHandler entryHandler = new CaseChangeEntryHandler();
                entryHandler.setAttributeNameCaseChange(CaseChange.LOWER);
                searchExecutor.setSearchEntryHandlers(entryHandler);
            }

            List<String> returnAttrs = null;
            final Element returnAttrsElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "ReturnAttributes"));
            if (returnAttrsElement != null) {
                returnAttrs = ElementSupport.getElementContentAsList(returnAttrsElement);
                if (returnAttrs != null && !returnAttrs.isEmpty()) {
                    searchExecutor.setReturnAttributes(returnAttrs.toArray(new String[returnAttrs.size()]));
                }
            }

            String filter = "";
            final Element filterElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "FilterTemplate"));
            if (filterElement != null) {
                filter = filterElement.getTextContent().trim();
                searchExecutor.setSearchFilter(new SearchFilter(filter));
            }

            return searchExecutor;
        }

        /**
         * Creates a new connection pool from a v2 XML configuration.
         * 
         * @return connection pool
         */
        @Nullable public BlockingConnectionPool createConnectionPool() {
            final Element poolConfigElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "ConnectionPool"));
            if (poolConfigElement == null) {
                return null;
            }

            final Long blockWaitTime =
                    AttributeSupport.getDurationAttributeValueAsLong(AttributeSupport.getAttribute(poolConfigElement,
                            new QName("blockWaitTime")));
            final Long expirationTime =
                    AttributeSupport.getDurationAttributeValueAsLong(AttributeSupport.getAttribute(poolConfigElement,
                            new QName("expirationTime")));

            BlockingConnectionPool pool = null;
            final Boolean blockWhenEmpty =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement, new QName(
                            "blockWhenEmpty")));
            if (blockWhenEmpty != null) {
                if (blockWhenEmpty.booleanValue()) {
                    pool = new BlockingConnectionPool();
                } else {
                    pool = new SoftLimitConnectionPool();
                }
            } else {
                pool = new BlockingConnectionPool();
            }
            if (blockWaitTime != null) {
                pool.setBlockWaitTime(blockWaitTime);
            }
            if (expirationTime != null) {
                pool.setPruneStrategy(new IdlePruneStrategy(expirationTime / 2000, expirationTime / 1000));
            }

            final PoolConfig poolConfig = createPoolConfig();
            pool.setPoolConfig(poolConfig);

            final String validateDN = AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateDN"));
            final String validateFilter =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateFilter"));

            final SearchValidator validator = new SearchValidator();
            if (validateDN != null) {
                validator.getSearchRequest().setBaseDn(validateDN);
            }
            if (validateFilter != null) {
                validator.getSearchRequest().setSearchFilter(new SearchFilter(validateFilter));
            }
            pool.setValidator(validator);

            return pool;
        }

        /**
         * Creates a new pool config from a v2 XML configuration.
         * 
         * @return pool config
         */
        @Nullable protected PoolConfig createPoolConfig() {
            final Element poolConfigElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "ConnectionPool"));
            if (poolConfigElement == null) {
                return null;
            }

            final String minPoolSize = AttributeSupport.getAttributeValue(poolConfigElement, new QName("minPoolSize"));
            final String maxPoolSize = AttributeSupport.getAttributeValue(poolConfigElement, new QName("maxPoolSize"));
            final Boolean validatePeriodically =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(poolConfigElement,
                            new QName("validatePeriodically")));
            final Long validateTimerPeriod =
                    AttributeSupport.getDurationAttributeValueAsLong(AttributeSupport.getAttribute(poolConfigElement,
                            new QName("validateTimerPeriod")));

            final PoolConfig poolConfig = new PoolConfig();
            if (minPoolSize == null) {
                final String poolInitialSize =
                        AttributeSupport.getAttributeValue(configElement, new QName("poolInitialSize"));
                if (poolInitialSize != null) {
                    poolConfig.setMinPoolSize(Integer.parseInt(poolInitialSize));
                } else {
                    poolConfig.setMinPoolSize(0);
                }
            } else {
                poolConfig.setMinPoolSize(Integer.parseInt(minPoolSize));
            }
            if (maxPoolSize == null) {
                final String poolMaxIdleSize =
                        AttributeSupport.getAttributeValue(configElement, new QName("poolMaxIdleSize"));
                if (poolMaxIdleSize != null) {
                    poolConfig.setMaxPoolSize(Integer.parseInt(poolMaxIdleSize));
                } else {
                    poolConfig.setMaxPoolSize(3);
                }
            } else {
                poolConfig.setMaxPoolSize(Integer.parseInt(maxPoolSize));
            }
            if (validatePeriodically != null && validatePeriodically.booleanValue()) {
                poolConfig.setValidatePeriodically(true);
            }
            if (validateTimerPeriod != null) {
                poolConfig.setValidatePeriod(validateTimerPeriod / 1000);
            } else {
                poolConfig.setValidatePeriod(1800);
            }
            return poolConfig;
        }

        /**
         * Create the results cache. See {@link CacheConfigParser}.
         * 
         * @return results cache
         */
        @Nullable public Cache createCache() {
            final CacheConfigParser parser = new CacheConfigParser(configElement);
            return parser.createCache();
        }
    }
}