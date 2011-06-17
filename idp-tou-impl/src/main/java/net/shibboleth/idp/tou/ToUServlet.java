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

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.tou.TermsOfUseContext.Decision;
import net.shibboleth.idp.tou.storage.Storage;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terms of use servlet controls UI interactions.
 */
public class ToUServlet extends HttpServlet {

    /** Key used for the user id. */
    public static final String USERID_KEY = "tou.key.userid";

    /** Serial version UID. */
    private static final long serialVersionUID = 5697887762728622595L;

    /** Class logger. */
    private final Logger logger = LoggerFactory.getLogger(ToUServlet.class);

    /** Configured terms of use. */
    @Resource(name = "tou")
    private ToU tou;

    /** Configured storage. */
    @Resource(name = "tou.storage")
    private Storage storage;

    /** Configured velocity engine. */
    @Resource(name = "velocityEngine")
    private VelocityEngine velocityEngine;

    /** {@inheritDoc} */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {

        // TODO: Initialize it once?
        final Context context = new VelocityContext();
        context.put("tou", tou);

        try {
            velocityEngine.mergeTemplate("terms-of-use.vm", "UTF-8", context, response.getWriter());
        } catch (Exception e) {
            throw new ServletException("Unable to call velocity engine", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        // Probably we need to go via session or other storage
        final String userId = (String) request.getAttribute(USERID_KEY);

        final TermsOfUseContext touContext = ToUHelper.getTermsOfUseContext(request);

        final boolean accepted =
                request.getParameter("tou.accept") != null && request.getParameter("tou.accept").equals("yes");
        if (accepted) {
            final DateTime acceptanceDate = new DateTime();
            logger.info("User {} has accepted ToU version {}", userId, tou.getVersion());
            if (storage.containsToUAcceptance(userId, tou.getVersion())) {
                storage.updateToUAcceptance(userId, ToUAcceptance.createToUAcceptance(tou, acceptanceDate));
            } else {
                storage.createToUAcceptance(userId, ToUAcceptance.createToUAcceptance(tou, acceptanceDate));
            }
            touContext.setTermsOfUseDecision(Decision.ACCEPTED);
        } else {
            logger.info("User {} has declined ToU version {}", userId, tou.getVersion());
            touContext.setTermsOfUseDecision(Decision.DECLINED);
        }
    }
}