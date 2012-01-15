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

package net.shibboleth.idp.tou;

import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;

/** Context used to collect data about the terms of use acceptance. */
public final class TermsOfUseContext implements Subcontext {

    /** Indicates the result of the terms of use engine's attempt to gain user acceptance. */
    public static enum Decision {
        /** Indicates that no claim is made about terms of use acceptance. */
        UNSPECIFIED,

        /**
         * Indicates that it was not possible to determine th user's acceptance of the terms of use for the given
         * request.
         */
        INAPPLICABLE,

        /** User gave acceptance of the terms of use in the past. */
        PRIOR,

        /** Indicates that the user accepted the terms of use. */
        ACCEPTED,

        /** Indicates that the user declined the terms of use. */
        DECLINED
    }

    /** Owner of this consent context. */
    private final SubcontextContainer owner;

    /** The decision determined by the terms of use engine. */
    private Decision termsOfUseDecision;

    /**
     * Constructor.
     * 
     * @param superContext owner of this context
     */
    public TermsOfUseContext(final SubcontextContainer superContext) {
        owner = Assert.isNotNull(superContext, "Owning super context may not be null");;
        termsOfUseDecision = Decision.UNSPECIFIED;
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return owner;
    }

    /**
     * Gets the terms of use decision established by the user.
     * 
     * @return terms of use decision established by the user
     */
    public Decision getTermsOfUseDecision() {
        return termsOfUseDecision;
    }

    /**
     * Sets the terms of use decision established by the user.
     * 
     * @param decision terms of use decision established by the user
     */
    public void setTermsOfUseDecision(final Decision decision) {
        termsOfUseDecision = decision;
    }
}