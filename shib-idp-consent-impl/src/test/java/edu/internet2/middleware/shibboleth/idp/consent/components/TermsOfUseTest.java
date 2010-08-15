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

package edu.internet2.middleware.shibboleth.idp.consent.components;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Tests AttributeList.
 */

@Test
public class TermsOfUseTest extends BaseTest {

    @javax.annotation.Resource(name="termsOfUse")
    private TermsOfUse termsOfUse;
        
    public void checkFingerprint() {
    	String version = termsOfUse.getVersion();
    	String text =  termsOfUse.getText();
    	
    	String fingerprintInput = version+"|"+text;
    	String fingerprint = new SHA256().digest(fingerprintInput.getBytes(), new HexConverter(true));
    	assertEquals(fingerprint, termsOfUse.getFingerprint());	
    }
    
    public void loadWrongFile() {
    	Resource resource = new FileSystemResource("not-existent.txt");
    	try {
			TermsOfUse invalidTermsOfUse = new TermsOfUse();
			invalidTermsOfUse.setVersion("1.1");
			invalidTermsOfUse.setResource(resource);
			invalidTermsOfUse.initialize();
			fail("Exception expected");
		} catch (UserConsentException e) {}
    }
    
    public void equals() {
        TermsOfUse termsOfUse = new TermsOfUse("version", "fingerprint");
        TermsOfUse termsOfUseT1 = new TermsOfUse("version", "fingerprint");
        TermsOfUse termsOfUseT2 = new TermsOfUse("other-version", "fingerprint");
        TermsOfUse termsOfUseT3 = new TermsOfUse("version", "other-fingerprint");
        
        assertEquals(termsOfUse, termsOfUseT1);
        assertFalse(termsOfUse.equals(termsOfUseT2));
        assertEquals(termsOfUse, termsOfUseT3);
        
        assertTrue(termsOfUse.equalsFingerprint(termsOfUseT1));
        assertFalse(termsOfUse.equalsFingerprint(termsOfUseT2));
        assertFalse(termsOfUse.equalsFingerprint(termsOfUseT3));        
    }
    
}
