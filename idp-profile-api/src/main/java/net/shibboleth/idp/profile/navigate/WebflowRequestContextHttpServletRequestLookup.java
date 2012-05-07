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

package net.shibboleth.idp.profile.navigate;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * A {@link Function} that extracts the {@link HttpServletRequest} from the {@link ExternalContext} of the given
 * {@link RequestContext}.
 */
public class WebflowRequestContextHttpServletRequestLookup implements
        Function<RequestContext, HttpServletRequest> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(WebflowRequestContextHttpServletRequestLookup.class);

    /** {@inheritDoc} */
    public HttpServletRequest apply(RequestContext requestContext) {
        final ExternalContext externalContext = requestContext.getExternalContext();
        if (externalContext == null) {
            log.debug("Webflow RequestContext's ExternalContext was null");
            return null;
        }

        return (HttpServletRequest) externalContext.getNativeRequest();
    }
}