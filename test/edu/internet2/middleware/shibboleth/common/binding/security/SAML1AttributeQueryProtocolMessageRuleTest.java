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
import org.opensaml.common.binding.security.BaseSAMLSecurityPolicyRuleTest;
import org.opensaml.saml1.core.AttributeQuery;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml1.core.Request;
import org.opensaml.saml1.core.Response;


/**
 * Test the SAML 1.x Shib profile protocol message rule for AttributeQuery requests.
 */
public class SAML1AttributeQueryProtocolMessageRuleTest extends BaseSAMLSecurityPolicyRuleTest<Request, Response, NameIdentifier> {
    
    private String issuer;
    private String messageID;
    private DateTime issueInstant;
    
    /** Constructor. */
    public SAML1AttributeQueryProtocolMessageRuleTest() {
        issuer = "SomeCoolIssuer";
        messageID = "abc123";
        issueInstant = new DateTime();
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        rule = new ShibbolethSAML1ProtocolMessageRule();
    }

    /** {@inheritDoc} */
    protected Request buildInboundSAMLMessage() {
        Request request = (Request) buildXMLObject(Request.DEFAULT_ELEMENT_NAME);
        request.setID(messageID);
        request.setIssueInstant(issueInstant);
        
        AttributeQuery attribQuery = (AttributeQuery) buildXMLObject(AttributeQuery.DEFAULT_ELEMENT_NAME);
        attribQuery.setResource(issuer);
        
        request.setQuery(attribQuery);
        
        return request;
    }
    
    /**
     * Test basic message information extraction from SAML 1.x AttributeQuery.
     */
    public void testRule() {
        assertRuleSuccess("Request/AttributeQuery (Shib profile) protocol message rule");
        assertEquals("Unexpected value for extracted message ID", messageID, messageContext.getInboundSAMLMessageId());
        assertTrue("Unexpected value for extracted message issue instant", 
                issueInstant.isEqual(messageContext.getInboundSAMLMessageIssueInstant()) );
        assertEquals("Unexpected value for Issuer found", issuer, messageContext.getInboundMessageIssuer());
    }

    /**
     * Test basic message information extraction from SAML 1.x AttributeQuery.
     */
    public void testRuleNoResource() {
        Request request = messageContext.getInboundSAMLMessage();
        request.getAttributeQuery().setResource(null);
        
        assertRuleSuccess("Request/AttributeQuery (Shib profile) protocol message rule");
        assertEquals("Unexpected value for extracted message ID", messageID, messageContext.getInboundSAMLMessageId());
        assertTrue("Unexpected value for extracted message issue instant", 
                issueInstant.isEqual(messageContext.getInboundSAMLMessageIssueInstant()) );
        assertNull("Non-null value for Issuer found", messageContext.getInboundMessageIssuer());
    }
}
