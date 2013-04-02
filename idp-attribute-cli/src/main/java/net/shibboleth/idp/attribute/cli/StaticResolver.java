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

package net.shibboleth.idp.attribute.cli;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.profile.EventIds;

import org.opensaml.messaging.profile.AbstractProfileAction;
import org.opensaml.messaging.profile.ProfileException;
import org.opensaml.messaging.profile.ProfileRequestContext;

import com.google.common.collect.ImmutableList;

/**
 * A dummy action to generate some data conformant to the current attribute
 * resolver action.
 */
@Events({
    @Event(id = EventIds.PROCEED_EVENT_ID)
    })
public final class StaticResolver extends AbstractProfileAction {

    /** Constructor. */
    public StaticResolver() {
        super();
    }

    /** {@inheritDoc} */
    public void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final AttributeContext attributeCtx = profileRequestContext.getSubcontext(AttributeContext.class, true);

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(ImmutableList.<AttributeValue> of(new StringAttributeValue("one"),
                new StringAttributeValue("two")));

        Attribute attribute2 = new Attribute("attribute2");
        attribute2.setValues(ImmutableList.<AttributeValue> of(new StringAttributeValue("a"),
                new StringAttributeValue("b")));
        
        attributeCtx.setAttributes(ImmutableList.<Attribute> of(attribute1, attribute2));
    }

}