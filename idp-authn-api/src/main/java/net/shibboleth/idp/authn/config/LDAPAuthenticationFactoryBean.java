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

package net.shibboleth.idp.authn.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.shibboleth.idp.authn.PooledTemplateSearchDnResolver;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import org.apache.velocity.app.VelocityEngine;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionInitializer;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapURL;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.FormatDnResolver;
import org.ldaptive.auth.PooledBindAuthenticationHandler;
import org.ldaptive.auth.ext.ActiveDirectoryAuthenticationResponseHandler;
import org.ldaptive.auth.ext.EDirectoryAuthenticationResponseHandler;
import org.ldaptive.auth.ext.FreeIPAAuthenticationResponseHandler;
import org.ldaptive.auth.ext.PasswordExpirationAuthenticationResponseHandler;
import org.ldaptive.auth.ext.PasswordPolicyAuthenticationRequestHandler;
import org.ldaptive.auth.ext.PasswordPolicyAuthenticationResponseHandler;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/** LDAP Authentication configuration. See ldap-authn-config.xml */
public class LDAPAuthenticationFactoryBean extends AbstractFactoryBean<Authenticator> {

  /** Class logger. */
  @Nonnull
  private final Logger log = LoggerFactory.getLogger(LDAPAuthenticationFactoryBean.class);

  /** Enum that defines authenticator configuration. Labels maps to values in ldap.properties. */
  public enum AuthenticatorType {
    ANON_SEARCH("anonSearchAuthenticator"),
    BIND_SEARCH("bindSearchAuthenticator"),
    DIRECT("directAuthenticator"),
    AD("adAuthenticator");

    /** Label for this type. */
    private final String label;

    AuthenticatorType(final String s) {
      label = s;
    }

    public String label() {
      return label;
    }

    public static AuthenticatorType fromLabel(final String s) {
      for (AuthenticatorType at : AuthenticatorType.values()) {
        if (at.label().equals(s)) {
          return at;
        }
      }
      return null;
    }
  }

  /** Enum that defines LDAP trust configuration. Labels maps to values in ldap.properties. */
  public enum TrustType {
    JVM("jvmTrust"),
    CERTIFICATE("certificateTrust"),
    KEYSTORE("keyStoreTrust");

    /** Label for this type. */
    private final String label;

    TrustType(final String s) {
      label = s;
    }

    public String label() {
      return label;
    }

    public static TrustType fromLabel(final String s) {
      for (TrustType tt : TrustType.values()) {
        if (tt.label().equals(s)) {
          return tt;
        }
      }
      return null;
    }
  }

  /** Type of authenticator to configure. */
  private AuthenticatorType authenticatorType;

  /** Type of trust model to configure. */
  private TrustType trustType;

  /** LDAP URL. */
  private String ldapUrl;

  /** Whether to use startTLS for connections. */
  private boolean useStartTLS;

  /** Whether to use LDAPS for connections. */
  private boolean useSSL;

  /** Wait time for connects. */
  private Duration connectTimeout;

  /** Wait time for operation responses. */
  private Duration responseTimeout;

  /** Trust configuration when using certificate based trust. */
  private CredentialConfig trustCertificatesCredentialConfig;

  /** Trust configuration when using truststore based trust. */
  private CredentialConfig truststoreCredentialConfig;

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

  /** Period at which to check and enforce the idle time. */
  private Duration prunePeriod;

  /** Time at which a connection has been idle and should be removed from the pool. */
  private Duration idleTime;

  /** Java format string used to construct an LDAP DN. See {@link String#format(String, Object...)}. */
  private String dnFormat;

  /** Base DN used to search for users. */
  private String baseDn;

  /** LDAP filter used to search for users. */
  private String userFilter;

  /** Whether to use a SUBTREE search with the baseDn. */
  private boolean subtreeSearch;

  /** Whether to return the LDAP entry even if the user BIND fails. */
  private boolean resolveEntryOnFailure;

  /** Velocity engine used to materialize the LDAP filter. */
  private VelocityEngine velocityEngine;

  /** Privileged entry used to search for users. */
  private String bindDn;

  /** Credential for the privileged entry. */
  private String bindDnCredential;

  /** Whether to use the password policy control with the BIND operation. See draft-behera-ldap-password-policy. */
  private boolean usePasswordPolicy;

  /** Whether to use the password expiration control with the BIND operation. See draft-vchu-ldap-pwd-policy. */
  private boolean usePasswordExpiration;

  /** Whether to use account state data as defined by the FreeIPA directory schema. */
  private boolean isFreeIPA;

  /** Whether to use account state data as defined by the EDirectory schema. */
  private boolean isEDirectory;

