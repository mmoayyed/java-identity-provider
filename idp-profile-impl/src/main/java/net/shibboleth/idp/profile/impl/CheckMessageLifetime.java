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

package net.shibboleth.idp.profile.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/** Checks that the inbound message should be considered valid based upon when it was issued. */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_MSG_CTX,
                description = "No inbound message context is associated with the current request"),
        @Event(id = EventIds.INVALID_MSG_MD,
                description = "No inbound message metadata is associated with the current request"),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CTX,
                description = "No relying party context is associated with the current request"),
        @Event(id = CheckMessageLifetime.NO_ISSUE_INSTANT,
                description = "No issue instant is associated with the inbound message"),
        @Event(id = CheckMessageLifetime.PAST_ISSUE_INSTANT,
                description = "Inbound message was issued too far in the past and is now considered expired"),
        @Event(id = CheckMessageLifetime.FUTURE_ISSUE_INSTANT,
                description = "Inbound message was issued too far in the future and is not yet valid")})
public final class CheckMessageLifetime extends AbstractProfileAction {

    /** ID of action returned if no issue instant is associated with the message. */
    public static final String NO_ISSUE_INSTANT = "NoIssueInstant";

    /** ID of action returned if inbound message was issued from a point in time too far in the past. */
    public static final String PAST_ISSUE_INSTANT = "PastIssueInstant";

    /** ID of action returned if inbound message was issued from a point in time too far in the future. */
    public static final String FUTURE_ISSUE_INSTANT = "FutureIssueInstant";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CheckMessageLifetime.class);

    /** Amount of time, in milliseconds, for which a message is valid. Default value: 5 minutes */
    private long messageLifetime;

    /**
     * Strategy used to look up the {@link RelyingPartyContext} associated with the given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> rpContextLookupStrategy;

    /**
     * Strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message context.
     */
    private Function<MessageContext, BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /**
     * Constructor.
     * 
     * Initializes {@link #messageLifetime} to 5 minutes. Initializes {@link #rpContextLookupStrategy} to
     * {@link ChildContextLookup}. Initializes {@link #messageMetadataContextLookupStrategy} to
     * {@link ChildContextLookup}.
     */
    public CheckMessageLifetime() {
        super();

        messageLifetime = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

        rpContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);

        messageMetadataContextLookupStrategy =
                new ChildContextLookup<MessageContext, BasicMessageMetadataContext>(BasicMessageMetadataContext.class,
                        false);
    }

    /**
     * Gets the strategy used to look up the {@link RelyingPartyContext} associated with the given
     * {@link ProfileRequestContext}.
     * 
     * @return strategy used to look up the {@link RelyingPartyContext} associated with the given
     *         {@link ProfileRequestContext}
     */
    @Nonnull public Function<ProfileRequestContext, RelyingPartyContext> getRelyingPartContextLookupStrategy() {
        return rpContextLookupStrategy;
    }

    /**
     * Sets the strategy used to look up the {@link RelyingPartyContext} associated with the given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to look up the {@link RelyingPartyContext} associated with the given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        rpContextLookupStrategy = Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy can not be null");
    }

    /**
     * Gets the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @return strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     *         context
     */
    public Function<MessageContext, BasicMessageMetadataContext> getMessageMetadataContextLookupStrategy() {
        return messageMetadataContextLookupStrategy;
    }

    /**
     * Sets the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @param strategy strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound
     *            message context
     */
    public synchronized void setMessageMetadataContextLookupStrategy(
            @Nonnull final Function<MessageContext, BasicMessageMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        messageMetadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "Message metadata context lookup strategy can not be null");
    }

    /**
     * Gets the amount of time, in milliseconds, for which a message is valid.
     * 
     * @return amount of time, in milliseconds, for which a message is valid
     */
    public long getMessageLifetime() {
        return messageLifetime;
    }

    /**
     * Sets the amount of time, in milliseconds, for which a message is valid.
     * 
     * @param lifetime amount of time, in milliseconds, for which a message is valid
     */
    public synchronized void setMessageLifetime(long lifetime) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Action " + getId()
                    + ": Message liftime can not be changed after action has been initialized");
        }

        messageLifetime = lifetime;
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, ProfileRequestContext profileRequestContext) throws ProfileException {
        log.debug("Action {}: Attempting to check message lifetime", getId());

        final RelyingPartyContext relyingPartyCtx = rpContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.error("Action {}: Relying party context is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }

        final ProfileConfiguration profileConfiguration = relyingPartyCtx.getProfileConfig();
        if (profileConfiguration == null) {
            log.error("Action {}: Relying party profile configuration is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }

        final SecurityConfiguration securityConfiguration = profileConfiguration.getSecurityConfiguration();
        if (securityConfiguration == null) {
            log.error("Action {}: Relying party security configuration is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }

        final MessageContext messageCtx = profileRequestContext.getInboundMessageContext();
        if (messageCtx == null) {
            log.error("Action {}: Inbound message context is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final BasicMessageMetadataContext messageMetadataCtx = messageMetadataContextLookupStrategy.apply(messageCtx);
        if (messageMetadataCtx == null) {
            log.error("Action {}: Inbound message metadata context is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_MD);
        }

        final long issueInstant = messageMetadataCtx.getMessageIssueInstant();
        if (issueInstant <= 0) {
            log.error("Action {}: Inbound message issue instant is not available", getId());
            return ActionSupport.buildEvent(this, NO_ISSUE_INSTANT);
        }

        final long clockskew = securityConfiguration.getClockSkew();
        final long currentTime = System.currentTimeMillis();

        if (issueInstant < currentTime - clockskew) {
            log.debug("Action {}: Inbound message {} was issued too far in the past {} and is now considered expired",
                    new Object[] {getId(), messageMetadataCtx.getMessageId(), new DateTime(issueInstant)});
            return ActionSupport.buildEvent(this, PAST_ISSUE_INSTANT);
        }

        if (issueInstant > currentTime + messageLifetime + clockskew) {
            log.debug("Action {}: Inbound message {} was issued too far in the future {} and is not yet valid",
                    new Object[] {getId(), messageMetadataCtx.getMessageId(), new DateTime(issueInstant)});
            return ActionSupport.buildEvent(this, FUTURE_ISSUE_INSTANT);
        }

        log.debug("Action {}: Message lifetime checked. Returning event '{}'", getId(), EventIds.PROCEED_EVENT_ID);
        return ActionSupport.buildProceedEvent(this);
    }
}