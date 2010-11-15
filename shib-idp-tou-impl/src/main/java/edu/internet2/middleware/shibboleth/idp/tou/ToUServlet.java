/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.tou;


import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.tou.storage.Storage;


/**
 *
 */

public class ToUServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 5697887762728622595L;

    private final Logger logger = LoggerFactory.getLogger(ToUServlet.class);

    @Resource(name="tou")
    private ToU tou;

    @Resource(name="tou.storage")
    private Storage storage;
    
    
    /** {@inheritDoc} */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO
        // show velocity template terms-of-use including the model attribute ToU
    }

    /** {@inheritDoc} */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      
        String userId = null;  // TODO get userId from context     
        
        boolean accepted = request.getParameter("accept-tou") != null && request.getParameter("accept-tou").equals("yes");
        if (accepted) {
            logger.info("User {} has accepted ToU version {}", userId, tou.getVersion());
            storage.createAcceptedToU(userId, tou, new DateTime());
            // TODO
            // set the decision to ACCEPTED in the ToU context
        } else {
            logger.info("User {} has declined ToU version {}", userId, tou.getVersion());
            // TODO
            // set the decision to DECLINED in the ToU context
            // show page again with warning message?
        }
    }
}