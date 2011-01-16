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

package net.shibboleth.idp.tou;

import javax.annotation.Resource;

import net.shibboleth.idp.tou.TermsOfUseContext;
import net.shibboleth.idp.tou.TermsOfUseEngine;
import net.shibboleth.idp.tou.TermsOfUseException;
import net.shibboleth.idp.tou.TermsOfUseContext.Decision;
import net.shibboleth.idp.tou.storage.Storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class TermsOfUseEngineImpl implements TermsOfUseEngine {

    private final Logger logger = LoggerFactory.getLogger(TermsOfUseEngineImpl.class);
    
    @Resource(name="tou")
    private ToU tou;

    @Resource(name="tou.storage")
    private Storage storage;
        
    /** {@inheritDoc} */
    public void determineAcceptance(TermsOfUseContext touContext) throws TermsOfUseException {
        
        final String userId = null; // TODO get userId from touContext
        
        ToUAcceptance touAcceptance;
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
        
        // TODO forward to ToU servlet
    }
    
}
