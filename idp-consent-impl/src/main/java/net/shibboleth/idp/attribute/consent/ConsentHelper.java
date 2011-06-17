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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vt.middleware.crypt.digest.DigestAlgorithm;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Consent helper class.
 * 
 * Provides static utility methods.
 */
@ThreadSafe
public final class ConsentHelper {

    /** Class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentHelper.class);

    /** Default constructor. */
    private ConsentHelper() {

    }

    /**
     * Retrieves the entityId of the relying party.
     * 
     * @param context The current terms of use context.
     * @return The entityId of the relying party.
     */
    public static String getRelyingParty(final ConsentContext context) {
        // TODO retrieve the relying party from one of the contexts
        return null;
    }

    /**
     * Checks if consent revocation is requested.
     * 
     * @param consentContext The @see ConsentContext.
     * @return Returns true if consent revocation was requested by the user.
     */
    public static boolean isConsentRevocationRequested(final ConsentContext consentContext) {
        // TODO retrieve reset flag from HttpRequest parameter.
        return false;
    }

    /**
     * Hashes the values of a given @see Attribute.
     * 
     * @param attribute The @see Attribute.
     * @return Returns the hash of all attribute values.
     */
    public static String hashAttributeValues(final Attribute<?> attribute) {

        // TODO: Make static member?
        final DigestAlgorithm digestAlgorithm = new SHA256();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(attribute.getValues());
            objectOutputStream.flush();
            objectOutputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            LOGGER.error("Error while converting attribute values into a byte array", e);
            return null;
        }
        return digestAlgorithm.digest(byteArrayOutputStream.toByteArray(), new HexConverter(true));
    }

    /**
     * Searches the user identification attribute in the collection of released attributes and returns its value as user
     * id.
     * 
     * @param userIdAttribute The id of the attribute which is used as user identification.
     * @param releasedAttributes The collection of released attributes.
     * @return Returns the user id.
     */
    public static final String findUserId(final String userIdAttribute,
            final Collection<Attribute<?>> releasedAttributes) {
        for (Attribute<?> releasedAttribute : releasedAttributes) {
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
     * Checks if user consent for a specific relying party should be skipped.
     * 
     * I.e., it searches for the relying party id within the white- or blacklist resp.
     * 
     * @param list The collection of relying party ids.
     * @param isBlacklist Indicates whether the list is a blacklist or not (whitelist).
     * @param relyingPartyId the relying party id.
     * @return Returns true if the relying party should skipped.
     */
    public static boolean skipRelyingParty(final Collection<String> list, final boolean isBlacklist,
            final String relyingPartyId) {

        boolean found = false;
        for (String regex : list) {
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(relyingPartyId).find()) {
                found = true;
                break;
            }
        }
        return isBlacklist ? found : !found;
    }

    /**
     * Removes blacklisted attributes from the attributes which will be released.
     * 
     * @param blacklist A collection of attribute ids which are blacklisted
     * @param allAttributes A collection of attributes which will be released.
     * @return Returns a new collection of attributes without the blacklisted ones.
     */
    public static Collection<Attribute<?>> removeBlacklistedAttributes(final Collection<String> blacklist,
            final Collection<Attribute<?>> allAttributes) {
        final Collection<Attribute<?>> attributes = new HashSet<Attribute<?>>();
        for (Attribute<?> attribute : allAttributes) {
            if (!blacklist.contains(attribute.getId())) {
                attributes.add(attribute);
            }
        }
        return attributes;
    }

    /**
     * Sorts the released attributes according the specified sort order.
     * 
     * @param sortOrder A list of attribute ids which specifies the sort order.
     * @param releasedAttributes A collection of attributes which will be released.
     * @return Returns a new collection of sorted attributes.
     */
    public static SortedSet<Attribute<?>> sortAttributes(final List<String> sortOrder,
            final Collection<Attribute<?>> releasedAttributes) {
        return new SortedAttributeSet(sortOrder, releasedAttributes);
    }

    /** A sorted set of attributes. */
    private static class SortedAttributeSet extends TreeSet<Attribute<?>> {

        /** The serial version UID. */
        private static final long serialVersionUID = -9035171496036683394L;

        /**
         * Constructs a new sorted set of attributes.
         * 
         * @param sortOrder A list of attribute ids which specifies the sort order.
         * @param attributes A collection of attributes to be sorted.
         */
        public SortedAttributeSet(final List<String> sortOrder, final Collection<Attribute<?>> attributes) {
            super(new Comparator<Attribute<?>>() {
                @Override
                public int compare(final Attribute<?> attribute1, final Attribute<?> attribute2) {
                    int last = sortOrder.size();

                    int rank1 = sortOrder.indexOf(attribute1.getId());
                    int rank2 = sortOrder.indexOf(attribute2.getId());

                    if (rank2 < 0) {
                        rank2 = last++;
                    }

                    if (rank1 < 0) {
                        rank1 = last++;
                    }

                    return rank1 - rank2;
                }
            });
            addAll(attributes);
        }
    }

    /**
     * Gets the consent context from the HTTP request.
     * 
     * @param request The HTTP request
     * @return Returns the consent context.
     */
    public static ConsentContext getConsentContext(final HttpServletRequest request) {
        // TODO
        return null;
    }

    /**
     * Gets the HTTP request from the consent context.
     * 
     * @param consentContext The consent context.
     * @return Returns the HTTP request.
     */
    public static HttpServletRequest getRequest(final ConsentContext consentContext) {
        // TODO
        return null;
    }

    /**
     * Gets the HTTP response from the consent context.
     * 
     * @param consentContext The consent context.
     * @return Returns the HTTP response.
     */
    public static HttpServletResponse getResponse(final ConsentContext consentContext) {
        // TODO
        return null;
    }

}
