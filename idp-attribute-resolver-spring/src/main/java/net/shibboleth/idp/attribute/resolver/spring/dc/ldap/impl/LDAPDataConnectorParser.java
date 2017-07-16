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

package net.shibboleth.idp.attribute.resolver.spring.dc.ldap.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.ConnectionFactoryValidator;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.LDAPDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.StringAttributeValueMappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.ldap.impl.TemplatedExecutableSearchFilterBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.AbstractWarningDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.CacheConfigParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.DataConnectorNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.profile.spring.factory.BasicX509CredentialFactoryBean;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.handler.CaseChangeEntryHandler;
import org.ldaptive.handler.CaseChangeEntryHandler.CaseChange;
import org.ldaptive.handler.DnAttributeEntryHandler;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Bean definition Parser for a {@link LDAPDataConnector}. <em>Note</em> That parsing the V2 configuration will set some
 * beans with hard wired defaults. See {@link #doParseV2(Element, ParserContext, BeanDefinitionBuilder)}.
 */
public class LDAPDataConnectorParser extends AbstractWarningDataConnectorParser {

    /** Schema type name - dc: (Legacy). */
    @Nonnull public static final QName
        TYPE_NAME_DC = new QName(DataConnectorNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Schema type name - resolver: . */
    @Nonnull public static final QName
        TYPE_NAME_RESOLVER = new QName(AttributeResolverNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LDAPDataConnectorParser.class);

    /** {@inheritDoc} */
    @Override protected Class<LDAPDataConnector> getNativeBeanClass() {
        return LDAPDataConnector.class;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected QName getPreferredName() {
        return TYPE_NAME_RESOLVER;
    }

    // CheckStyle: MethodLength|CyclomaticComplexity OFF
    /**
     * Parses a version 2 configuration. <br/>
     * The following automatically created & injected beans acquire hard wired defaults:
     * <ul>
     * <li>{@link SearchExecutor#setTimeLimit(long)} defaults to 3000, overridden by the "searchTimeLimit" attribute.
     * </li>
     * <li>{@link SearchExecutor#setSizeLimit(long)} defaults to 1, overridden by the "maxResultSize" attribute.</li>
     * <li>{@link SearchRequest#setBaseDn(String)} default to "", overridden by the "validateDN" attribute.</li>
     * <li>{@link SearchFilter#SearchFilter(String)} defaults to "(objectClass=*)", overridden by the "validateFilter"
     * attribute.</li>
     * <li>{@link PoolConfig#setMinPoolSize(int)} defaults to 0 if neither the attribute "poolInitialSize" nor the
     * attribute "minPoolSize" are set.</li>
     * <li>{@link PoolConfig#setMaxPoolSize(int)} defaults to 3 if neither the attribute "poolMaxIdleSize" nor the
     * attribute "maxPoolSize" are set.</li>
     * <li>{@link PoolConfig#setValidatePeriod(long)} defaults to 1800, overridden by the attribute
     * "validateTimerPeriod"</li>
     * </ul>
     * 
     * @param config LDAPDirectory containing v2 configuration
     * @param parserContext bean definition parsing context
     * @param builder to initialize
     */
    @Override protected void doV2Parse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        log.debug("{} Parsing v2 configuration {}", getLogPrefix(), config);

        final V2Parser v2Parser = new V2Parser(config, getLogPrefix());

        final BeanDefinitionBuilder connectionFactory =
                BeanDefinitionBuilder.genericBeanDefinition(DefaultConnectionFactory.class);
        connectionFactory.addConstructorArgValue(v2Parser.createConnectionConfig(parserContext));

        final BeanDefinitionBuilder provider =
                BeanDefinitionBuilder.genericBeanDefinition(DefaultConnectionFactory.getDefaultProvider().getClass());
        final BeanDefinitionBuilder providerConfig =
                BeanDefinitionBuilder.genericBeanDefinition(DefaultConnectionFactory.getDefaultProvider()
                        .getProviderConfig().getClass());
        final String connectionStrategy = AttributeSupport.getAttributeValue(config, new QName("connectionStrategy"));
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
                ElementSupport.getChildElements(config,
                        new QName(DataConnectorNamespaceHandler.NAMESPACE, "LDAPProperty"));
        propertyElements.addAll(ElementSupport.getChildElements(config,
                        new QName(AttributeResolverNamespaceHandler.NAMESPACE, "LDAPProperty")));
        for (final Element e : propertyElements) {
            props.put(AttributeSupport.getAttributeValue(e, new QName("name")),
                    AttributeSupport.getAttributeValue(e, new QName("value")));
        }
        providerConfig.addPropertyValue("properties", props);
        provider.addPropertyValue("providerConfig", providerConfig.getBeanDefinition());
        connectionFactory.addPropertyValue("provider", provider.getBeanDefinition());

        final String searchBuilderID = v2Parser.getBeanSearchBuilderID();
        if (searchBuilderID != null) {
            builder.addPropertyReference("executableSearchBuilder", searchBuilderID);
        } else {
            final BeanDefinition def = v2Parser.createTemplateBuilder();
            if (def != null) {
                builder.addPropertyValue("executableSearchBuilder", def);
            }
        }

        final BeanDefinition connectionPool = v2Parser.createConnectionPool(connectionFactory.getBeanDefinition());
        BeanDefinitionBuilder pooledConnectionFactory = null;
        if (connectionPool != null) {
            pooledConnectionFactory = BeanDefinitionBuilder.genericBeanDefinition(PooledConnectionFactory.class);
            pooledConnectionFactory.addConstructorArgValue(connectionPool);
            builder.addPropertyValue("connectionFactory", pooledConnectionFactory.getBeanDefinition());
        } else {
            builder.addPropertyValue("connectionFactory", connectionFactory.getBeanDefinition());
        }

        final BeanDefinition searchExecutor = v2Parser.createSearchExecutor();
        builder.addPropertyValue("searchExecutor", searchExecutor);

        final String mappingStrategyID = AttributeSupport.getAttributeValue(config, new QName("mappingStrategyRef"));
        if (mappingStrategyID != null) {
            builder.addPropertyReference("mappingStrategy", mappingStrategyID);
        } else {
            final BeanDefinition def = v2Parser.createMappingStrategy();
            if (def != null) {
                builder.addPropertyValue("mappingStrategy", def);
            }
        }

        final String validatorID = AttributeSupport.getAttributeValue(config, new QName("validatorRef"));
        if (validatorID != null) {
            builder.addPropertyReference("validator", validatorID);
        } else {
            if (pooledConnectionFactory != null) {
                builder.addPropertyValue("validator",
                        v2Parser.createValidator(pooledConnectionFactory.getBeanDefinition()));
            } else {
                builder.addPropertyValue("validator", v2Parser.createValidator(connectionFactory.getBeanDefinition()));
            }
        }
        
        final String resultCacheBeanID = CacheConfigParser.getBeanResultCacheID(config);
        if (null != resultCacheBeanID) {
            builder.addPropertyReference("resultsCache", resultCacheBeanID);
        } else {
            builder.addPropertyValue("resultsCache", v2Parser.createCache(parserContext));
        }

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
    }

    // Checkstyle: CyclomaticComplexity|MethodLength ON

    /**
     * Utility class for parsing v2 schema configuration.
     * 
     * <em>Note</em> That parsing the V2 configuration will set some beans with hard wired defaults. See
     * {@link #doParseV2(Element, ParserContext, BeanDefinitionBuilder)}.
     */

    protected static class V2Parser {

        /** LDAPDirectory XML element. */
        private final Element configElement;

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(V2Parser.class);
        
        /** LogPrefix of parent. */
        private final String logPrefix;

        /**
         * Creates a new V2Parser with the supplied LDAPDirectory element.
         * 
         * @param config LDAPDirectory element
         * @param prefix the parent's log prefix
         */
        public V2Parser(@Nonnull final Element config, @Nonnull final String prefix) {
            Constraint.isNotNull(config, "LDAPDirectory element cannot be null");
            configElement = config;
            logPrefix = prefix; 
            // warn about deprecated (rmeoved?) attribute
            if (AttributeSupport.hasAttribute(config, new QName("mergeResults"))) {
                DeprecationSupport.warn(ObjectType.ATTRIBUTE,  "mergeResults", prefix, null);
            }
        }

        /**
         * Creates a connection config bean definition from a v2 XML configuration.
         * 
         * @param parserContext bean definition parsing context
         * @return connection config bean definition
         */
        // CheckStyle: CyclomaticComplexity OFF
        @Nonnull public BeanDefinition createConnectionConfig(@Nonnull final ParserContext parserContext) {
            final String url = AttributeSupport.getAttributeValue(configElement, new QName("ldapURL"));
            final String useStartTLS = AttributeSupport.getAttributeValue(configElement, new QName("useStartTLS"));
            final String principal = AttributeSupport.getAttributeValue(configElement, new QName("principal"));
            final String principalCredential =
                    AttributeSupport.getAttributeValue(configElement, new QName("principalCredential"));
            final String authenticationType =
                    AttributeSupport.getAttributeValue(configElement, new QName("authenticationType"));
            final String connectTimeout =
                    AttributeSupport.getAttributeValue(configElement, new QName("connectTimeout"));
            final String responseTimeout =
                    AttributeSupport.getAttributeValue(configElement, new QName("responseTimeout"));

            final BeanDefinitionBuilder connectionConfig =
                    BeanDefinitionBuilder.genericBeanDefinition(ConnectionConfig.class);
            connectionConfig.addPropertyValue("ldapUrl", url);
            if (useStartTLS != null) {
                connectionConfig.addPropertyValue("useStartTLS", useStartTLS);
            }
            if (connectTimeout != null) {
                final BeanDefinitionBuilder timeout =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                timeout.addConstructorArgValue(connectTimeout);
                timeout.addConstructorArgValue(1);
                connectionConfig.addPropertyValue("connectTimeout", timeout.getBeanDefinition());
            } else {
                connectionConfig.addPropertyValue("connectTimeout", 3000);
            }
            if (responseTimeout != null) {
                final BeanDefinitionBuilder timeout =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                timeout.addConstructorArgValue(responseTimeout);
                timeout.addConstructorArgValue(1);
                connectionConfig.addPropertyValue("responseTimeout", timeout.getBeanDefinition());
            } else {
                connectionConfig.addPropertyValue("responseTimeout", 3000);
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
        // CheckStyle: CyclomaticComplexity ON

        /**
         * Read StartTLS trust and authentication credentials.
         * 
         * @param parserContext bean definition parsing context
         * @return credential config
         */
        @Nonnull protected BeanDefinition createCredentialConfig(@Nonnull final ParserContext parserContext) {
            final BeanDefinitionBuilder result =
                    BeanDefinitionBuilder.genericBeanDefinition(CredentialConfigFactoryBean.class);

            final List<Element> trustElements =
                    ElementSupport.getChildElements(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "StartTLSTrustCredential"));
            trustElements.addAll(ElementSupport.getChildElements(configElement, new QName(
                    AttributeResolverNamespaceHandler.NAMESPACE, "StartTLSTrustCredential")));
            final String trustResource =
                    StringSupport.trimOrNull(AttributeSupport.getAttributeValue(configElement, null, "trustFile"));
            if (trustResource != null) {
                if (!trustElements.isEmpty()) {
                    log.warn("{} StartTLSTrustCredential and trustFile= are incompatible.  trustFile used.",
                            getLogPrefix());
                }
                final BeanDefinitionBuilder credential =
                        BeanDefinitionBuilder.genericBeanDefinition(BasicX509CredentialFactoryBean.class);
                credential.addPropertyValue("certificateResource", trustResource);
                result.addPropertyValue("trustCredential", credential.getBeanDefinition());
            } else if (!trustElements.isEmpty()) {
                if (trustElements.size() > 1) {
                    log.warn("{} Too many StartTLSTrustCredential elements in {}; only the first has been consulted",
                            getLogPrefix(), parserContext.getReaderContext().getResource().getDescription());
                }
                result.addPropertyValue("trustCredential",
                        SpringSupport.parseCustomElements(trustElements, parserContext).get(0));
            }

            final List<Element> authElements =
                    ElementSupport.getChildElements(configElement,
                            new QName(DataConnectorNamespaceHandler.NAMESPACE, "StartTLSAuthenticationCredential"));
            authElements.addAll(ElementSupport.getChildElements(configElement,
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "StartTLSAuthenticationCredential")));
            if (!authElements.isEmpty()) {
                if (authElements.size() > 1) {
                    log.warn("{} Too many StartTLSAuthenticationCredential elements in {};"
                            + " only the first has been consulted", getLogPrefix(), 
                            parserContext.getReaderContext().getResource().getDescription());
                }
                result.addPropertyValue("authCredential", SpringSupport
                        .parseCustomElements(authElements, parserContext).get(0));
            }

            return result.getBeanDefinition();
        }
        
        /**
         * Get the textual content of the &lt;FilterTemplate&gt;.
         * 
         * We have to look in two places and warn appropriately.
         * @return the filter or null.
         */
        @Nullable private String getFilterText() {
            final List<Element> filterElements = ElementSupport.getChildElements(configElement,
                    new QName(DataConnectorNamespaceHandler.NAMESPACE, "FilterTemplate"));
            filterElements.addAll(ElementSupport.getChildElements(configElement,
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "FilterTemplate")));
            
            final String filter;
            if (!filterElements.isEmpty()) {
                if (filterElements.size() > 1) {
                    log.warn("{} only one <FilterTemplate> can be specified; only the first has been consulted",
                            getLogPrefix());
                }
                filter = StringSupport.trimOrNull(filterElements.get(0).getTextContent().trim());
            } else {
                filter = null;
            }
            return filter;
        }

        /**
         * Get the bean ID of an externally defined search builder.
         * 
         * @return search builder bean ID
         */
        @Nullable public String getBeanSearchBuilderID() {
            return AttributeSupport.getAttributeValue(configElement, null, "executableSearchBuilderRef");
        }
        
        /**
         * Construct the definition of the template driven search builder.
         * 
         * @return the bean definition for the template search builder.
         */
        @Nonnull public BeanDefinition createTemplateBuilder() {
            final BeanDefinitionBuilder templateBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(TemplatedExecutableSearchFilterBuilder.class);
            templateBuilder.setInitMethodName("initialize");

            String velocityEngineRef = StringSupport.trimOrNull(configElement.getAttribute("templateEngine"));
            if (null == velocityEngineRef) {
                velocityEngineRef = "shibboleth.VelocityEngine";
            }
            templateBuilder.addPropertyReference("velocityEngine", velocityEngineRef);

            templateBuilder.addPropertyValue("v2Compatibility", true);

            templateBuilder.addPropertyValue("templateText", getFilterText());

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
            final String lowercaseAttributeNames =
                    AttributeSupport.getAttributeValue(configElement, new QName("lowercaseAttributeNames"));

            final BeanDefinitionBuilder searchExecutor =
                    BeanDefinitionBuilder.genericBeanDefinition(SearchExecutor.class);
            if (baseDn != null) {
                searchExecutor.addPropertyValue("baseDn", baseDn);
            }
            if (searchScope != null) {
                searchExecutor.addPropertyValue("searchScope", searchScope);
            }
            if (searchTimeLimit != null) {
                final BeanDefinitionBuilder duration =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildDuration");
                duration.addConstructorArgValue(searchTimeLimit);
                duration.addConstructorArgValue(1);
                searchExecutor.addPropertyValue("timeLimit", duration.getBeanDefinition());
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
            handlers.addConstructorArgValue(lowercaseAttributeNames);
            searchExecutor.addPropertyValue("searchEntryHandlers", handlers.getBeanDefinition());

            final List<Element> returnAttrsElements = ElementSupport.getChildElements(configElement, 
                    new QName(DataConnectorNamespaceHandler.NAMESPACE, "ReturnAttributes"));
            returnAttrsElements.addAll(ElementSupport.getChildElements(configElement, 
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ReturnAttributes")));
            
            if (!returnAttrsElements.isEmpty()) {
                if (returnAttrsElements.size() > 1) {
                    log.warn("{} Only one <ReturnAttributes> element can be specified; "+
                            "only the first has been consulted.", getLogPrefix());
                }
                final Element returnAttrsElement = returnAttrsElements.get(0);
                
                final BeanDefinitionBuilder returnAttrs =
                        BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildStringList");
                returnAttrs.addConstructorArgValue(ElementSupport.getElementContentAsString(returnAttrsElement));
                searchExecutor.addPropertyValue("returnAttributes", returnAttrs.getBeanDefinition());
            }

            return searchExecutor.getBeanDefinition();
        }

        /** Get the Pool configuration &lt;ConnectionPool&gt; element contents, warning if there is more than one.
         * @return the &lt;ConnectionPool&gt; or null if there isn't one.
         */
        @Nullable Element getConnectionPoolElement() {
            final List<Element> poolConfigElements =
                    ElementSupport.getChildElements(configElement,
                            new QName(DataConnectorNamespaceHandler.NAMESPACE, "ConnectionPool"));
            poolConfigElements.addAll(ElementSupport.getChildElements(configElement,
                    new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ConnectionPool")));
            if (poolConfigElements.isEmpty()) {
                return null;
            }
            if (poolConfigElements.size() > 1) {
                log.warn("{} Only one <ConnectionPool> should be specified; only the first has been consulted.",
                        getLogPrefix());
            }

            return poolConfigElements.get(0);
        }
        
        // CheckStyle: CyclomaticComplexity ON

        /**
         * Creates a new connection pool bean definition from a v2 XML configuration.
         * 
         * @param connectionFactory used by the connection pool
         * 
         * @return connection pool bean definition
         */
        // CheckStyle: MethodLength OFF
        @Nullable public BeanDefinition createConnectionPool(final BeanDefinition connectionFactory) {

            final Element poolConfigElement = getConnectionPoolElement();
            if (null == poolConfigElement) {
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

            final BeanDefinitionBuilder validator =
                    BeanDefinitionBuilder.rootBeanDefinition(V2Parser.class, "buildSearchValidator");
            validator.addConstructorArgValue(
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validatePeriodically")));
            validator.addConstructorArgValue(
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateDN")));
            validator.addConstructorArgValue(
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("validateFilter")));
            pool.addPropertyValue("validator", validator.getBeanDefinition());

            pool.addPropertyValue("connectionFactory", connectionFactory);
            final String failFastInitialize =
                    AttributeSupport.getAttributeValue(poolConfigElement, new QName("failFastInitialize"));
            if (failFastInitialize != null) {
                pool.addPropertyValue("failFastInitialize", failFastInitialize);
            }
            pool.setInitMethodName("initialize");
            return pool.getBeanDefinition();
        }

        // CheckStyle: MethodLength ON

        /**
         * Creates a new pool config bean definition from a v2 XML configuration.
         * 
         * @return pool config bean definition
         */
        @Nullable protected BeanDefinition createPoolConfig() {
            final Element poolConfigElement = getConnectionPoolElement();
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
         * Create the result mapping strategy. See {@link net.shibboleth.idp.attribute.resolver.dc.MappingStrategy}.
         * 
         * @return mapping strategy
         */
        @Nullable public BeanDefinition createMappingStrategy() {

            final BeanDefinitionBuilder mapper =
                    BeanDefinitionBuilder.genericBeanDefinition(StringAttributeValueMappingStrategy.class);
            final List<Element> columns =
                    ElementSupport.getChildElementsByTagNameNS(configElement, 
                            DataConnectorNamespaceHandler.NAMESPACE, "Column");
            columns.addAll(ElementSupport.getChildElementsByTagNameNS(configElement,
                            AttributeResolverNamespaceHandler.NAMESPACE, "Column"));

            if (!columns.isEmpty()) {
                final ManagedMap renamingMap = new ManagedMap();
                for (final Element column : columns) {
                    final String columnName = AttributeSupport.getAttributeValue(column, null, "columnName");
                    final String attributeId = AttributeSupport.getAttributeValue(column, null, "attributeID");
                    if (columnName != null && attributeId != null) {
                        renamingMap.put(columnName, attributeId);
                    }

                    if (AttributeSupport.hasAttribute(column, new QName("type"))) {
                        log.warn("{} Column type attribute not supported for LDAP results", getLogPrefix());
                    }
                }
                mapper.addPropertyValue("resultRenamingMap", renamingMap);
            }

            final String noResultIsError =
                    AttributeSupport.getAttributeValue(configElement, new QName("noResultIsError"));
            if (noResultIsError != null) {
                mapper.addPropertyValue("noResultAnError", noResultIsError);
            }

            final String multipleResultsIsError =
                    AttributeSupport.getAttributeValue(configElement, new QName("multipleResultsIsError"));
            if (multipleResultsIsError != null) {
                mapper.addPropertyValue("multipleResultsAnError", multipleResultsIsError);
            }
            return mapper.getBeanDefinition();
        }

        /**
         * Create the validator. See {@link net.shibboleth.idp.attribute.resolver.dc.Validator}.
         * 
         * @param connectionFactory to provide to the validator
         * 
         * @return validator
         */
        @Nullable public BeanDefinition createValidator(final BeanDefinition connectionFactory) {

            final BeanDefinitionBuilder validator =
                    BeanDefinitionBuilder.genericBeanDefinition(ConnectionFactoryValidator.class);

            validator.addPropertyValue("connectionFactory", connectionFactory);
            return validator.getBeanDefinition();
        }

        /**
         * Create a results cache bean definition. See {@link CacheConfigParser}.
         * 
         * @param parserContext bean parser context
         * 
         * @return results cache bean definition
         */
        @Nullable public BeanDefinition createCache(@Nonnull final ParserContext parserContext) {
            final CacheConfigParser parser = new CacheConfigParser(configElement);
            return parser.createCache(parserContext);
        }
        
        /** The parent's log prefix.
         * @return the log prefix.  Set up in the constructor.
         */
        @Nonnull String getLogPrefix() {
            return logPrefix;
        }

        /**
         * Converts the supplied duration to milliseconds and divides it by the divisor. Useful for modifying durations
         * while resolving property replacement.
         * 
         * @param duration the duration (which may have gone through spring translation from iso to long)
         * @param divisor to modify the duration with
         * 
         * @return result of the division
         */
        @Duration public static long buildDuration(@Duration final long duration, final long divisor) {
            return duration / divisor;
        } 

        /**
         * Converts the supplied value to a list of strings delimited by {@link XMLConstants#LIST_DELIMITERS} and comma.
         * 
         * @param value to convert to a list
         * 
         * @return list of strings
         */
        @Nonnull public static List<String> buildStringList(final String value) {
            return StringSupport.stringToList(value, XMLConstants.LIST_DELIMITERS + ",");
        }

        /**
         * Returns a soft limit connection pool if blockWhenEmpty is false, otherwise return a blocking connection pool.
         * 
         * @param blockWhenEmpty boolean string indicating the type of blocking connection pool
         * 
         * @return soft limit or blocking connection pool
         */
        @Nonnull public static BlockingConnectionPool buildConnectionPool(@Nullable final String blockWhenEmpty) {
            BlockingConnectionPool pool = null;
            if (blockWhenEmpty == null || Boolean.valueOf(blockWhenEmpty)) {
                pool = new BlockingConnectionPool();
            } else {
                pool = new SoftLimitConnectionPool();
            }
            pool.setName("resolver-pool");
            return pool;
        }

        /**
         * Returns a search validator or null if validatePeriodically is false.
         *
         * @param validatePeriodically whether to create a search validator
         * @param validateDN baseDN to search on
         * @param validateFilter to search with
         *
         * @return  search validator or null
         */
        @Nullable public static SearchValidator buildSearchValidator(@Nullable final String validatePeriodically,
                @Nullable final String validateDN, @Nullable final String validateFilter) {
            if (!Boolean.valueOf(validatePeriodically)) {
                return null;
            }
            final SearchRequest searchRequest = new SearchRequest();
            searchRequest.setReturnAttributes("1.1");
            searchRequest.setSearchScope(SearchScope.OBJECT);
            searchRequest.setSizeLimit(1);
            if (validateDN != null) {
                searchRequest.setBaseDn(validateDN);
            } else {
                searchRequest.setBaseDn("");
            }
            final SearchFilter searchFilter = new SearchFilter();
            if (validateFilter != null) {
                searchFilter.setFilter(validateFilter);
            } else {
                searchFilter.setFilter("(objectClass=*)");
            }
            searchRequest.setSearchFilter(searchFilter);
            final SearchValidator validator = new SearchValidator();
            validator.setSearchRequest(searchRequest);
            return validator;
        }

        /**
         * Factory method for handling spring property replacement. Adds a {@link DnAttributeEntryHandler} by default.
         * Adds a {@link CaseChangeEntryHandler} if lowercaseAttributeNames is true. 
         * 
         * @param lowercaseAttributeNames boolean string value
         * @return list of search entry handlers
         */
        @Nonnull public static List<SearchEntryHandler> buildSearchEntryHandlers(
                @Nullable final String lowercaseAttributeNames) {
            final List<SearchEntryHandler> handlers = new ArrayList<>();
            handlers.add(new DnAttributeEntryHandler());
            if (Boolean.valueOf(lowercaseAttributeNames)) {
                final CaseChangeEntryHandler entryHandler = new CaseChangeEntryHandler();
                entryHandler.setAttributeNameCaseChange(CaseChange.LOWER);
                handlers.add(entryHandler);
            }
            return handlers;
        }
    }

}