/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.consent.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.storage.impl.ConsentResult;
import net.shibboleth.shared.collection.CollectionSupport;

/**
 * Helper methods for creating test objects for consent action tests.
 */
@SuppressWarnings("javadoc")
public class ConsentTestingSupport {

    @Nonnull public static Map<String, Consent> newConsentMap() {
        final Consent consent1 = new Consent();
        consent1.setId("consent1");
        consent1.setValue("value1");

        final Consent consent2 = new Consent();
        consent2.setId("consent2");
        consent2.setValue("value2");

        final Map<String, Consent> map = new HashMap<>();
        map.put(consent1.getId(), consent1);
        map.put(consent2.getId(), consent2);
        return map;
    }
    
    public static enum MapType {
        SORTED,
        ORDER1,
        ORDER2,
    }
    @Nonnull public static final Map<String, IdPAttribute> newAttributeMap() {
        return newAttributeMap(MapType.SORTED);
    }

    @Nonnull public static final Map<String, IdPAttribute> newAttributeMap(final MapType order) {
        final IdPAttributeValue value1a = new StringAttributeValue("Avalue1");
        final IdPAttributeValue value1b = new StringAttributeValue("Bvalue1");
        final IdPAttributeValue value1c = new StringAttributeValue("Cvalue1");
        final IdPAttributeValue value2 = new StringAttributeValue("value2");
        final IdPAttributeValue value3 = new StringAttributeValue("value3");

        final IdPAttribute attribute1 = new IdPAttribute("attribute1");
        List<IdPAttributeValue> values;
        switch (order) {
            case SORTED:
            default:
                values = List.of(value1a, value1b, value1c);
                break;
                
            case ORDER1:

                values = List.of(value1b, value1a, value1c);
                break;
                
            case ORDER2:
                values = List.of(value1c, value1a, value1b);
                break;
        }
        attribute1.setValues(values);

        final IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(CollectionSupport.listOf(value1a, value2));

        final IdPAttribute attribute3 = new IdPAttribute("attribute3");
        attribute3.setValues(CollectionSupport.singletonList(value3));

        final Map<String, IdPAttribute> map = new HashMap<>();
        map.put(attribute1.getId(), attribute1);
        map.put(attribute2.getId(), attribute2);
        map.put(attribute3.getId(), attribute3);
        return map;
    }

    @Nonnull public static final List<ConsentResult> newConsentResults() {
        final List<ConsentResult> consentResults = new ArrayList<>();
        consentResults.add(new ConsentResult("context1", "key1", "value1", null));
        consentResults.add(new ConsentResult("context2", "key1", "value1", null));
        consentResults.add(new ConsentResult("context2", "key2", "value2", null));
        return consentResults;
    }

    @Nonnull public static Map<String, Integer> newSymbolicsMap() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("consent1", 101);
        map.put("consent2", 102);
        map.put("attribute1", 201);
        map.put("attribute2", 202);
        map.put("attribute3", 203);
        return map;
    }
}
