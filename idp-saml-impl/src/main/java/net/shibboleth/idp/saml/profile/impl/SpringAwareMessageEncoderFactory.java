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

package net.shibboleth.idp.saml.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.encoder.MessageEncoder;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.shibboleth.idp.saml.binding.BindingDescriptor;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.component.AbstractInitializableComponent;


/**
 * A function that returns the correct {@link MessageEncoder} to use based on an underlying {@link BindingDescriptor}.
 */
@ThreadSafeAfterInit
public class SpringAwareMessageEncoderFactory extends AbstractInitializableComponent
        implements Function<ProfileRequestContext,MessageEncoder>, ApplicationContextAware {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringAwareMessageEncoderFactory.class);
    
    /** Application context injected by surroundings. */
    @Nullable private ApplicationContext applicationContext;
    
    /** {@inheritDoc} */
    public void setApplicationContext(@Nullable final ApplicationContext context) {
        applicationContext = context;
    }
    
    /** {@inheritDoc} */
    @Nullable public MessageEncoder apply(@Nullable final ProfileRequestContext profileRequestContext) {
        checkComponentActive();
        
        if (applicationContext == null) {
            log.warn("No Spring ApplicationContext set");
            return null;
        } else if (profileRequestContext.getOutboundMessageContext() == null) {
            log.warn("No outbound message context, unable to lookup message encoder");
            return null;
        }
        
        final SAMLBindingContext bindingContext =
                profileRequestContext.getOutboundMessageContext().getSubcontext(SAMLBindingContext.class);
        if (bindingContext == null || bindingContext.getBindingDescriptor() == null
                || !(bindingContext.getBindingDescriptor() instanceof BindingDescriptor)) {
            log.warn("BindingDescriptor was not available, unable to lookup message encoder");
            return null;
        }
        
        log.debug("Looking up message encoder based on binding URI: {}", bindingContext.getBindingUri());
        
        final BindingDescriptor descriptor = (BindingDescriptor) bindingContext.getBindingDescriptor();
        if (descriptor.getEncoderBeanId() != null) {
            
            try {
                return applicationContext.getBean(descriptor.getEncoderBeanId(), MessageEncoder.class);
            } catch (final BeansException e) {
                log.warn("Error instantiating message encoder from bean ID {}", descriptor.getEncoderBeanId(), e);
            }
        }
        
        log.warn("Failed to find a message encoder based on binding URI: {}", bindingContext.getBindingUri());
        return null;
    }
    
}
