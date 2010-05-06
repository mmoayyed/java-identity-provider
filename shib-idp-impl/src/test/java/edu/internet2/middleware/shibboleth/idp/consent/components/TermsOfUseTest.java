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

import static org.testng.AssertJUnit.*;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.components.AttributeList;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Tests AttributeList.
 */

@Test
public class TermsOfUseTest extends BaseTest {

    private final Logger logger = LoggerFactory.getLogger(TermsOfUseTest.class);

    @Autowired
    private TermsOfUse termsOfUse;
        
    @Test()
    public void checkFingerprint() {
    	String version = termsOfUse.getVersion();
    	String text =  termsOfUse.getText();
    	
    	String fingerprintInput = version+"|"+text;
    	String fingerprint = new SHA256().digest(fingerprintInput.getBytes(), new HexConverter(true));
    	
    	logger.debug("Terms of use version {}", version);
    	logger.debug("Terms of use text {}", text);
    	logger.debug("Terms of use fingerprint {}", fingerprint);
    	
    	assertEquals(fingerprint, termsOfUse.getFingerprint());	
    }
    
    @Test()
    public void loadWrongFile() {
    	Resource resource = new FileSystemResource("not-existent.txt");
    	try {
			TermsOfUse invalidTermsOfUse = new TermsOfUse("1.1", resource);
			fail("Exception expected");
		} catch (UserConsentException e) {}
    }
    
}
