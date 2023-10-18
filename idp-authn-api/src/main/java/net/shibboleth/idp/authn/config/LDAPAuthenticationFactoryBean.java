/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.authn.config;

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Period;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import net.shibboleth.idp.authn.TemplateSearchDnResolver;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

import org.apache.velocity.app.VelocityEngine;
import org.ldaptive.ActivePassiveConnectionStrategy;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionInitializer;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.FilterTemplate;
import org.ldaptive.PooledConnectionFactory;
import org.ldaptive.RandomConnectionStrategy;
import org.ldaptive.RoundRobinConnectionStrategy;
import org.ldaptive.SearchConnectionValidator;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.SimpleBindRequest;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.FormatDnResolver;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;
import org.ldaptive.auth.SearchEntryResolver;
import org.ldaptive.auth.ext.ActiveDirectoryAuthenticationResponseHandler;
import org.ldaptive.auth.ext.EDirectoryAuthenticationResponseHandler;
import org.ldaptive.auth.ext.FreeIPAAuthenticationResponseHandler;
import org.ldaptive.auth.ext.PasswordExpirationAuthenticationResponseHandler;
import org.ldaptive.auth.ext.PasswordPolicyAuthenticationRequestHandler;
import org.ldaptive.auth.ext.PasswordPolicyAuthenticationResponseHandler;
import org.ldaptive.pool.BindConnectionPassivator;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.ConnectionPassivator;
import org.ldaptive.ssl.AllowAnyHostnameVerifier;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.slf4j.Logger;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/** LDAP Authentication configuration. See ldap-authn-config.xml */
public class LDAPAuthenticationFactoryBean extends AbstractFactoryBean<Authenticator> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LDAPAuthenticationFactoryBean.class);

    /** Enum that defines authenticator configuration. Labels maps to values in ldap.properties. */
    public enum AuthenticatorType {
        
        /** Anonymous bind. */
        ANON_SEARCH("anonSearchAuthenticator"),
        
        /** Authenticated bind. */
        BIND_SEARCH("bindSearchAuthenticator"),
        
        /** Direct bind by subject. */
        DIRECT("directAuthenticator"),
        
        /** AD specific bind. */
        AD("adAuthenticator");

        /** Label for this type. */
        @Nonnull @NotEmpty private final String label;
    
        /**
         * Constructor.
         *
         * @param s label for enum
         */
        AuthenticatorType(@Nonnull @NotEmpty final String s) {
            label = s;
        }
    
        /**
         * Gets the enum string label.
         * 
         * @return string label
         */
        @Nonnull @NotEmpty public String label() {
            return label;
        }
    
        /**
         * Returns the enum matching the input label.
         *  
         * @param s input label
         * 
         * @return matching enum or null
         */
        @Nullable public static AuthenticatorType fromLabel(@Nonnull @NotEmpty final String s) {
            for (final AuthenticatorType at : AuthenticatorType.values()) {
                if (at.label().equals(s)) {
                    return at;
                }
            }
            return null;
        }
    }

    /** Enum that defines LDAP trust configuration. Labels maps to values in ldap.properties. */
    public enum TrustType {
        
        /** JVM trust. */
        JVM("jvmTrust"),
        
        /** Explicit certificate file trust. */
        CERTIFICATE("certificateTrust"),
        
        /** Explicit keystore file trust. */
        KEYSTORE("keyStoreTrust"),
        
        /** Trust disabled for non-TLS. */
        DISABLED("disabled");

        /** Label for this type. */
        @Nonnull @NotEmpty private final String label;

        /**
         * Constructor.
         *
         * @param s
         *            label for enum
         */
        TrustType(@Nonnull @NotEmpty final String s) {
            label = s;
        }

        /**
         * Gets the enum string label.
         * 
         * @return string label
         */
        @Nonnull
        @NotEmpty
        public String label() {
            return label;
        }

        /**
         * Returns the enum matching the input label.
         * 
         * @param s
         *            input label
         * 
         * @return matching enum or null
         */
        @Nullable
        public static TrustType fromLabel(@Nonnull @NotEmpty final String s) {
            for (final TrustType tt : TrustType.values()) {
                if (tt.label().equals(s)) {
                    return tt;
                }
            }
            return null;
        }
    }

    /**
     * Enum that defines an LDAP pool passivator. Labels maps to values in
     * ldap.properties.
     */
    public enum PassivatorType {
        
        /** No passivator. */
        NONE("none"),
        
        /** Bind passivator. */
        BIND("bind"),
        
        /** Anonymoud bind passivator. */
        ANONYMOUS_BIND("anonymousBind");

        /** Label for this type. */
        @Nonnull
        @NotEmpty
        private final String label;

        /**
         * Constructor.
         *
         * @param s
         *            label for enum
         */
        PassivatorType(@Nonnull @NotEmpty final String s) {
            label = s;
        }

        /**
         * Gets the enum string label.
         * 
         * @return string label
         */
        @Nonnull
        @NotEmpty
        public String label() {
            return label;
        }

        /**
         * Returns the enum matching the input label.
         * 
         * @param s
         *            input label
         * 
         * @return matching enum or null
         */
        @Nullable
        public static PassivatorType fromLabel(@Nonnull @NotEmpty final String s) {
            for (final PassivatorType pt : PassivatorType.values()) {
                if (pt.label().equals(s)) {
                    return pt;
                }
            }
            return null;
        }
    }

    /**
     * Enum that defines LDAP connection strategy. Labels maps to values in
     * ldap.properties.
     */
    public enum ConnectionStrategyType {
        
        /** Active/passive connection strategy. */
        ACTIVE_PASSIVE("ACTIVE_PASSIVE"),
        
        /** Round robin connection strategy. */
        ROUND_ROBIN("ROUND_ROBIN"),
        
        /** Random connection strategy. */
        RANDOM("RANDOM");

        /** Label for this type. */
        @Nonnull
        @NotEmpty
        private final String label;

        /**
         * Constructor.
         *
         * @param s
         *            label for enum
         */
        ConnectionStrategyType(@Nonnull @NotEmpty final String s) {
            label = s;
        }

        /**
         * Gets the enum string label.
         * 
         * @return string label
         */
        @Nonnull
        @NotEmpty
        public String label() {
            return label;
        }

        /**
         * Returns the enum matching the input label.
         * 
         * @param s
         *            input label
         * 
         * @return matching enum or null
         */
        @Nullable
        public static ConnectionStrategyType fromLabel(@Nonnull @NotEmpty final String s) {
            for (final ConnectionStrategyType cst : ConnectionStrategyType.values()) {
                if (cst.label().equals(s)) {
                    return cst;
                }
            }
            return null;
        }
    }
  

    /** Type of authenticator to configure. */
    private AuthenticatorType authenticatorType;

    /** Type of trust model to configure. */
    private TrustType trustType;

    /** Type of connection strategy to configure. */
    private ConnectionStrategyType connectionStrategyType;

    /** LDAP URL. */
    private String ldapUrl;

    /** Whether to use startTLS for connections. */
    private boolean useStartTLS;

    /** Wait time for startTLS responses. */
    private Duration startTLSTimeout;

    /** Whether to use the allow-all hostname verifier. */
    private boolean disableHostnameVerification;

    /** Wait time for connects. */
    private Duration connectTimeout;

    /** Wait time for operation responses. */
    private Duration responseTimeout;

    /** Whether to automatically reconnect to the server when a connection is lost. */
    private boolean autoReconnect;

    /** Wait time for reconnects. */
    private Duration reconnectTimeout;

    /** Trust configuration when using certificate based trust. */
    private CredentialConfig trustCertificatesCredentialConfig;

    /** Trust configuration when using truststore based trust. */
    private CredentialConfig truststoreCredentialConfig;

    /** Whether to disable connection pooling for both binds and searches. */
    private boolean disablePooling;

    /** Wait time for getting a connection from the pool. */
    private Duration blockWaitTime;

    /** Minimum pool size. */
    private int minPoolSize;

    /** Maximum pool size. */
    private int maxPoolSize;

    /** Whether to validate connections when checked out from the pool. */
    private boolean validateOnCheckout;

    /** Whether to validate connections periodically on a background thread. */
    private boolean validatePeriodically;

    /** Period at which to validate periodically. */
    private Duration validatePeriod;

    /** DN to perform connection pool validation against. */
    private String validateDn;

    /** Filter to execute against {@link #validateDn}. */
    private String validateFilter;

    /** Type of passivator to configure for the bind pool. */
    private PassivatorType bindPoolPassivatorType;

    /** Period at which to check and enforce the idle time. */
    private Duration prunePeriod;

    /**
     * Time at which a connection has been idle and should be removed from the pool.
     */
    private Duration idleTime;

    /**
     * Java format string used to construct an LDAP DN. See
     * {@link String#format(String, Object...)}.
     */
    private String dnFormat;

    /** Base DN used to search for users. */
    private String baseDn;

    /** LDAP filter used to search for users. */
    private String userFilter;

    /** Whether to use a SUBTREE search with the baseDn. */
    private boolean subtreeSearch;

    /** Whether to return the LDAP entry even if the user BIND fails. */
    private boolean resolveEntryOnFailure;

    /** Whether to resolve the user entry with the bind credentials. */
    private boolean resolveEntryWithBindDn;

    /** Velocity engine used to materialize the LDAP filter. */
    private VelocityEngine velocityEngine;

    /** Privileged entry used to search for users. */
    private String bindDn;

    /** Credential for the privileged entry. */
    private String bindDnCredential;

    /**
     * Whether to use the password policy control with the BIND operation. See
     * draft-behera-ldap-password-policy.
     */
    private boolean usePasswordPolicy;

    /**
     * Whether to use the password expiration control with the BIND operation. See
     * draft-vchu-ldap-pwd-policy.
     */
    private boolean usePasswordExpiration;

    /**
     * Whether to use account state data as defined by active directory diagnostic
     * messages.
     */
    private boolean isActiveDirectory;

    /**
     * Whether to use account state data as defined by the FreeIPA directory schema.
     */
    private boolean isFreeIPA;

    /** Whether to use account state data as defined by the EDirectory schema. */
    private boolean isEDirectory;

    /** Authentication handler account state expiration period. */
    private Period accountStateExpirationPeriod;

    /** Authentication handler account state warning period. */
    private Period accountStateWarningPeriod;

    /** Authentication handler account state login failures. */
    private int accountStateLoginFailures;

    /** Set {@link #authenticatorType}.
     * @param type what to set
     */
    public void setAuthenticatorType(@Nonnull @NotEmpty final String type) {
        authenticatorType = AuthenticatorType.fromLabel(type);
        if (authenticatorType == null) {
            throw new IllegalArgumentException("authenticatorType property did not have a valid value");
        }
    }

    /** Set {@link #trustType}.
     * @param type what to set
     */
    public void setTrustType(@Nonnull @NotEmpty final String type) {
        trustType = TrustType.fromLabel(type);
        if (trustType == null) {
            throw new IllegalArgumentException("trustType property did not have a valid value");
        }
    }

    /** Set {@link #connectionStrategyType}.
     * @param type what to set
     */
    public void setConnectionStrategyType(@Nonnull @NotEmpty final String type) {
        connectionStrategyType = ConnectionStrategyType.fromLabel(type);
        if (connectionStrategyType == null) {
            throw new IllegalArgumentException("connectionStrategyType property did not have a valid value");
        }
    }

    /** Set {@link #ldapUrl}.
     * @param url what to set
     */
    public void setLdapUrl(@Nullable @NotEmpty final String url) {
        ldapUrl = url;
    }

    /** Set {@link #useStartTLS}.
     * @param b what to set
     */
    public void setUseStartTLS(final boolean b) {
        useStartTLS = b;
    }

    /** Set {@link #startTLSTimeout}.
     * @param timeout what to set
     */
    public void setStartTLSTimeout(@Nullable final Duration timeout) {
        startTLSTimeout = timeout;
    }

    /** Set {@link #disableHostnameVerification}.
     * @param b what to set
     */
    public void setDisableHostnameVerification(final boolean b) {
        disableHostnameVerification = b;
    }

    /** Set {@link #connectTimeout}.
     * @param timeout what to set
     */
    public void setConnectTimeout(@Nullable final Duration timeout) {
        connectTimeout = timeout;
    }

    /** Set {@link #responseTimeout}.
     * @param timeout what to set
     */
    public void setResponseTimeout(@Nullable final Duration timeout) {
        responseTimeout = timeout;
    }

    /** Set {@link #autoReconnect}.
     * @param b what to set
     */
    public void setAutoReconnect(final boolean b) {
        autoReconnect = b;
    }

    /** Set {@link #reconnectTimeout}.
     * @param timeout what to set
     */
    public void setReconnectTimeout(@Nullable final Duration timeout) {
        reconnectTimeout = timeout;
    }

    /** Set {@link #trustCertificatesCredentialConfig}.
     * @param config to set
     */
    public void setTrustCertificatesCredentialConfig(final CredentialConfig config) {
        trustCertificatesCredentialConfig = config;
    }

    /** Set {@link #truststoreCredentialConfig}.
     * @param config to set
     */
    public void setTruststoreCredentialConfig(final CredentialConfig config) {
        truststoreCredentialConfig = config;
    }

    /** Set {@link #disablePooling}.
     * @param b what to set
     */
    public void setDisablePooling(final boolean b) {
        disablePooling = b;
    }

    /** Set {@link #blockWaitTime}.
     * @param time what to set
     */
    public void setBlockWaitTime(@Nullable final Duration time) {
        blockWaitTime = time;
    }

    /** Set {@link #minPoolSize}.
     * @param size what to set
     */
    public void setMinPoolSize(final int size) {
        minPoolSize = size;
    }

    /** Set {@link #maxPoolSize}.
     * @param size what to set
     */
    public void setMaxPoolSize(final int size) {
        maxPoolSize = size;
    }

    /** Set {@link #validateOnCheckout}.
     * @param b what to set
     */
    public void setValidateOnCheckout(final boolean b) {
        validateOnCheckout = b;
    }

    /** Set {@link #validatePeriodically}.
     * @param b what to set
     */
    public void setValidatePeriodically(final boolean b) {
        validatePeriodically = b;
    }

    /** Set {@link #validatePeriod}.
     * @param period what to set
     */
    public void setValidatePeriod(@Nullable final Duration period) {
        validatePeriod = period;
    }

    /** Set {@link #validateDn}.
     * @param dn what to set
     */
    public void setValidateDn(final String dn) {
        validateDn = dn;
    }

    /** Set {@link #validateFilter}.
     * @param filter what to set
     */
    public void setValidateFilter(final String filter) {
        validateFilter = filter;
    }

    /** Set {@link #bindPoolPassivatorType}.
     * @param type what to set
     */
    public void setBindPoolPassivatorType(@Nonnull @NotEmpty final String type) {
        bindPoolPassivatorType = PassivatorType.fromLabel(type);
        if (bindPoolPassivatorType == null) {
            throw new IllegalArgumentException("bindPoolPassivatorType property did not have a valid value");
        }
    }

    /** Set {@link #prunePeriod}.
     * @param period what to set
     */
    public void setPrunePeriod(@Nullable final Duration period) {
        prunePeriod = period;
    }

    /** Set {@link #idleTime}.
     * @param time what to set
     */
    public void setIdleTime(@Nullable final Duration time) {
        idleTime = time;
    }

    /** Set {@link #dnFormat}.
     * @param format what to set
     */
    public void setDnFormat(final String format) {
        dnFormat = format;
    }

    /** Set {@link #baseDn}.
     * @param dn what to set
     */
    public void setBaseDn(final String dn) {
        baseDn = dn;
    }

    /** Set {@link #userFilter}.
     * @param filter what to set
     */
    public void setUserFilter(final String filter) {
        userFilter = filter;
    }
    
    /**
     *Get the {@link #userFilter}.
     * @return the userfilter
     */
    @Nonnull private String getUserFilter() {
        return Constraint.isNotNull(userFilter, "p:userFilter must be specified");
    }

    /** Set {@link #subtreeSearch}.
     * @param b what to set
     */
    public void setSubtreeSearch(final boolean b) {
        subtreeSearch = b;
    }

    /** Set {@link #resolveEntryOnFailure}.
     * @param b what to set
     */
    public void setResolveEntryOnFailure(final boolean b) {
        resolveEntryOnFailure = b;
    }

    /** Set {@link #resolveEntryWithBindDn}.
     * @param b what to set
     */
    public void setResolveEntryWithBindDn(final boolean b) {
        resolveEntryWithBindDn = b;
    }

    /** Set {@link #velocityEngine}.
     * @param engine what to set
     */
    public void setVelocityEngine(final VelocityEngine engine) {
        velocityEngine = engine;
    }

    /**
     *Get the {@link #velocityEngine}.
     * @return the velocityEngine
     */
    @Nonnull private VelocityEngine getVelocityEngine() {
        return Constraint.isNotNull(velocityEngine, "p:velocityEngine must be specified");
    }


    /** Set {@link #bindDn}.
     * @param dn what to set
     */
    public void setBindDn(final String dn) {
        bindDn = dn;
    }

    /** Set {@link #bindDnCredential}.
     * @param credential what to set
     */
    public void setBindDnCredential(final String credential) {
        bindDnCredential = credential;
    }

    /** Set {@link #usePasswordExpiration}.
     * @param b what to set
     */
    public void setUsePasswordPolicy(final boolean b) {
        usePasswordPolicy = b;
    }

    /** Set {@link #usePasswordExpiration}.
     * @param b what to set
     */
    public void setUsePasswordExpiration(final boolean b) {
        usePasswordExpiration = b;
    }

    /** Set {@link #isActiveDirectory}.
     * @param b what to set
     */
    public void setActiveDirectory(final boolean b) {
        isActiveDirectory = b;
    }

    /** Set {@link #isFreeIPA}.
     * @param b what to set
     */
    public void setFreeIPA(final boolean b) {
        isFreeIPA = b;
    }

    /** Set {@link #isEDirectory}.
     * @param b what to set
     */
    public void setEDirectory(final boolean b) {
        isEDirectory = b;
    }

    /** Set {@link #accountStateExpirationPeriod}.
     * @param period what to set
     */
    public void setAccountStateExpirationPeriod(@Nullable final Period period) {
        accountStateExpirationPeriod = period;
    }

    /** Set {@link #accountStateWarningPeriod}.
     * @param period what to set
     */
    public void setAccountStateWarningPeriod(@Nullable final Period period) {
        accountStateWarningPeriod = period;
    }

    /** Set {@link #accountStateLoginFailures}.
     * @param loginFailures what to set
     */
    public void setAccountStateLoginFailures(final int loginFailures) {
        accountStateLoginFailures = loginFailures;
    }

    /**
     * Returns a new SslConfig object derived from the configured
     * {@link #trustType}. Default uses JVM trust.
     *
     * @return new SslConfig
     */
    protected SslConfig createSslConfig() {
        final SslConfig config = new SslConfig();
        switch (trustType) {
        case CERTIFICATE:
            config.setCredentialConfig(trustCertificatesCredentialConfig);
            break;
        case KEYSTORE:
            config.setCredentialConfig(truststoreCredentialConfig);
            break;
        case DISABLED:
            config.setCredentialConfig(() -> {
                throw new GeneralSecurityException("SSL/startTLS is disabled");
            });
            break;
        case JVM:
        default:
            break;
        }

        if (disableHostnameVerification) {
            log.warn("LDAP Authenticator configured to bypass TLS hostname checking!");
            config.setHostnameVerifier(new AllowAnyHostnameVerifier());
        }
        return config;
    }

    /**
     * Returns a new ConnectionConfig without a connection initializer.
     *
     * @return new ConnectionConfig
     */
    protected ConnectionConfig createConnectionConfig() {
        return createConnectionConfig(null);
    }

    /**
     * Returns a new ConnectionConfig with the supplied connection initializer.
     *
     * @param initializer
     *            to configure or null
     *
     * @return new ConnectionConfig
     */
    protected ConnectionConfig createConnectionConfig(@Nullable final ConnectionInitializer initializer) {
        final ConnectionConfig config = new ConnectionConfig();
        config.setLdapUrl(ldapUrl);
        config.setUseStartTLS(useStartTLS);
        config.setStartTLSTimeout(startTLSTimeout);
        config.setConnectTimeout(connectTimeout);
        config.setResponseTimeout(responseTimeout);
        config.setAutoReconnect(autoReconnect);
        config.setReconnectTimeout(reconnectTimeout);
        switch (connectionStrategyType) {
        case ROUND_ROBIN:
            config.setConnectionStrategy(new RoundRobinConnectionStrategy());
            break;
        case RANDOM:
            config.setConnectionStrategy(new RandomConnectionStrategy());
            break;
        case ACTIVE_PASSIVE:
        default:
            config.setConnectionStrategy(new ActivePassiveConnectionStrategy());
            break;
        }
        config.setSslConfig(createSslConfig());
        if (initializer != null) {
            config.setConnectionInitializers(initializer);
        }
        return config;
    }

    /**
     * Returns a new pooled connection factory. Wires a
     * {@link SearchConnectionValidator} by default.
     *
     * @param name
     *            of the connection pool
     * @param config
     *            to assign to the pool
     *
     * @return new blocking connection pool
     */
    protected PooledConnectionFactory createPooledConnectionFactory(final String name, final ConnectionConfig config) {
        return createPooledConnectionFactory(name, config,
                SearchConnectionValidator.builder().period(validatePeriod).build());
    }

    /**
     * Returns a new pooled connection factory using the supplied search validator.
     *
     * @param name
     *            of the connection pool
     * @param config
     *            to assign to the pool
     * @param validator
     *            pool validator
     *
     * @return new blocking connection pool
     */
    protected PooledConnectionFactory createPooledConnectionFactory(final String name, final ConnectionConfig config,
            final SearchConnectionValidator validator) {
        return createPooledConnectionFactory(name, config, validator, null);
    }

    /**
     * Returns a new pooled connection factory using the supplied search validator
     * and passivator. Note that a {@link PassivatorType#BIND} uses the configured
     * {@link #bindDn} and {@link #bindDnCredential}.
     *
     * @param name
     *            of the connection pool
     * @param config
     *            to assign to the pool
     * @param validator
     *            pool validator
     * @param passivator
     *            pool passivator
     *
     * @return new blocking connection pool
     */
    protected PooledConnectionFactory createPooledConnectionFactory(final String name, final ConnectionConfig config,
            final SearchConnectionValidator validator, final ConnectionPassivator passivator) {
        final PooledConnectionFactory factory = new PooledConnectionFactory();
        factory.setConnectionConfig(config);
        factory.setMinPoolSize(minPoolSize);
        factory.setMaxPoolSize(maxPoolSize);
        factory.setValidateOnCheckOut(validateOnCheckout);
        factory.setValidatePeriodically(validatePeriodically);
        factory.setName(name);
        factory.setBlockWaitTime(blockWaitTime);
        factory.setPruneStrategy(new IdlePruneStrategy(prunePeriod, idleTime));
        factory.setValidator(validator);
        if (passivator != null) {
            factory.setPassivator(passivator);
        }
        factory.setFailFastInitialize(false);
        factory.initialize();
        return factory;
    }

    /**
     * Create {@link SearchConnectionValidator}.
     * 
     * @param baseDn base DN
     * @param filter search filter
     * 
     * @return the validator
     */
    @Nonnull protected SearchConnectionValidator createSearchConnectionValidator(@Nullable final String baseDn,
            @Nullable final String filter) {
        final SearchRequest searchRequest = new SearchRequest();
        searchRequest.setReturnAttributes("1.1");
        searchRequest.setSearchScope(SearchScope.OBJECT);
        searchRequest.setSizeLimit(1);
        if (baseDn != null) {
            searchRequest.setBaseDn(baseDn);
        } else {
            searchRequest.setBaseDn("");
        }
        final FilterTemplate searchFilter = new FilterTemplate();
        if (filter != null) {
            searchFilter.setFilter(filter);
        } else {
            searchFilter.setFilter("(objectClass=*)");
        }
        searchRequest.setFilter(searchFilter);
        return SearchConnectionValidator.builder().request(searchRequest).period(validatePeriod).build();
    }

    /**
     * Creates {@link ConnectionPassivator} object.
     * 
     * @param type type to create
     * 
     * @return the created object
     */
    @Nullable protected ConnectionPassivator createConnectionPassivator(@Nonnull final PassivatorType type) {
        switch (type) {
        case BIND:
            return new BindConnectionPassivator(new SimpleBindRequest(bindDn, new Credential(bindDnCredential)));
        case ANONYMOUS_BIND:
            return new BindConnectionPassivator();
        case NONE:
        default:
            return null;
        }
    }

    // Checkstyle: CyclomaticComplexity|MethodLength OFF
    @Override
    @Nonnull protected Authenticator createInstance() throws Exception {
        final Authenticator authenticator = new Authenticator();
        if (disablePooling) {
            authenticator.setAuthenticationHandler(
                    new SimpleBindAuthenticationHandler(new DefaultConnectionFactory(createConnectionConfig())));
        } else {
            authenticator.setAuthenticationHandler(new SimpleBindAuthenticationHandler(createPooledConnectionFactory(
                    "bind-pool", createConnectionConfig(), createSearchConnectionValidator(validateDn, validateFilter),
                    createConnectionPassivator(bindPoolPassivatorType))));
        }
        switch (authenticatorType) {
        case BIND_SEARCH:
            if (disablePooling) {
                final TemplateSearchDnResolver bindSearchDnResolver = new TemplateSearchDnResolver(getVelocityEngine(),
                        getUserFilter());
                bindSearchDnResolver.setBaseDn(baseDn);
                bindSearchDnResolver.setSubtreeSearch(subtreeSearch);
                bindSearchDnResolver.setConnectionFactory(new DefaultConnectionFactory(createConnectionConfig(
                        new BindConnectionInitializer(bindDn, new Credential(bindDnCredential)))));
                authenticator.setDnResolver(bindSearchDnResolver);
            } else {
                final TemplateSearchDnResolver bindSearchDnResolver = new TemplateSearchDnResolver(getVelocityEngine(),
                        getUserFilter());
                bindSearchDnResolver.setBaseDn(baseDn);
                bindSearchDnResolver.setSubtreeSearch(subtreeSearch);
                bindSearchDnResolver.setConnectionFactory(createPooledConnectionFactory("dn-search-pool",
                        createConnectionConfig(new BindConnectionInitializer(bindDn, new Credential(bindDnCredential))),
                        createSearchConnectionValidator(validateDn, validateFilter)));
                authenticator.setDnResolver(bindSearchDnResolver);
            }
            authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
            break;
        case DIRECT:
            authenticator.setDnResolver(new FormatDnResolver(dnFormat));
            authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
            break;
        case AD:
            authenticator.setDnResolver(new FormatDnResolver(dnFormat));
            authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
            authenticator.setResponseHandlers(new ActiveDirectoryAuthenticationResponseHandler());
            break;
        case ANON_SEARCH:
            if (disablePooling) {
                final TemplateSearchDnResolver anonSearchDnResolver = new TemplateSearchDnResolver(getVelocityEngine(),
                        getUserFilter());
                anonSearchDnResolver.setBaseDn(baseDn);
                anonSearchDnResolver.setSubtreeSearch(subtreeSearch);
                anonSearchDnResolver.setConnectionFactory(new DefaultConnectionFactory(createConnectionConfig()));
                authenticator.setDnResolver(anonSearchDnResolver);
            } else {
                final TemplateSearchDnResolver anonSearchDnResolver = new TemplateSearchDnResolver(getVelocityEngine(),
                        getUserFilter());
                anonSearchDnResolver.setBaseDn(baseDn);
                anonSearchDnResolver.setSubtreeSearch(subtreeSearch);
                anonSearchDnResolver.setConnectionFactory(createPooledConnectionFactory("dn-search-pool",
                        createConnectionConfig(), createSearchConnectionValidator(validateDn, validateFilter)));
                authenticator.setDnResolver(anonSearchDnResolver);
            }
            authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
            break;
        default:
            break;
        }

        if (resolveEntryWithBindDn) {
            if (disablePooling) {
                final SearchEntryResolver searchEntryResolver = new SearchEntryResolver();
                searchEntryResolver.setConnectionFactory(new DefaultConnectionFactory(createConnectionConfig(
                        new BindConnectionInitializer(bindDn, new Credential(bindDnCredential)))));
                authenticator.setEntryResolver(searchEntryResolver);
            } else {
                final SearchEntryResolver searchEntryResolver = new SearchEntryResolver();
                searchEntryResolver.setConnectionFactory(createPooledConnectionFactory("entry-search-pool",
                        createConnectionConfig(new BindConnectionInitializer(bindDn, new Credential(bindDnCredential))),
                        createSearchConnectionValidator(validateDn, validateFilter)));
                authenticator.setEntryResolver(searchEntryResolver);
            }
        }

        if (usePasswordPolicy) {
            authenticator.setRequestHandlers(new PasswordPolicyAuthenticationRequestHandler());
            authenticator.setResponseHandlers(new PasswordPolicyAuthenticationResponseHandler());
        } else if (usePasswordExpiration) {
            authenticator.setResponseHandlers(new PasswordExpirationAuthenticationResponseHandler());
        } else if (isActiveDirectory) {
            authenticator.setResponseHandlers(new ActiveDirectoryAuthenticationResponseHandler(
                    accountStateExpirationPeriod, accountStateWarningPeriod));
        } else if (isEDirectory) {
            authenticator.setResponseHandlers(new EDirectoryAuthenticationResponseHandler(accountStateWarningPeriod));
        } else if (isFreeIPA) {
            authenticator.setResponseHandlers(new FreeIPAAuthenticationResponseHandler(accountStateExpirationPeriod,
                    accountStateWarningPeriod, accountStateLoginFailures));
        }
        log.debug("Created {} from {}", authenticator, this);
        return authenticator;
    }
    // Checkstyle: CyclomaticComplexity|MethodLength ON

    @Override
    protected void destroyInstance(@Nullable final Authenticator instance) {
        if (instance != null) {
            instance.close();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("authenticatorType", authenticatorType).add("trustType", trustType)
                .add("connectionStrategyType", connectionStrategyType).add("ldapUrl", ldapUrl)
                .add("useStartTLS", useStartTLS).add("startTLSTimeout", startTLSTimeout)
                .add("disableHostnameVerification", disableHostnameVerification)
                .add("connectTimeout", connectTimeout).add("responseTimeout", responseTimeout)
                .add("trustCertificatesCredentialConfig", trustCertificatesCredentialConfig)
                .add("truststoreCredentialConfig", truststoreCredentialConfig).add("disablePooling", disablePooling)
                .add("blockWaitTime", blockWaitTime).add("minPoolSize", minPoolSize).add("maxPoolSize", maxPoolSize)
                .add("validateOnCheckout", validateOnCheckout).add("validatePeriodically", validatePeriodically)
                .add("validatePeriod", validatePeriod).add("validateDn", validateDn)
                .add("validateFilter", validateFilter).add("bindPoolPassivatorType", bindPoolPassivatorType)
                .add("prunePeriod", prunePeriod).add("idleTime", idleTime).add("dnFormat", dnFormat)
                .add("baseDn", baseDn).add("userFilter", userFilter).add("subtreeSearch", subtreeSearch)
                .add("resolveEntryOnFailure", resolveEntryOnFailure)
                .add("resolveEntryWithBindDn", resolveEntryWithBindDn).add("velocityEngine", velocityEngine)
                .add("bindDn", bindDn).add("bindDnCredential", bindDnCredential != null ? "suppressed" : null)
                .add("usePasswordPolicy", usePasswordPolicy).add("usePasswordExpiration", usePasswordExpiration)
                .add("isActiveDirectory", isActiveDirectory).add("isFreeIPA", isFreeIPA)
                .add("isEDirectory", isEDirectory).add("accountStateExpirationPeriod", accountStateExpirationPeriod)
                .add("accountStateWarningPeriod", accountStateWarningPeriod)
                .add("accountStateLoginFailures", accountStateLoginFailures).toString();
    }

    @Override
    public Class<?> getObjectType() {
        return Authenticator.class;
    }
}
