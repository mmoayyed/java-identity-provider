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
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.Config;
import sun.security.krb5.Credentials;
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
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("{} No attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        upContext = authenticationContext.getSubcontext(UsernamePasswordContext.class, false);
        if (upContext == null) {
            log.debug("{} No UsernameContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (upContext.getUsername() == null || upContext.getPassword() == null) {
            log.debug("{} No username or password available within UsernamePasswordContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        return super.doPreExecute(profileRequestContext, authenticationContext);
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
            
            log.info("{} Login by '{}' succeeded", getLogPrefix(), pname.getName());
            
            buildAuthenticationResult(profileRequestContext, authenticationContext);
        } catch (final KrbException | IOException e) {
            log.warn(getLogPrefix() + " Login by '" + upContext.getUsername() + "' produced exception", e);
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

}