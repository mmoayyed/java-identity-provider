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

package net.shibboleth.idp.authn.context;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.collect.ImmutableList;

/**
 * A {@link BaseContext}, usually attached to {@link AuthenticationContext},
 * that holds information about authentication failures.
 *
 * <p>The login process is particularly prone to requiring detailed error
 * information to provide appropriate user feedback and auditing, and this
 * context tracks errors that occur and preserves detailed information about
 * the kind of errors encountered in multi-part authentication flows.
 */
public class AuthenticationErrorContext extends BaseContext {

    /** Ordered list of exceptions encountered. */
    @Nonnull @NonnullElements private List<Exception> exceptions;

    /** Indicates at least one action didn't recognize the username. */
    private boolean unknownUsername;

    /** Indicates at least one action didn't recognize the password. */
    private boolean invalidPassword;

    /** Indicates at least one action detected an expired password. */
    private boolean expiredPassword;

    /** Indicates at least one action detected an account lockout. */
    private boolean accountLocked;

    /** Indicates at least one action detected a disabled account. */
    private boolean accountDisabled;

    /** Indicates at least one action detected a password needing reset. */
    private boolean passwordReset;
    
    /** Constructor. */
    public AuthenticationErrorContext() {
        super();
        
        exceptions = new ArrayList();
    }

    /**
     * Get an immutable list of the exceptions encountered.
     * 
     * @return  immutable list of exceptions
     */
    @Nonnull @NonnullElements @Unmodifiable public List<Exception> getExceptions() {
        return ImmutableList.copyOf(exceptions);
    }
    
    /**
     * Add an exception to the list.
     * 
     * @param e exception to add
     */
    public void addException(@Nonnull final Exception e) {
        Constraint.isNotNull(e, "Exception cannot be null");
        
        exceptions.add(e);
    }
    
    /**
     * Get the unknown username indicator.
     * 
     * @return the unknown username indicator
     */
    public boolean isUnknownUsername() {
        return unknownUsername;
    }

    /**
     * Get the invalid password indicator.
     * 
     * @return invalid password indicator
     */
    public boolean isInvalidPassword() {
        return invalidPassword;
    }

    /**
     * Get the expired password indicator.
     * 
     * @return expired password indicator
     */
    public boolean isExpiredPassword() {
        return expiredPassword;
    }

    /**
     * Get the account lockout indicator.
     * .
     * @return account lockout indicator
     */
    public boolean isAccountLocked() {
        return accountLocked;
    }

    /**
     * Get the account disabled indicator.
     * 
     * @return account disabled indicator
     */
    public boolean isAccountDisabled() {
        return accountDisabled;
    }

    /**
     * Get the password reset indicator.
     * 
     * @return password reset indicator
     */
    public boolean isPasswordReset() {
        return passwordReset;
    }
    
    /**
     * Set the unknown username indicator.
     * 
     * @param flag the indicator to set
     */
    public void setUnknownUsername(boolean flag) {
        unknownUsername = flag;
    }

    /**
     * Set the invalid password indicator.
     * 
     * @param flag the indicator to set
     */
    public void setInvalidPassword(boolean flag) {
        invalidPassword = flag;
    }

    /**
     * Set the expired password indicator.
     * 
     * @param flag the indicator to set 
     */
    public void setExpiredPassword(boolean flag) {
        expiredPassword = flag;
    }

    /**
     * Set the account lockout indicator.
     * 
     * @param flag the indicator to set 
     */
    public void setAccountLocked(boolean flag) {
        accountLocked = flag;
    }

    /**
     * Set the account disabled indicator.
     * 
     * @param flag the indicator to set 
     */
    public void setAccountDisabled(boolean flag) {
        accountDisabled = flag;
    }

    /**
     * Set the password reset indicator.
     * 
     * @param flag the indicator to set
     */
    public void setPasswordReset(boolean flag) {
        passwordReset = flag;
    }

}