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

package net.shibboleth.idp.attribute.consent;

/**
 * A consent engine is used to determine if the user consents to the release of their information. This may be done by
 * explicitly asking the user via a prompt, based on previously established preferences, or in any other manner
 * appropriate for the protocol.
 */
public interface ConsentEngine {

    /**
     * Determines the consent of a user for the release of their information.
     * 
     * @param consentContext current consent context
     * 
     * @throws ConsentException thrown if there is a problem determining the user's consent
     */
    public void determineConsent(ConsentContext consentContext) throws ConsentException;
}