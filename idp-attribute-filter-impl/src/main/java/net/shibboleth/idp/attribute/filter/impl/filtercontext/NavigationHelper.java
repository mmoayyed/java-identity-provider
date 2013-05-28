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

package net.shibboleth.idp.attribute.filter.impl.filtercontext;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;

import org.opensaml.messaging.context.BaseContext;

/**
 * Helper class for context Navigation.
 */
public final class NavigationHelper {

    /** (hidden) Constructor. */
    private NavigationHelper() {

    }

    /**
     * Find the {@link AttributeResoltuionContext} for this filter Context.
     * 
     * @param filterContext the filterContext
     * @return the associated resolution context
     */
    @Nonnull protected static AttributeResolutionContext locateResolverContext(
            @Nonnull AttributeFilterContext filterContext) {
        final BaseContext parent = filterContext.getParent();

        if (null == parent) {
            //  TODO
            throw new IllegalArgumentException("Attribute Filter:  Count not locate the parent context");
        }

        final AttributeResolutionContext resolutionContext =
                parent.getSubcontext(AttributeResolutionContext.class, false);
        if (null == resolutionContext) {
            //  TODO
            throw new IllegalArgumentException("Attribute Filter:  Count not locate the resolution context");
        }
        return resolutionContext;
    }

    /**
     * Find the {@link AttributeRecipientContext} for this filter Context.
     * 
     * @param filterContext the filterContext
     * @return the associated recipient context
     */
    @Nonnull protected static AttributeRecipientContext locateRecipientContext(
            @Nonnull AttributeResolutionContext filterContext) {
        
        final AttributeRecipientContext recipientContext =
                filterContext.getSubcontext(AttributeRecipientContext.class, false);
        
        if (null == recipientContext) {
            //  TODO
            throw new IllegalArgumentException("Attribute Filter:  Count not locate the recipient context");
        }
        
        return recipientContext;
    } 
}