  public void setAuthenticatorType(@Nonnull @NotEmpty final String type) {
    authenticatorType = AuthenticatorType.fromLabel(type);
  }

  public void setTrustType(@Nonnull @NotEmpty final String type) {
    trustType = TrustType.fromLabel(type);
  }

  public void setLdapUrl(@Nullable @NotEmpty final String url) {
    ldapUrl = url;
  }

  public void setUseStartTLS(final boolean b) {
    useStartTLS = b;
  }

  public void setUseSSL(final boolean b) {
    useSSL = b;
  }

  public void setConnectTimeout(@Nullable final Duration timeout) {
    connectTimeout = timeout;
  }

  public void setResponseTimeout(@Nullable final Duration timeout) {
    responseTimeout = timeout;
  }

  public void setTrustCertificatesCredentialConfig(final CredentialConfig config) {
    trustCertificatesCredentialConfig = config;
  }

  public void setTruststoreCredentialConfig(final CredentialConfig config) {
    truststoreCredentialConfig = config;
  }

  public void setBlockWaitTime(@Nullable final Duration time) {
    blockWaitTime = time;
  }

  public void setMinPoolSize(final int size) {
    minPoolSize = size;
  }

  public void setMaxPoolSize(final int size) {
    maxPoolSize = size;
  }

  public void setValidateOnCheckout(final boolean b) {
    validateOnCheckout = b;
  }

  public void setValidatePeriodically(final boolean b) {
    validatePeriodically = b;
  }

  public void setValidatePeriod(@Nullable final Duration period) {
    validatePeriod = period;
  }

  public void setPrunePeriod(@Nullable final Duration period) {
    prunePeriod = period;
  }

  public void setIdleTime(@Nullable final Duration time) {
    idleTime = time;
  }

  public void setDnFormat(final String format) {
    dnFormat = format;
  }

  public void setBaseDn(final String dn) {
    baseDn = dn;
  }

  public void setUserFilter(final String filter) {
    userFilter = filter;
  }

  public void setSubtreeSearch(final boolean b) {
    subtreeSearch = b;
  }

  public void setResolveEntryOnFailure(final boolean b) {
    resolveEntryOnFailure = b;
  }

  public void setVelocityEngine(final VelocityEngine engine) {
    velocityEngine = engine;
  }

  public void setBindDn(final String dn) {
    bindDn = dn;
  }

  public void setBindDnCredential(final String credential) {
    bindDnCredential = credential;
  }

  public void setUsePasswordPolicy(final boolean b) {
    usePasswordPolicy = b;
  }

  public void setUsePasswordExpiration(final boolean b) {
    usePasswordExpiration = b;
  }

  public void setFreeIPA(final boolean b) {
    isFreeIPA = b;
  }

  public void setEDirectory(final boolean b) {
    isEDirectory = b;
  }

  /**
   * Returns a new SslConfig object derived from the configured {@link #trustType}. Default uses JVM trust.
   *
   * @return new SslConfig
   */
  protected SslConfig createSslConfig() {
    final SslConfig config = new SslConfig();
    switch(trustType) {
    case CERTIFICATE:
      config.setCredentialConfig(trustCertificatesCredentialConfig);
      break;
    case KEYSTORE:
      config.setCredentialConfig(truststoreCredentialConfig);
      break;
    case JVM:
    default:
      break;
    }
    return config;
  }

  /**
   * Returns a new ConnectionConfig without a connection initializer.
   *
   * @return  new ConnectionConfig
   */
  protected ConnectionConfig createConnectionConfig() {
    return createConnectionConfig(null);
  }

  /**
   * Returns a new ConnectionConfig with the supplied connection initializer.
   *
   * @param initializer to configure or null
   *
   * @return new ConnectionConfig
   */
  protected ConnectionConfig createConnectionConfig(@Nullable final ConnectionInitializer initializer) {
    final ConnectionConfig config = new ConnectionConfig();
    config.setLdapUrl(ldapUrl);
    config.setUseStartTLS(useStartTLS);
    config.setConnectTimeout(connectTimeout);
    config.setResponseTimeout(responseTimeout);
    config.setSslConfig(createSslConfig());
    if (initializer != null) {
      config.setConnectionInitializer(initializer);
    }
    return config;
  }

