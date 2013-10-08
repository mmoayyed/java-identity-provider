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

package net.shibboleth.idp.session.criterion;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.Criterion;

/** {@link Criterion} representing a session bound to an {@link HttpServletRequest}. */
public final class HttpServletRequestCriterion implements Criterion {

    /** The session ID. */
    @Nonnull private final HttpServletRequest request;

    /**
     * Constructor.
     * 
     * @param httpRequest the servlet request
     */
    public HttpServletRequestCriterion(@Nonnull final HttpServletRequest httpRequest) {
        request = Constraint.isNotNull(httpRequest, "HttpServletRequest cannot be null");
    }

    /**
     * Get the servlet request.
     * 
     * @return the request
     */
    @Nonnull public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return request.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof HttpServletRequestCriterion) {
            return request.equals(((HttpServletRequestCriterion) obj).request);
        }

        return false;
    }
}