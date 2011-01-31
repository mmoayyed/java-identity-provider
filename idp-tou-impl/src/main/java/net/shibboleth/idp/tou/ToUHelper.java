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

import java.util.Collection;
import java.util.SortedMap;
import java.util.regex.Pattern;


import net.shibboleth.idp.attribute.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 */
public class ToUHelper {

    private static final Logger logger = LoggerFactory.getLogger(ToUHelper.class);
    
    // TODO: Method is duplicated in ConsentHelper
    public static String getRelyingParty(final TermsOfUseContext context) {
        // TODO retrieve the relying party from one of the contexts
        return null;
    }
    
    public static Collection<Attribute<?>> getUserAttributes(final TermsOfUseContext context) {
        // TODO retrieve user attributes from one of the contexts
        return null;
    }
    
    // TODO: Method is duplicated in ConsentHelper
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
    
    public static ToU getToUForRelyingParty(final SortedMap<String, ToU> touMap, final String entityId) {
        Pattern pattern;
        logger.trace("touMap {}", touMap);
        for (String key : touMap.keySet()) {
            pattern = Pattern.compile(key);
            logger.trace("checking pattern {}", pattern);
            if (pattern.matcher(entityId).find()) {
                logger.trace("{} matches {}", entityId, pattern);
                return touMap.get(key);
            }
        }
        return null;
    }
    
}
