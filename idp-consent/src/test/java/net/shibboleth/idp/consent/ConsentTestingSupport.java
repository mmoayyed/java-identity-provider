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

package net.shibboleth.idp.consent;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods for creating test objects for consent action tests.
 */
public class ConsentTestingSupport {

    public static Map<String, Consent> getMap() {

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
}