  /**
   * Returns a new blocking connection pool. Wires a {@link SearchValidator} by default.
   *
   * @param name of the connection pool
   * @param config to assign to the pool
   *
   * @return new blocking connection pool
   */
  protected BlockingConnectionPool createConnectionPool(final String name, final ConnectionConfig config) {
    final PoolConfig poolConfig = new PoolConfig();
    poolConfig.setMinPoolSize(minPoolSize);
    poolConfig.setMaxPoolSize(maxPoolSize);
    poolConfig.setValidateOnCheckOut(validateOnCheckout);
    poolConfig.setValidatePeriodically(validatePeriodically);
    poolConfig.setValidatePeriod(validatePeriod);
    final BlockingConnectionPool pool = new BlockingConnectionPool();
    pool.setName(name);
    pool.setBlockWaitTime(blockWaitTime);
    pool.setPoolConfig(poolConfig);
    pool.setPruneStrategy(new IdlePruneStrategy(prunePeriod, idleTime));
    pool.setValidator(new SearchValidator());
    pool.setFailFastInitialize(false);
    pool.setConnectionFactory(new DefaultConnectionFactory(config));
    pool.initialize();
    return pool;
  }

// Checkstyle: CyclomaticComplexity|MethodLength OFF
  @Override
  protected Authenticator createInstance() throws Exception {
    // check for deprecated useSSL property
    if (useSSL) {
      DeprecationSupport.warn(ObjectType.PROPERTY, "useSSL", "LDAP authentication",
              "use of ldaps:// scheme in connection URL");
      final LdapURL url = new LdapURL(ldapUrl);
      for (final String s : url.getHostnamesWithSchemeAndPort()) {
        if (!s.startsWith("ldaps://")) {
          throw new IllegalArgumentException("useSSL property specified but URL scheme is not ldaps:// for " + s);
        }
      }
    }
    final Authenticator authenticator = new Authenticator();
    authenticator.setAuthenticationHandler(
      new PooledBindAuthenticationHandler(
        new PooledConnectionFactory(createConnectionPool("bind-pool", createConnectionConfig()))));
    switch(authenticatorType) {
    case BIND_SEARCH:
      final PooledTemplateSearchDnResolver bindSearchDnResolver =
        new PooledTemplateSearchDnResolver(velocityEngine, userFilter);
      bindSearchDnResolver.setBaseDn(baseDn);
      bindSearchDnResolver.setSubtreeSearch(subtreeSearch);
      bindSearchDnResolver.setConnectionFactory(
        new PooledConnectionFactory(
          createConnectionPool(
            "search-pool",
            createConnectionConfig(new BindConnectionInitializer(bindDn, new Credential(bindDnCredential))))));
      authenticator.setDnResolver(bindSearchDnResolver);
      authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
      break;
    case DIRECT:
      authenticator.setDnResolver(new FormatDnResolver(dnFormat));
      authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
      break;
    case AD:
      authenticator.setDnResolver(new FormatDnResolver(dnFormat));
      authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
      authenticator.setAuthenticationResponseHandlers(new ActiveDirectoryAuthenticationResponseHandler());
      break;
    case ANON_SEARCH:
      final PooledTemplateSearchDnResolver anonSearchDnResolver =
        new PooledTemplateSearchDnResolver(velocityEngine, userFilter);
      anonSearchDnResolver.setBaseDn(baseDn);
      anonSearchDnResolver.setSubtreeSearch(subtreeSearch);
      anonSearchDnResolver.setConnectionFactory(
        new PooledConnectionFactory(
          createConnectionPool(
            "search-pool",
            createConnectionConfig())));
      authenticator.setDnResolver(anonSearchDnResolver);
      authenticator.setResolveEntryOnFailure(resolveEntryOnFailure);
      break;
    default:
      break;
    }
    final List<String> retAttrs = new ArrayList<>();
    if (usePasswordPolicy) {
      authenticator.setAuthenticationRequestHandlers(new PasswordPolicyAuthenticationRequestHandler());
      authenticator.setAuthenticationResponseHandlers(new PasswordPolicyAuthenticationResponseHandler());
    } else if (usePasswordExpiration) {
      authenticator.setAuthenticationResponseHandlers(new PasswordExpirationAuthenticationResponseHandler());
    } else if (isEDirectory) {
      retAttrs.addAll(Arrays.asList(EDirectoryAuthenticationResponseHandler.ATTRIBUTES));
      authenticator.setAuthenticationResponseHandlers(new EDirectoryAuthenticationResponseHandler());
    } else if (isFreeIPA) {
      retAttrs.addAll(Arrays.asList(FreeIPAAuthenticationResponseHandler.ATTRIBUTES));
      authenticator.setAuthenticationResponseHandlers(new FreeIPAAuthenticationResponseHandler());
    }
    if (!retAttrs.isEmpty()) {
      authenticator.setReturnAttributes(retAttrs.toArray(new String[0]));
    }
    return authenticator;
  }
// Checkstyle: CyclomaticComplexity|MethodLength ON

  @Override
  public Class<?> getObjectType() {
    return Authenticator.class;
  }
}
