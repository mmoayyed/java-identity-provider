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

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.impl.dc.MappingStrategy;
import net.shibboleth.idp.attribute.resolver.impl.dc.Validator;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.LdapDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.dc.ldap.ParameterizedExecutableSearchFilterBuilder;
import net.shibboleth.idp.attribute.resolver.spring.dc.BaseDataConnectorBeanDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.DataConnectorNamespaceHandler;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchScope;
import org.ldaptive.handler.CaseChangeEntryHandler;
import org.ldaptive.handler.MergeAttributeEntryHandler;
import org.ldaptive.handler.CaseChangeEntryHandler.CaseChange;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.pool.SoftLimitConnectionPool;
import org.ldaptive.provider.ConnectionStrategy;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.SaslConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;

/** Bean definition Parser for a {@link LdapDataConnector}. */
public class LdapDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "LDAPDirectory");

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Attribute");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(LdapDataConnectorBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
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
        final BeanFactory bf = createBeanFactory(springBeans);
        final ConnectionFactory cf = bf.getBean(ConnectionFactory.class);
        final SearchExecutor se = bf.getBean(SearchExecutor.class);

        ExecutableSearchBuilder sb = getBean(bf, ExecutableSearchBuilder.class);
        if (sb == null) {
            // TODO this should use the Templated builder once the velocity engine is working
            sb = new ParameterizedExecutableSearchFilterBuilder(se.getSearchFilter().getFilter());
            log.debug("no executable search builder configured, created {}", sb);
        }

        final Validator v = getBean(bf, Validator.class);
        final MappingStrategy ms = getBean(bf, MappingStrategy.class);
        final Cache<String, Optional<Map<String, Attribute>>> cache = getBean(bf, Cache.class);
        final Boolean noResultAnError =
                AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(config, new QName(
                        "noResultIsError")));
        log.debug("parsed noResultAnError {}", noResultAnError);

        builder.addPropertyValue("connectionFactory", cf);
        builder.addPropertyValue("searchExecutor", se);
        builder.addPropertyValue("executableSearchBuilder", sb);
        if (v != null) {
            builder.addPropertyValue("validator", v);
        }
        if (ms != null) {
            builder.addPropertyValue("mappingStrategy", ms);
        }
        if (noResultAnError != null && noResultAnError.booleanValue()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        if (cache != null) {
            builder.addPropertyValue("resultCache", cache);
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

        // TODO deprecated, should throw exception?
        // final String poolInitialSize = config.getAttribute("poolInitialSize");
        // final String poolMaxIdleSize = config.getAttribute("poolMaxIdleSize");

        final Boolean noResultAnError =
                AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(config, new QName(
                        "noResultIsError")));

        final String templateEngine = config.getAttribute("templateEngine");

        final ConnectionConfig cc = v2Parser.createConnectionConfig();
        final DefaultConnectionFactory cf = new DefaultConnectionFactory(cc);
        final String connectionStrategy = AttributeSupport.getAttributeValue(config, new QName("connectionStrategy"));
        if (connectionStrategy != null) {
            final ConnectionStrategy cs = ConnectionStrategy.valueOf(connectionStrategy);
            if (cs != null) {
                cf.getProvider().getProviderConfig().setConnectionStrategy(cs);
            } else {
                cf.getProvider().getProviderConfig().setConnectionStrategy(ConnectionStrategy.ACTIVE_PASSIVE);
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
            cf.getProvider().getProviderConfig().setProperties(props);
        }

        final SearchExecutor se = v2Parser.createSearchExecutor();

        // TODO this should use the Templated builder once the velocity engine is working
        ExecutableSearchBuilder sb = new ParameterizedExecutableSearchFilterBuilder(se.getSearchFilter().getFilter());

        final BlockingConnectionPool cp = v2Parser.createConnectionPool();
        if (cp != null) {
            cp.setConnectionFactory(cf);
            cp.initialize();
            builder.addPropertyValue("connectionFactory", new PooledConnectionFactory(cp));
        } else {
            builder.addPropertyValue("connectionFactory", cf);
        }

        // TODO add support for cacheResults and ResultCache

        builder.addPropertyValue("searchExecutor", se);
        builder.addPropertyValue("executableSearchBuilder", sb);
        if (noResultAnError != null && noResultAnError.booleanValue()) {
            builder.addPropertyValue("noResultAnError", true);
        }
        builder.setInitMethodName("initialize");
    }

    /**
     * Returns the first child element of the supplied element if it exists. Otherwise null.
     * 
     * @param element to parse child elements
     * @param name of the child elements to parse
     * @return first child element or null
     */
    protected static Element getFirstChildElement(final Element element, final QName name) {
        final List<Element> elements = ElementSupport.getChildElements(element, name);
        if (elements.size() > 0) {
            return elements.get(0);
        }
        return null;
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
        public V2Parser(final Element config) {
            configElement = config;
        }

        /**
         * Creates a connection config from a v2 XML configuration.
         * 
         * @return connection config
         */
        public ConnectionConfig createConnectionConfig() {
            // TODO need the 2.0 security schema to set trust and authentication credential
            final String url = AttributeSupport.getAttributeValue(configElement, new QName("ldapURL"));
            final Boolean useStartTLS =
                    AttributeSupport.getAttributeValueAsBoolean(AttributeSupport.getAttribute(configElement, new QName(
                            "useStartTLS")));
            final String principal = AttributeSupport.getAttributeValue(configElement, new QName("principal"));
            final String principalCredential =
                    AttributeSupport.getAttributeValue(configElement, new QName("principalCredential"));
            final String authenticationType =
                    AttributeSupport.getAttributeValue(configElement, new QName("authenticationType"));

            final ConnectionConfig cc = new ConnectionConfig();
            cc.setLdapUrl(url);
            if (useStartTLS != null && useStartTLS.booleanValue()) {
                cc.setUseStartTLS(true);
            }
            final BindConnectionInitializer ci = new BindConnectionInitializer();
            if (principal != null) {
                ci.setBindDn(principal);
            }
            if (principalCredential != null) {
                ci.setBindCredential(new Credential(principalCredential));
            }
            if (authenticationType != null) {
                final Mechanism m = Mechanism.valueOf(authenticationType);
                if (m != null) {
                    final SaslConfig sc = new SaslConfig();
                    sc.setMechanism(m);
                    ci.setBindSaslConfig(sc);
                }
            }
            if (!ci.isEmpty()) {
                cc.setConnectionInitializer(ci);
            }
            return cc;
        }

        /**
         * Creates a new search executor from a v2 XML configuration.
         * 
         * @return search executor
         */
        public SearchExecutor createSearchExecutor() {
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

            final SearchExecutor se = new SearchExecutor();
            se.setBaseDn(baseDn);
            if (searchScope != null) {
                se.setSearchScope(SearchScope.valueOf(searchScope));
            }
            if (searchTimeLimit != null) {
                se.setTimeLimit(Long.valueOf(searchTimeLimit));
            } else {
                se.setTimeLimit(3000);
            }
            if (maxResultSize != null) {
                se.setSizeLimit(Long.valueOf(maxResultSize));
            } else {
                se.setSizeLimit(1);
            }
            if (mergeResults != null && mergeResults.booleanValue()) {
                se.setSearchEntryHandlers(new MergeAttributeEntryHandler());
            }
            if (lowercaseAttributeNames != null && lowercaseAttributeNames.booleanValue()) {
                final CaseChangeEntryHandler eh = new CaseChangeEntryHandler();
                eh.setAttributeNameCaseChange(CaseChange.LOWER);
                se.setSearchEntryHandlers(eh);
            }

            List<String> returnAttrs = null;
            final Element returnAttrsElement =
                    getFirstChildElement(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "ReturnAttributes"));
            if (returnAttrsElement != null) {
                returnAttrs = ElementSupport.getElementContentAsList(returnAttrsElement);
                if (returnAttrs != null && !returnAttrs.isEmpty()) {
                    se.setReturnAttributes(returnAttrs.toArray(new String[returnAttrs.size()]));
                }
            }

            String filter = "";
            final Element filterElement =
                    getFirstChildElement(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "FilterTemplate"));
            if (filterElement != null) {
                filter = filterElement.getTextContent().trim();
                se.setSearchFilter(new SearchFilter(filter));
            }
            return se;
        }

        /**
         * Creates a new connection pool from a v2 XML configuration.
         * 
         * @return connection pool
         */
        public BlockingConnectionPool createConnectionPool() {
            final Element poolConfigElement =
                    getFirstChildElement(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "ConnectionPool"));
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

            final PoolConfig pc = createPoolConfig();
            pool.setPoolConfig(pc);

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
        protected PoolConfig createPoolConfig() {
            final Element poolConfigElement =
                    getFirstChildElement(configElement, new QName(DataConnectorNamespaceHandler.NAMESPACE,
                            "ConnectionPool"));
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

            final PoolConfig pc = new PoolConfig();
            if (minPoolSize != null) {
                pc.setMinPoolSize(Integer.parseInt(minPoolSize));
            } else {
                pc.setMinPoolSize(0);
            }
            if (maxPoolSize != null) {
                pc.setMaxPoolSize(Integer.parseInt(maxPoolSize));
            } else {
                pc.setMaxPoolSize(3);
            }
            if (validatePeriodically != null && validatePeriodically.booleanValue()) {
                pc.setValidatePeriodically(true);
            }
            if (validateTimerPeriod != null) {
                pc.setValidatePeriod(validateTimerPeriod / 1000);
            } else {
                pc.setValidatePeriod(1800);
            }
            return pc;
        }
    }
}