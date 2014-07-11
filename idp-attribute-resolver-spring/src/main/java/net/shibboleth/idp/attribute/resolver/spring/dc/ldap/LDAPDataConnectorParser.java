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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.LDAPDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.TemplatedExecutableSearchFilterBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.DataConnectorNamespaceHandler;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.handler.CaseChangeEntryHandler;
import org.ldaptive.handler.CaseChangeEntryHandler.CaseChange;
import org.ldaptive.handler.MergeAttributeEntryHandler;
import org.ldaptive.handler.SearchEntryHandler;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.pool.SoftLimitConnectionPool;
import org.ldaptive.provider.ConnectionStrategy;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.SaslConfig;
import org.ldaptive.ssl.SslConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.collect.Lists;

/** Bean definition Parser for a {@link LDAPDataConnector}. */
public class LDAPDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Attribute");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(LDAPDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<LDAPDataConnector> getBeanClass(@Nullable final Element element) {
        return LDAPDataConnector.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        log.debug("doParse {}", config);

        final String springResources = AttributeSupport.getAttributeValue(config, new QName("springResources"));
        if (springResources == null) {
            log.debug("parsing v2 configuration");
            doParseV2(config, parserContext, builder);
        } else {
            doParseInternal(config, createBeanFactory(springResources.split(";")), builder);
        }

    }

    /**
     * Parses a Spring <beans/> configuration.
     * 
     * @param config LDAPDirectory containing Spring configuration
     * @param beanFactory containing spring beans
     * @param builder to initialize
     */
    protected void doParseInternal(@Nonnull final Element config, @Nonnull final BeanFactory beanFactory,
            @Nonnull final BeanDefinitionBuilder builder) {

        addPropertyDescriptorValues(builder, beanFactory, LDAPDataConnector.class);

        final String noResultAnError = AttributeSupport.getAttributeValue(config, new QName("noResultIsError"));
        log.debug("parsed noResultAnError {}", noResultAnError);
        if (noResultAnError != null) {
            builder.addPropertyValue("noResultAnError", noResultAnError);
        }
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
    }

    /**
     * Parses a version 2 configuration.
     * 
     * @param config LDAPDirectory containing v2 configuration
     * @param parserContext bean definition parsing context
     * @param builder to initialize
     */
    protected void doParseV2(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {

        final V2Parser v2Parser = new V2Parser(config);

        final BeanDefinitionBuilder connectionFactory =
                BeanDefinitionBuilder.genericBeanDefinition(DefaultConnectionFactory.class);
        connectionFactory.addConstructorArgValue(v2Parser.createConnectionConfig(parserContext));

        final BeanDefinitionBuilder provider =
                BeanDefinitionBuilder.genericBeanDefinition(DefaultConnectionFactory.getDefaultProvider().getClass());
        final BeanDefinitionBuilder providerConfig =
                BeanDefinitionBuilder.genericBeanDefinition(DefaultConnectionFactory.getDefaultProvider()
                        .getProviderConfig().getClass());
        String connectionStrategy = AttributeSupport.getAttributeValue(config, new QName("connectionStrategy"));
        if (connectionStrategy == null) {
            providerConfig.addPropertyValue("connectionStrategy", ConnectionStrategy.ACTIVE_PASSIVE);
        } else {
            switch (connectionStrategy) {
                case "DEFAULT":
                    providerConfig.addPropertyValue("connectionStrategy", ConnectionStrategy.DEFAULT);
                    break;

                case "ROUND_ROBIN":
                    providerConfig.addPropertyValue("connectionStrategy", ConnectionStrategy.ROUND_ROBIN);
                    break;

                case "RANDOM":
                    providerConfig.addPropertyValue("connectionStrategy", ConnectionStrategy.RANDOM);
                    break;

                default:
                    providerConfig.addPropertyValue("connectionStrategy", ConnectionStrategy.ACTIVE_PASSIVE);
                    break;
            }
        }

        final ManagedMap<String, String> props = new ManagedMap<>();
        final List<Element> propertyElements =
                ElementSupport.getChildElements(config, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                        "LDAPProperty"));
        for (Element e : propertyElements) {
            props.put(AttributeSupport.getAttributeValue(e, new QName("name")),
                    AttributeSupport.getAttributeValue(e, new QName("value")));
        }
        providerConfig.addPropertyValue("properties", props);
        provider.addPropertyValue("providerConfig", providerConfig.getBeanDefinition());
        connectionFactory.addPropertyValue("provider", provider.getBeanDefinition());

        builder.addPropertyValue("executableSearchBuilder", v2Parser.createTemplatedExecutableSearchFilterBuilder());

        final BeanDefinition connectionPool = v2Parser.createConnectionPool(connectionFactory.getBeanDefinition());
        if (connectionPool != null) {
            final BeanDefinitionBuilder pooledConnectionFactory =
                    BeanDefinitionBuilder.genericBeanDefinition(PooledConnectionFactory.class);
            pooledConnectionFactory.addConstructorArgValue(connectionPool);
            builder.addPropertyValue("connectionFactory", pooledConnectionFactory.getBeanDefinition());
        } else {
            builder.addPropertyValue("connectionFactory", connectionFactory.getBeanDefinition());
        }

        final BeanDefinition searchExecutor = v2Parser.createSearchExecutor();
        builder.addPropertyValue("searchExecutor", searchExecutor);

        builder.addPropertyValue("resultsCache", v2Parser.createCache());

        final String noResultIsError = AttributeSupport.getAttributeValue(config, new QName("noResultIsError"));
        if (noResultIsError != null) {
            builder.addPropertyValue("noResultAnError", noResultIsError);
        }
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
    }

    /** Utility class for parsing v2 schema configuration. */
    protected static class V2Parser {

        /** LDAPDirectory XML element. */
        private final Element configElement;

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(V2Parser.class);

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
         * Creates a connection config bean definition from a v2 XML configuration.
         * 
         * @param parserContext bean definition parsing context
         * @return connection config bean definition
         */
        @Nonnull public BeanDefinition createConnectionConfig(@Nonnull final ParserContext parserContext) {
            final String url = AttributeSupport.getAttributeValue(configElement, new QName("ldapURL"));
            final String useStartTLS = AttributeSupport.getAttributeValue(configElement, new QName("useStartTLS"));
            final String principal = AttributeSupport.getAttributeValue(configElement, new QName("principal"));
            final String principalCredential =
                    AttributeSupport.getAttributeValue(configElement, new QName("principalCredential"));
            final String authenticationType =
                    AttributeSupport.getAttributeValue(configElement, new QName("authenticationType"));

            final BeanDefinitionBuilder connectionConfig =
                    BeanDefinitionBuilder.genericBeanDefinition(ConnectionConfig.class);
            connectionConfig.addPropertyValue("ldapUrl", url);
            if (useStartTLS != null) {
                connectionConfig.addPropertyValue("useStartTLS", useStartTLS);
            }
            final BeanDefinitionBuilder sslConfig = BeanDefinitionBuilder.genericBeanDefinition(SslConfig.class);
            sslConfig.addPropertyValue("credentialConfig", createCredentialConfig(parserContext));
            connectionConfig.addPropertyValue("sslConfig", sslConfig.getBeanDefinition());
            final BeanDefinitionBuilder connectionInitializer =
                    BeanDefinitionBuilder.genericBeanDefinition(BindConnectionInitializer.class);
            if (principal != null) {
                connectionInitializer.addPropertyValue("bindDn", principal);
            }
            if (principalCredential != null) {
                final BeanDefinitionBuilder credential = BeanDefinitionBuilder.genericBeanDefinition(Credential.class);
                credential.addConstructorArgValue(principalCredential);
                connectionInitializer.addPropertyValue("bindCredential", credential.getBeanDefinition());
            }
            if (authenticationType != null) {
                final Mechanism mechanism = Mechanism.valueOf(authenticationType);
                if (mechanism != null) {
                    final SaslConfig config = new SaslConfig();
                    config.setMechanism(mechanism);
                    connectionInitializer.addPropertyValue("bindSaslConfig", config);
                }
            }
            if (principal != null || principalCredential != null || authenticationType != null) {
                connectionConfig.addPropertyValue("connectionInitializer", connectionInitializer.getBeanDefinition());
            }
            return connectionConfig.getBeanDefinition();
        }

        /**
         * Uses {@link X509CredentialSupport} to read StartTLS trust and authentication credentials.
         * 
         * @param parserContext bean definition parsing context
         * @return credential config
         */
        @Nonnull protected BeanDefinition createCredentialConfig(@Nonnull final ParserContext parserContext) {
            BeanDefinitionBuilder result =
                    BeanDefinitionBuilder.genericBeanDefinition(CredentialConfigFactoryBean.class);

            final List<Element> trustElements =
                    ElementSupport.getChildElements(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "StartTLSTrustCredential"));
            if (trustElements != null && !trustElements.isEmpty()) {
                if (trustElements.size() > 1) {
                    log.warn("Too many StartTLSTrustCredential elements in {}; only the first has been consulted",
                            parserContext.getReaderContext().getResource().getDescription());
                }
                result.addPropertyValue("trustCredential",
                        SpringSupport.parseCustomElements(trustElements, parserContext).get(0));
            }

            final List<Element> authElements =
                    ElementSupport.getChildElements(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "StartTLSAuthenticationCredential"));

            if (authElements != null && !authElements.isEmpty()) {
                if (authElements.size() > 1) {
                    log.warn("Too many StartTLSAuthenticationCredential elements in {};"
                            + " only the first has been consulted", parserContext.getReaderContext().getResource()
                            .getDescription());
                }
                result.addPropertyValue("authCredential",
                        SpringSupport.parseCustomElements(authElements, parserContext).get(0));
            }

            return result.getBeanDefinition();
        }

        /**
         * Construct the definition of the template driven search builder.
         * 
         * @return the bean definition for the template search builder.
         */
        @Nonnull public BeanDefinition createTemplatedExecutableSearchFilterBuilder() {
            final BeanDefinitionBuilder templateBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(TemplatedExecutableSearchFilterBuilder.class);
            templateBuilder.setInitMethodName("initialize");

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

            return templateBuilder.getBeanDefinition();
        }

        /**
         * Creates a new search executor bean definition from a v2 XML configuration.
         * 
         * @return search executor bean definition
         */
        // CheckStyle: CyclomaticComplexity OFF
        @Nonnull public BeanDefinition createSearchExecutor() {
            final String baseDn = AttributeSupport.getAttributeValue(configElement, new QName("baseDN"));
            final String searchScope = AttributeSupport.getAttributeValue(configElement, new QName("searchScope"));
            final String searchTimeLimit =
                    AttributeSupport.getAttributeValue(configElement, new QName("searchTimeLimit"));
            final String maxResultSize = AttributeSupport.getAttributeValue(configElement, new QName("maxResultSize"));
            final String mergeResults = AttributeSupport.getAttributeValue(configElement, new QName("mergeResults"));
            final String lowercaseAttributeNames =
                    AttributeSupport.getAttributeValue(configElement, new QName("lowercaseAttributeNames"));

            final BeanDefinitionBuilder searchExecutor =
                    BeanDefinitionBuilder.genericBeanDefinition(SearchExecutor.class);
            searchExecutor.addPropertyValue("baseDn", baseDn);
            if (searchScope != null) {
                searchExecutor.addPropertyValue("searchScope", searchScope);
            }
            if (searchTimeLimit != null) {
                searchExecutor.addPropertyValue("timeLimit", searchTimeLimit);
            } else {
                searchExecutor.addPropertyValue("timeLimit", 3000);
            }
            if (maxResultSize != null) {
                searchExecutor.addPropertyValue("sizeLimit", maxResultSize);
            } else {
                searchExecutor.addPropertyValue("sizeLimit", 1);
            }

            final BeanDefinitionBuilder handlers =
                    BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildSearchEntryHandlers");
            handlers.addConstructorArgValue(mergeResults);
            handlers.addConstructorArgValue(lowercaseAttributeNames);
            searchExecutor.addPropertyValue("searchEntryHandlers", handlers.getBeanDefinition());

            List<String> returnAttrs = null;
            final Element returnAttrsElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "ReturnAttributes"));
            if (returnAttrsElement != null) {
                returnAttrs = ElementSupport.getElementContentAsList(returnAttrsElement);
                if (returnAttrs != null && !returnAttrs.isEmpty()) {
                    searchExecutor.addPropertyValue("returnAttributes", returnAttrs);
                }
            }

            final Element filterElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "FilterTemplate"));
            if (filterElement != null) {
                final BeanDefinitionBuilder searchFilter =
                        BeanDefinitionBuilder.genericBeanDefinition(SearchFilter.class);
                searchFilter.addConstructorArgValue(filterElement.getTextContent().trim());
                searchExecutor.addPropertyValue("searchFilter", searchFilter.getBeanDefinition());
            }

            return searchExecutor.getBeanDefinition();
        }

        // CheckStyle: CyclomaticComplexity ON

        /**
         * Creates a new connection pool bean definition from a v2 XML configuration.
         * 
         * @param connectionFactory used by the connection pool
         * 
         * @return connection pool bean definition
         */
        @Nullable public BeanDefinition createConnectionPool(final BeanDefinition connectionFactory) {
            final Element poolConfigElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "ConnectionPool"));
            if (poolConfigElement == null) {
                return null;
            }

            final String blockWaitTime =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("blockWaitTime"));
            final String expirationTime =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("expirationTime"));

            final BeanDefinitionBuilder pool =
                    BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildConnectionPool");
            pool.addConstructorArgValue(AttributeSupport.getAttributeValue(configElement, new QName("blockWhenEmpty")));
            if (blockWaitTime != null) {
                final BeanDefinitionBuilder duration =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                duration.addConstructorArgValue(blockWaitTime);
                duration.addConstructorArgValue(1);
                pool.addPropertyValue("blockWaitTime", duration.getBeanDefinition());
            }
            if (expirationTime != null) {
                final BeanDefinitionBuilder period =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                period.addConstructorArgValue(expirationTime);
                period.addConstructorArgValue(2000);
                final BeanDefinitionBuilder idle =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                idle.addConstructorArgValue(expirationTime);
                idle.addConstructorArgValue(1000);
                final BeanDefinitionBuilder strategy =
                        BeanDefinitionBuilder.genericBeanDefinition(IdlePruneStrategy.class);
                strategy.addConstructorArgValue(period.getBeanDefinition());
                strategy.addConstructorArgValue(idle.getBeanDefinition());
                pool.addPropertyValue("pruneStrategy", strategy.getBeanDefinition());
            }

            pool.addPropertyValue("poolConfig", createPoolConfig());

            final String validateDN = AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateDN"));
            final String validateFilter =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateFilter"));

            final BeanDefinitionBuilder searchRequest =
                    BeanDefinitionBuilder.genericBeanDefinition(SearchRequest.class);
            if (validateDN != null) {
                searchRequest.addPropertyValue("baseDn", validateDN);
            }
            if (validateFilter != null) {
                final BeanDefinitionBuilder searchFilter =
                        BeanDefinitionBuilder.genericBeanDefinition(SearchFilter.class);
                searchFilter.addConstructorArgValue(validateFilter);
                searchRequest.addPropertyValue("searchFilter", searchFilter.getBeanDefinition());
            }
            final BeanDefinitionBuilder validator = BeanDefinitionBuilder.genericBeanDefinition(SearchValidator.class);
            validator.addPropertyValue("searchRequest", searchRequest.getBeanDefinition());
            pool.addPropertyValue("validator", validator.getBeanDefinition());

            pool.addPropertyValue("connectionFactory", connectionFactory);
            pool.setInitMethodName("initialize");
            return pool.getBeanDefinition();
        }

        /**
         * Creates a new pool config bean definition from a v2 XML configuration.
         * 
         * @return pool config bean definition
         */
        @Nullable protected BeanDefinition createPoolConfig() {
            final Element poolConfigElement =
                    ElementSupport.getFirstChildElement(configElement, new QName(
                            DataConnectorNamespaceHandler.NAMESPACE, "ConnectionPool"));
            if (poolConfigElement == null) {
                return null;
            }

            final String minPoolSize = AttributeSupport.getAttributeValue(poolConfigElement, new QName("minPoolSize"));
            final String maxPoolSize = AttributeSupport.getAttributeValue(poolConfigElement, new QName("maxPoolSize"));
            final String validatePeriodically =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validatePeriodically"));
            final String validateTimerPeriod =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateTimerPeriod"));

            final BeanDefinitionBuilder poolConfig = BeanDefinitionBuilder.genericBeanDefinition(PoolConfig.class);
            if (minPoolSize == null) {
                final String poolInitialSize =
                        AttributeSupport.getAttributeValue(configElement, new QName("poolInitialSize"));
                if (poolInitialSize != null) {
                    poolConfig.addPropertyValue("minPoolSize", poolInitialSize);
                } else {
                    poolConfig.addPropertyValue("minPoolSize", 0);
                }
            } else {
                poolConfig.addPropertyValue("minPoolSize", minPoolSize);
            }
            if (maxPoolSize == null) {
                final String poolMaxIdleSize =
                        AttributeSupport.getAttributeValue(configElement, new QName("poolMaxIdleSize"));
                if (poolMaxIdleSize != null) {
                    poolConfig.addPropertyValue("maxPoolSize", poolMaxIdleSize);
                } else {
                    poolConfig.addPropertyValue("maxPoolSize", 3);
                }
            } else {
                poolConfig.addPropertyValue("maxPoolSize", maxPoolSize);
            }
            if (validatePeriodically != null) {
                poolConfig.addPropertyValue("validatePeriodically", validatePeriodically);
            }
            if (validateTimerPeriod != null) {
                final BeanDefinitionBuilder period =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                period.addConstructorArgValue(validateTimerPeriod);
                period.addConstructorArgValue(1000);
                poolConfig.addPropertyValue("validatePeriod", period.getBeanDefinition());
            } else {
                poolConfig.addPropertyValue("validatePeriod", 1800);
            }
            return poolConfig.getBeanDefinition();
        }

        /**
         * Create a results cache bean definition. See {@link CacheConfigParser}.
         * 
         * @return results cache bean definition
         */
        @Nullable public BeanDefinition createCache() {
            final CacheConfigParser parser = new CacheConfigParser(configElement);
            return parser.createCache();
        }

        /**
         * Converts the supplied duration to milliseconds and divides it by the divisor. Useful for modifying durations
         * while resolving property replacement.
         * 
         * @param duration string format
         * @param divisor to modify the duration with
         * 
         * @return result of the division
         */
        public static long buildDuration(final String duration, final long divisor) {
            return DOMTypeSupport.durationToLong(duration) / divisor;
        }

        /**
         * Returns a soft limit connection pool if blockWhenEmpty is false, otherwise return a blocking connection pool.
         * 
         * @param blockWhenEmpty boolean string indicating the type of blocking connection pool
         * 
         * @return soft limit or blocking connection pool
         */
        public static BlockingConnectionPool buildConnectionPool(final String blockWhenEmpty) {
            BlockingConnectionPool pool = null;
            if (blockWhenEmpty == null || Boolean.valueOf(blockWhenEmpty)) {
                pool = new BlockingConnectionPool();
            } else {
                pool = new SoftLimitConnectionPool();
            }
            return pool;
        }

        /**
         * Factory method for handling spring property replacement.
         * 
         * @param mergeResults boolean string value
         * @param lowercaseAttributeNames boolean string value
         * @return possibly empty list of search entry handlers
         */
        public static List<SearchEntryHandler> buildSearchEntryHandlers(final String mergeResults,
                final String lowercaseAttributeNames) {
            final List<SearchEntryHandler> handlers = Lists.newArrayList();
            if (Boolean.valueOf(mergeResults)) {
                handlers.add(new MergeAttributeEntryHandler());
            }
            if (Boolean.valueOf(lowercaseAttributeNames)) {
                final CaseChangeEntryHandler entryHandler = new CaseChangeEntryHandler();
                entryHandler.setAttributeNameCaseChange(CaseChange.LOWER);
                handlers.add(entryHandler);
            }
            return handlers;
        }
    }
}