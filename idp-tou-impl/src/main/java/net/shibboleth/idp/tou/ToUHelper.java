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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.IdPAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vt.middleware.crypt.digest.DigestAlgorithm;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.Converter;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Terms of use helper class.
 * 
 * Provides static utility methods.
 */
public final class ToUHelper {

    /** Class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ToUHelper.class);

    /** Default constructor. */
    private ToUHelper() {

    }

    /**
     * Retrieves the entityId of the relying party.
     * 
     * @param context The current terms of use context.
     * @return The entityId of the relying party.
     */
    // TODO: Method is duplicated in ConsentHelper
    public static String getRelyingParty(final TermsOfUseContext context) {
        // TODO retrieve the relying party from one of the contexts
        return null;
    }

    /**
     * Retrieves the entityId of the relying party.
     * 
     * @param context The current terms of use context.
     * @return A collection of attribute which are releases to the relying party.
     */
    public static Collection<IdPAttribute> getUserAttributes(final TermsOfUseContext context) {
        // TODO retrieve user attributes from one of the contexts
        return null;
    }

    /**
     * Searches the user identification attribute in the collection of released attributes and returns its value as user
     * id.
     * 
     * @param userIdAttribute The id of the attribute which is used as user identification.
     * @param releasedAttributes The collection of released attributes.
     * @return Returns the user id.
     */
    // TODO: Method is duplicated in ConsentHelper
    public static final String findUserId(final String userIdAttribute,
            final Collection<IdPAttribute> releasedAttributes) {
        for (final IdPAttribute releasedAttribute : releasedAttributes) {
            if (releasedAttribute.getId().equals(userIdAttribute)) {
                final Collection<?> userIdAttributeValues = releasedAttribute.getValues();
                if (userIdAttributeValues.isEmpty()) {
                    LOGGER.error("uniqueId attribute {} contains no values.", userIdAttribute);
                    return null;
                }

                if (userIdAttributeValues.size() > 1) {
                    LOGGER.warn("uniqueId attribute {} has more than one value {}. Select first.", userIdAttribute,
                            userIdAttributeValues);
                }

                return String.valueOf(userIdAttributeValues.iterator().next());
            }
        }
        LOGGER.error("uniqueId attribute {} will not be released", userIdAttribute);
        return null;
    }

    /**
     * Retrieves the mapped {@see ToU} for a specific relying party.
     * 
     * @param touMap The relying party / terms of use map.
     * @param entityId The relying party id.
     * @return Returns the mapped {@see ToU} for this relying party id or <code>null</code>.
     */
    public static ToU getToUForRelyingParty(final Map<String, ToU> touMap, final String entityId) {
        Pattern pattern;
        LOGGER.trace("touMap {}", touMap);
        for (final Entry<String, ToU> entry : touMap.entrySet()) {
            pattern = Pattern.compile(entry.getKey());
            LOGGER.trace("checking pattern {}", pattern);
            if (pattern.matcher(entityId).find()) {
                LOGGER.trace("{} matches {}", entityId, pattern);
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Gets the terms of use fingerprint.
     * 
     * @param tou The {@see Tou}.
     * @return Returns the terms of use fingerprint.
     */
    public static String getToUFingerprint(final ToU tou) {
        final DigestAlgorithm digestAlgorithm = new SHA256();
        final Converter converter = new HexConverter(true);

        return digestAlgorithm.digest(tou.getText().getBytes(), converter);
    }

    /**
     * Gets the terms of use context from the HTTP request.
     * 
     * @param request The HTTP request
     * @return Returns the terms of use context.
     */
    public static TermsOfUseContext getTermsOfUseContext(final HttpServletRequest request) {
        // TODO
        return null;
    }

    /**
     * Gets the HTTP request from the terms of use context.
     * 
     * @param touContext The terms of use context.
     * @return Returns the HTTP request.
     */
    public static HttpServletRequest getRequest(final TermsOfUseContext touContext) {
        // TODO
        return null;
    }

    /**
     * Gets the HTTP response from the terms of use context.
     * 
     * @param touContext The term of use context.
     * @return Returns the HTTP response.
     */
    public static HttpServletResponse getResponse(final TermsOfUseContext touContext) {
        // TODO
        return null;
    }

}
