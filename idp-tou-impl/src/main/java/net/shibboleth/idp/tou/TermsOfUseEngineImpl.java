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

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.tou.TermsOfUseContext.Decision;
import net.shibboleth.idp.tou.storage.Storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/** Implements @see TermsOfUseEngine. */
public class TermsOfUseEngineImpl implements TermsOfUseEngine {

    /** Class logger. */
    private final Logger logger = LoggerFactory.getLogger(TermsOfUseEngineImpl.class);

    /** The @see Storage instance which is used. */
    @Resource(name = "tou.storage")
    private Storage storage;

    /** A map of all configured ToUs. */
    @Resource(name = "tou.config.touMap")
    private Map<String, ToU> touMap;

    /** The id of the @see Attribute which should be used for user identifcation. */
    @Resource(name = "tou.config.userIdAttribute")
    private String userIdAttribute;

    /** {@inheritDoc} */
    @Override
    public void determineAcceptance(final TermsOfUseContext touContext) throws TermsOfUseException {

        final String relyingPartyId = ToUHelper.getRelyingParty(touContext);
        final ToU tou = ToUHelper.getToUForRelyingParty(touMap, relyingPartyId);

        if (tou == null) {
            touContext.setTermsOfUseDecision(Decision.INAPPLICABLE);
            logger.info("No ToU found for relying party {}", relyingPartyId);
            return;
        }

        logger.debug("Using ToU {} for relying party {}", tou, relyingPartyId);

        final String userId = ToUHelper.findUserId(userIdAttribute, ToUHelper.getUserAttributes(touContext));
        Assert.notNull(userId, "No userId found");
        logger.debug("Using {}({}) as userId attribute", userIdAttribute, userId);

        final ToUAcceptance touAcceptance;
        if (storage.containsToUAcceptance(userId, tou.getVersion())) {
            touAcceptance = storage.readToUAcceptance(userId, tou.getVersion());
        } else {
            touAcceptance = ToUAcceptance.emptyToUAcceptance();
        }

        if (touAcceptance.contains(tou)) {
            touContext.setTermsOfUseDecision(Decision.PRIOR);
            logger.info("User {} has already accepted ToU {}", userId, tou);
            return;
        }

        final HttpServletRequest request = ToUHelper.getRequest(touContext);
        final HttpServletResponse response = ToUHelper.getResponse(touContext);

        request.setAttribute(ToUServlet.USERID_KEY, userId);

        logger.debug("Dispatch to terms of use view");
        try {
            request.getRequestDispatcher("terms-of-use").forward(request, response);
        } catch (final ServletException e) {
            logger.error("Error while dispatching to terms of use view", e);
        } catch (final IOException e) {
            logger.error("Error while dispatching to terms of use view", e);
        }
    }

}
