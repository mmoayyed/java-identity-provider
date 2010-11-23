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

package edu.internet2.middleware.shibboleth.idp.consent;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;


/**
 *
 */
public class ConsentHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConsentHelper.class);
    
    public static String getRelyingParty(ConsentContext context) {
        // TODO retrieve the relying party from one of the contexts
        return null;
    }

    /**
     * @param consentContext
     * @return
     */
    public static boolean isConsentRevocationRequested(ConsentContext consentContext) {
        // TODO retrieve reset flag from HttpRequest parameter.
        return false;
    }
    
    public static final String findUserId(final String userIdAttribute, final Collection<Attribute<?>> releasedAttributes) {
        for (Attribute<?> releasedAttribute: releasedAttributes) {
            if  (releasedAttribute.getId().equals(userIdAttribute)) {
                Collection<?> userIdAttributeValues = releasedAttribute.getValues();
                if (userIdAttributeValues.isEmpty()) { 
                    logger.error("uniqueId attribute {} contains no values.", userIdAttribute);
                    return null;
                }
                
                if (userIdAttributeValues.size() > 1) {
                    logger.warn("uniqueId attribute {} has more than one value {}. Select first.", userIdAttribute, userIdAttributeValues);
                }
                
                return String.valueOf(userIdAttributeValues.iterator().next());
            }
        }
        logger.error("uniqueId attribute {} will not be released", userIdAttribute);
        return null;
     }
    
    public static boolean isRelyingPartyBlacklisted(final Collection<String> blacklist, final String entityId) {
        Pattern pattern;        
        for (String regex : blacklist) {
            pattern = Pattern.compile(regex);
            if (pattern.matcher(entityId).find()) {
                return true;
            }
        }
        return false;
    }
    
    public static Collection<Attribute<?>> removeBlacklistedAttributes(final Collection<String> blacklist, final Collection<Attribute<?>> allAttributes) {
        final Collection<Attribute<?>> attributes = new HashSet<Attribute<?>>();
        for (Attribute<?> attribute : allAttributes) {
            if (!blacklist.contains(attribute.getId())) {
                attributes.add(attribute);
            }
        }
        return attributes; 
    }
    
    public static SortedSet<Attribute<?>> sortAttributes(final List<String> sortOrder, final Collection <Attribute<?>> releasedAttributes) {       
        return new SortedAttributeSet(sortOrder, releasedAttributes);
    }
        
    private static class SortedAttributeSet extends TreeSet<Attribute<?>> {
        private static final long serialVersionUID = 1L;

        public SortedAttributeSet(final List<String> sortOrder, final Collection<Attribute<?>> attributes) {        
            super(new Comparator<Attribute<?>>() {
                public int compare(Attribute<?> attribute1, Attribute<?> attribute2) {
                    int last = sortOrder.size();
                    
                    int rank1 = sortOrder.indexOf(attribute1.getId());
                    int rank2 = sortOrder.indexOf(attribute2.getId());
                    
                    if (rank2 < 0) rank2 = last++;
                    if (rank1 < 0) rank1 = last++;
                    
                    return rank1 - rank2;
                }});
            addAll(attributes);
        }
    }
    
}
