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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * Tests ToUHelper.
 */

@ContextConfiguration("classpath:/tou-test-context.xml")
@Test(dataProviderClass = TestData.class)
public class ToUHelperTest extends AbstractTestNGSpringContextTests {

    @Resource(name = "tou.config.touMap")
    private Map<String, ToU> touMap;

    @javax.annotation.Resource(name = "tou")
    private ToU defaultToU;

    public void getToUForRelyingParty() {
        ToU specificToU = ToUHelper.getToUForRelyingParty(touMap, "https://sp.example.org/shibboleth");
        assertNotNull(specificToU);
        assertFalse(specificToU.getVersion().equals(defaultToU.getVersion()));

        ToU nonSpecificToU = ToUHelper.getToUForRelyingParty(touMap, "https://sp.other-example.org/shibboleth");
        assertNotNull(nonSpecificToU);
        assertEquals(defaultToU.getVersion(), nonSpecificToU.getVersion());

        touMap.remove(".*");
        nonSpecificToU = ToUHelper.getToUForRelyingParty(touMap, "https://sp.other-example.org/shibboleth");
        assertNull(nonSpecificToU);
    }

}