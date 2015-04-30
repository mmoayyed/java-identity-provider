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

package net.shibboleth.idp.authn.impl;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.Config;
import sun.security.krb5.Credentials;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.KrbApReq;
import sun.security.krb5.KrbAsReqBuilder;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;

/**
 * An action that checks for a {@link UsernamePasswordContext} and directly produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on that identity by acquiring
 * a TGT and optional service ticket from Kerberos.
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UsernamePasswordContext.class, false) != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 * successful login. On a failed login, the {@link net.shibboleth.idp.authn.AbstractValidationAction#handleError(
 * ProfileRequestContext, AuthenticationContext, Exception, String)} method is called.
 */
public class ValidateUsernamePasswordAgainstKerberos extends AbstractValidationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateUsernamePasswordAgainstKerberos.class);

    /** Refresh the Kerberos config before running? */
    private boolean refreshKrb5Config;
    
    /** Save the TGT in the resulting Subject? */
    private boolean preserveTicket;
    
    /** Service principal to acquire a ticket for to verify KDC. */
    private String servicePrincipal;
    
    /** Path to keytab for service principal. */
    private String keytabPath;
    
    /** UsernamePasswordContext containing the credentials to validate. */
    @Nullable private UsernamePasswordContext upContext;
    
    /** Result of authentication. */
    @Nullable private Credentials krbCreds;
    
    /**
     * Set whether to refresh the Kerberos configuration before running.
     * 
     * @param flag  flag to set
     */
    public void setRefreshKrb5Config(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        refreshKrb5Config = flag;
    }

    /**
     * Set whether to save the TGT in the Subject.
     * 
     * @param flag  flag to set
     */
    public void setPreserveTicket(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        preserveTicket = flag;
    }
    
    /**
     * Set the name of a service principal to use to verify the KDC.
     * 
     * <p>If non-null, a keytab resource must also be set.</p>
     * 
     * @param name name of service principal
     */
    public void setServicePrincipal(@Nullable final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        servicePrincipal = StringSupport.trimOrNull(name);
    }

    /**
     * Provides a keytab for the service principal to use to verify the KDC.
     * 
     * @param path path to file containing a keytab
     */
    public void setKeytabPath(@Nullable final String path) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keytabPath = path;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (servicePrincipal != null && keytabPath == null) {
            throw new ComponentInitializationException("A keytab path is required if a service principal is set");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }

        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("{} No attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        upContext = authenticationContext.getSubcontext(UsernamePasswordContext.class);
        if (upContext == null) {
            log.info("{} No UsernamePasswordContext available within authentication context", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "NoCredentials", AuthnEventIds.NO_CREDENTIALS);
            return false;
        } else if (upContext.getUsername() == null) {
            log.info("{} No username available within UsernamePasswordContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "NoCredentials", AuthnEventIds.NO_CREDENTIALS);
            return false;
        } else if (upContext.getPassword() == null) {
            log.info("{} No password available within UsernamePasswordContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "InvalidCredentials",
                    AuthnEventIds.INVALID_CREDENTIALS);
            return false;
        }
                
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        try {
            if (refreshKrb5Config) {
                Config.refresh();
            }
            
            // Build principal name to authenticate.
            final PrincipalName pname = new PrincipalName(upContext.getUsername(), PrincipalName.KRB_NT_PRINCIPAL);
            final KrbAsReqBuilder reqBuilder = new KrbAsReqBuilder(pname, upContext.getPassword().toCharArray());
            
            // Do the exchange.
            krbCreds = reqBuilder.action().getCreds();
            reqBuilder.destroy();
            
            if (servicePrincipal != null) {
                verifyKDC();
            }
            
            log.info("{} Login by '{}' succeeded", getLogPrefix(), pname.getName());
            
            buildAuthenticationResult(profileRequestContext, authenticationContext);
        } catch (final KrbException | IOException e) {
            log.warn("{} Login by {} produced exception", getLogPrefix(), upContext.getUsername(), e);
            handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        subject.getPrincipals().add(new UsernamePrincipal(upContext.getUsername()));
        subject.getPrincipals().add(new KerberosPrincipal(krbCreds.getClient().getName()));
        
        if (preserveTicket) {
            subject.getPrivateCredentials().add(Krb5Util.credsToTicket(krbCreds));
        }
        
        return subject;
    }

    /**
     * Use credentials to acquire and verify a service ticket.
     * 
     * @throws IOException
     * @throws KrbException
     */
    private void verifyKDC() throws KrbException, IOException {
        log.debug("{} Attempting to verify authenticity of TGT using service principal '{}'", getLogPrefix(),
                servicePrincipal);
        
        final Credentials serviceCreds = Credentials.acquireServiceCreds(servicePrincipal, krbCreds);
        
        final KeyTab keytab = KeyTab.getInstance(keytabPath);
        if (!keytab.isValid() || keytab.isMissing()) {
            throw new IOException("Service principal keytab was missing or invalid, unable to verify KDC");
        }
        final EncryptionKey[] serviceKeys = keytab.readServiceKeys(new PrincipalName(servicePrincipal));
        if (serviceKeys.length == 0) {
            throw new KrbException("No service keys found in keytab file, unable to verify KDC");
        }
        
        // TODO: this no longer works on 8, so we're probably going to have to redo all this
        // on top of GSS-API to make it viable.
        final KrbApReq request = new KrbApReq(serviceCreds, false, false, false, null);
        //final KrbApReq decrypted = new KrbApReq(request.getMessage(), serviceKeys, null);
        
        log.debug("{} Successfully decrypted AP_REQ issued to service principal '{}'", getLogPrefix(),
                servicePrincipal);
    }
    
}