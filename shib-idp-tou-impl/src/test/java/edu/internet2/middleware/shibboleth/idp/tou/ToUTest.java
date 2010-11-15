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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Tests ToU.
 */

@Test
@ContextConfiguration("classpath:/tou-test-context.xml")
public class ToUTest extends AbstractTestNGSpringContextTests {

    @javax.annotation.Resource(name="tou")
    private ToU tou;
        
    public void checkFingerprint() {
    	final String fingerprint = new SHA256().digest(tou.getText().getBytes(), new HexConverter(true));
    	assertEquals(fingerprint, tou.getFingerprint());	
    }
    
    public void loadWrongFile() {
    	Resource resource = new FileSystemResource("not-existent.txt");
    	try {
			@SuppressWarnings("unused")
            final ToU invalidTermsOfUse = new ToU("1.0", resource);
			fail("Exception expected");
		} catch (TermsOfUseException e) {}
    }
    
    public void equals() {
        final ToU tou = new ToU("version", "fingerprint");
        
        final ToU touT1 = new ToU("version", "fingerprint");
        final ToU touT2 = new ToU("other-version", "fingerprint");
        final ToU touT3 = new ToU("version", "other-fingerprint");
        
        assertEquals(tou, touT1);
        assertFalse(tou.equals(touT2));
        assertFalse(tou.equals(touT3));
    }
    
}
