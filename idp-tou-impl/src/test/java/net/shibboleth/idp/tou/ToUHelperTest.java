/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Collection;
import java.util.SortedMap;

import javax.annotation.Resource;

import net.shibboleth.idp.attribute.Attribute;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;


/**
 * Tests ToUHelper.
 */

@ContextConfiguration("classpath:/tou-test-context.xml")
@Test(dataProviderClass = TestData.class)
public class ToUHelperTest extends AbstractTestNGSpringContextTests {
    
    @Resource(name="tou.config.touMap")
    private SortedMap<String, ToU> touMap;
    
    @javax.annotation.Resource(name="tou")
    private ToU tou;

    public void getToUForRelyingParty() {
        assertNotNull(ToUHelper.getToUForRelyingParty(touMap, "https://sp.example.org/shibboleth"));
        System.out.println(tou.getVersion());
        System.out.println(ToUHelper.getToUForRelyingParty(touMap, "https://sp.example.org/shibboleth").getVersion());

        //assertFalse(tou.getVersion().equals(ToUHelper.getToUForRelyingParty(touMap, "https://sp.example.org/shibboleth").getVersion()));
        assertEquals(tou.getVersion(), ToUHelper.getToUForRelyingParty(touMap, "https://sp.other-example.org/shibboleth").getVersion());
        
        touMap.remove(".*");
        assertNull(ToUHelper.getToUForRelyingParty(touMap, "https://sp.other-example.org/shibboleth"));        
    }
    
}