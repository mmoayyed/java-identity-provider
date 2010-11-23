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

package edu.internet2.middleware.shibboleth.idp.tou;

import static org.testng.AssertJUnit.*;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;


/**
 * Tests ToU.
 */

@Test
@ContextConfiguration("classpath:/tou-test-context.xml")
public class ToUTest extends AbstractTestNGSpringContextTests {

    @javax.annotation.Resource(name="tou")
    private ToU tou;
    
    public void instantiation() {
        assertEquals("1.0", tou.getVersion());
        assertNotNull(tou.getText());
    }
    
    public void loadWrongFile() {
    	Resource resource = new FileSystemResource("not-existent.txt");
    	try {
			@SuppressWarnings("unused")
            final ToU invalidTermsOfUse = new ToU("1.0", resource);
			fail("Exception expected");
		} catch (TermsOfUseException e) {}
    }
}
