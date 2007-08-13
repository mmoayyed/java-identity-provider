/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.binding.security;

import org.joda.time.DateTime;
import org.opensaml.common.binding.security.BaseSAMLSecurityPolicyTest;
import org.opensaml.saml1.core.AttributeQuery;
import org.opensaml.saml1.core.Request;
import org.opensaml.xml.XMLObject;


/**
 * Test the SAML 1.x Shib profile protocol message rule for AttributeQuery requests.
 */
public class SAML1AttributeQueryProtocolMessageRuleTest extends BaseSAMLSecurityPolicyTest {
    
//    private String issuer;
//    private String messageID;
//    private DateTime issueInstant;
//    
//    /** Constructor. */
//    public SAML1AttributeQueryProtocolMessageRuleTest() {
//        issuer = "SomeCoolIssuer";
//        messageID = "abc123";
//        issueInstant = new DateTime();
//    }
//
//    /** {@inheritDoc} */
//    protected void setUp() throws Exception {
//        super.setUp();
//        
//        shibbolethProtocolMessageRuleFactory = new ShibbolethSAML1ProtocolMessageRuleFactory();
//        getPolicyRuleFactories().add(shibbolethProtocolMessageRuleFactory);
//        
//        policyFactory.setRequiredAuthenticatedIssuer(false);
//    }
//
//    /** {@inheritDoc} */
//    protected XMLObject buildMessage() {
//        Request request = (Request) buildXMLObject(Request.DEFAULT_ELEMENT_NAME);
//        request.setID(messageID);
//        request.setIssueInstant(issueInstant);
//        
//        AttributeQuery attribQuery = (AttributeQuery) buildXMLObject(AttributeQuery.DEFAULT_ELEMENT_NAME);
//        attribQuery.setResource(issuer);
//        
//        request.setQuery(attribQuery);
//        
//        return request;
//    }
//    
//    /**
//     * Test basic message information extraction from SAML 1.x AttributeQuery.
//     */
//    public void testRule() {
//        assertPolicySuccess("Request/AttributeQuery (Shib profile) protocol message rule");
//        SAMLSecurityPolicyContext samlContext = (SAMLSecurityPolicyContext) policy.getSecurityPolicyContext();
//        assertEquals("Unexpected value for extracted message ID", messageID, samlContext.getMessageID());
//        assertTrue("Unexpected value for extracted message issue instant", 
//                issueInstant.isEqual(samlContext.getIssueInstant()));
//        assertEquals("Unexpected value for Issuer found", issuer, samlContext.getIssuer());
//    }
//
//    /**
//     * Test basic message information extraction from SAML 1.x AttributeQuery.
//     */
//    public void testRuleNoResource() {
//        Request request = (Request) message;
//        request.getAttributeQuery().setResource(null);
//        
//        assertPolicySuccess("Request/AttributeQuery (Shib profile) protocol message rule");
//        SAMLSecurityPolicyContext samlContext = (SAMLSecurityPolicyContext) policy.getSecurityPolicyContext();
//        assertEquals("Unexpected value for extracted message ID", messageID, samlContext.getMessageID());
//        assertTrue("Unexpected value for extracted message issue instant", 
//                issueInstant.isEqual(samlContext.getIssueInstant()));
//        assertNull("Non-null value for Issuer found", samlContext.getIssuer());
//    }
}
