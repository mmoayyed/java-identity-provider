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

package edu.internet2.middleware.shibboleth.common.security;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.provider.DOMMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.security.MetadataCriteria;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.w3c.dom.Document;

import edu.internet2.middleware.shibboleth.common.BaseTestCase;

/**
 * Testing the Shibboleth metadata PKIX validation information resolver.
 */
public class MetadataPKIXValidationInformationResolverTest extends BaseTestCase {
    
    private String protocolBlue = "PROTOCOL_BLUE";
    
    private String protocolGreen = "PROTOCOL_GREEN";
    
    private String fooEntityID = "http://foo.example.org/shibboleth";
    
    private String barEntityID = "http://bar.example.org/shibboleth";
    
    private BasicParserPool parserPool;
    
    private CriteriaSet criteriaSet;
    
    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        parserPool = new BasicParserPool();
        criteriaSet = new CriteriaSet();
    }
    
    public void testEmpty() {
       MetadataPKIXValidationInformationResolver resolver = getResolver("empty-metadata-pkix.xml");
       criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
       criteriaSet.add( new EntityIDCriteria(fooEntityID) );
       criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
       
       Iterator<PKIXValidationInformation> iter = null;
       try {
           iter = resolver.resolve(criteriaSet).iterator();
       } catch (SecurityException e) {
           fail("Error resolving from PKIX resolver: " + e.getMessage());
       }
       
       assertFalse("Iterator was not empty", iter.hasNext());
    }
    
    public void testNames() {
        MetadataPKIXValidationInformationResolver resolver = getResolver("names-entities-metadata-pkix.xml");
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(fooEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        Set<String> names = null;
        try {
            names = resolver.resolveTrustedNames(criteriaSet);
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        assertNotNull("Set of resolved trusted names was null", names);
        assertFalse("Set of trusted names was empty", names.isEmpty());
        assertEquals("Set of trusted names had incorrect size", 1, names.size());
        assertTrue("Did't find expected name value", names.contains("foo.example.org"));
        
        criteriaSet.clear();
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(fooEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolGreen) );
        
        try {
            names = resolver.resolveTrustedNames(criteriaSet);
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        assertNotNull("Set of resolved trusted names was null", names);
        assertFalse("Set of trusted names was empty", names.isEmpty());
        assertEquals("Set of trusted names had incorrect size", 2, names.size());
        assertTrue("Did't find expected name value", names.contains("CN=foo.example.org,O=Internet2"));
        assertTrue("Did't find expected name value", names.contains("idp.example.org"));
        
        criteriaSet.clear();
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(barEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        try {
            names = resolver.resolveTrustedNames(criteriaSet);
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        assertNotNull("Set of resolved trusted names was null", names);
        assertTrue("Set of trusted names was not empty", names.isEmpty());
     }
    
    public void testNonExistentEntityID() {
        MetadataPKIXValidationInformationResolver resolver = getResolver("oneset-entities-metadata-pkix.xml");
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria("http://doesnt.exist.example.org/shibboleth") );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        Iterator<PKIXValidationInformation> iter = null;
        try {
            iter = resolver.resolve(criteriaSet).iterator();
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        assertFalse("Iterator was not empty", iter.hasNext());
     }
    
    public void testOneSetOnEntitiesDescriptor() {
       MetadataPKIXValidationInformationResolver resolver = getResolver("oneset-entities-metadata-pkix.xml");
       criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
       criteriaSet.add( new EntityIDCriteria(fooEntityID) );
       criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
       
       Iterator<PKIXValidationInformation> iter = null;
       try {
           iter = resolver.resolve(criteriaSet).iterator();
       } catch (SecurityException e) {
           fail("Error resolving from PKIX resolver: " + e.getMessage());
       }
       
       PKIXValidationInformation infoSet = null;
       assertTrue("Iterator was empty", iter.hasNext());
       infoSet = iter.next();
       assertEquals("Incorrect number of certificates", 3, infoSet.getCertificates().size());
       assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
       assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
       
       assertFalse("Iterator was not empty", iter.hasNext());
    }
    
    public void testNoVerifyDepth() {
       MetadataPKIXValidationInformationResolver resolver = getResolver("nodepth-entities-metadata-pkix.xml");
       criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
       criteriaSet.add( new EntityIDCriteria(fooEntityID) );
       criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
       
       Iterator<PKIXValidationInformation> iter = null;
       try {
           iter = resolver.resolve(criteriaSet).iterator();
       } catch (SecurityException e) {
           fail("Error resolving from PKIX resolver: " + e.getMessage());
       }
       
       PKIXValidationInformation infoSet = null;
       assertTrue("Iterator was empty", iter.hasNext());
       infoSet = iter.next();
       // 1 is the default VerifyDepth value
       assertEquals("Incorrect VerifyDepth", new Integer(1), infoSet.getVerificationDepth());
       
       assertFalse("Iterator was not empty", iter.hasNext());
    }
    
    public void testOneSetOnEntitiesDescriptor3KeyInfo() {
        MetadataPKIXValidationInformationResolver resolver = getResolver("oneset-3keyinfo-metadata-pkix.xml");
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(fooEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        Iterator<PKIXValidationInformation> iter = null;
        try {
            iter = resolver.resolve(criteriaSet).iterator();
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        PKIXValidationInformation infoSet = null;
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 7, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 2, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
        
        assertFalse("Iterator was not empty", iter.hasNext());
     }
    
    public void testOneSetOnEntityDescriptor() {
       MetadataPKIXValidationInformationResolver resolver = getResolver("oneset-entity-metadata-pkix.xml");
       criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
       criteriaSet.add( new EntityIDCriteria(fooEntityID) );
       criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
       
       Iterator<PKIXValidationInformation> iter = null;
       try {
           iter = resolver.resolve(criteriaSet).iterator();
       } catch (SecurityException e) {
           fail("Error resolving from PKIX resolver: " + e.getMessage());
       }
       
       PKIXValidationInformation infoSet = null;
       assertTrue("Iterator was empty", iter.hasNext());
       infoSet = iter.next();
       assertEquals("Incorrect number of certificates", 3, infoSet.getCertificates().size());
       assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
       assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
       
       assertFalse("Iterator was not empty", iter.hasNext());
       
       // Now check other entity ID resolves as empty
       criteriaSet.clear();
       criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
       criteriaSet.add( new EntityIDCriteria(barEntityID) );
       criteriaSet.add( new MetadataCriteria(SPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
       
       try {
           iter = resolver.resolve(criteriaSet).iterator();
       } catch (SecurityException e) {
           fail("Error resolving from PKIX resolver: " + e.getMessage());
       }
       
       assertFalse("Iterator was not empty", iter.hasNext());
    }
    
    public void testTwoSetOnEntitiesAndEntityDescriptor() {
        MetadataPKIXValidationInformationResolver resolver = getResolver("twoset-entity-entities-metadata-pkix.xml");
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(fooEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        Iterator<PKIXValidationInformation> iter = null;
        try {
            iter = resolver.resolve(criteriaSet).iterator();
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        PKIXValidationInformation infoSet = null;
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 1, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(3), infoSet.getVerificationDepth());
        
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 6, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
        
        assertFalse("Iterator was not empty", iter.hasNext());
     }
    
    public void testTwoSetOn2Authorities() {
        MetadataPKIXValidationInformationResolver resolver = getResolver("twoset-2authorities-entities-metadata-pkix.xml");
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(fooEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        Iterator<PKIXValidationInformation> iter = null;
        try {
            iter = resolver.resolve(criteriaSet).iterator();
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        PKIXValidationInformation infoSet = null;
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 3, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
        
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 1, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(3), infoSet.getVerificationDepth());
        
        
        assertFalse("Iterator was not empty", iter.hasNext());
    }
    
    public void testThreeSetOn3Authorities() {
        MetadataPKIXValidationInformationResolver resolver = getResolver("threeset-entity-entities-entities-metadata-pkix.xml");
        criteriaSet.add( new UsageCriteria(UsageType.SIGNING) );
        criteriaSet.add( new EntityIDCriteria(fooEntityID) );
        criteriaSet.add( new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, protocolBlue) );
        
        Iterator<PKIXValidationInformation> iter = null;
        try {
            iter = resolver.resolve(criteriaSet).iterator();
        } catch (SecurityException e) {
            fail("Error resolving from PKIX resolver: " + e.getMessage());
        }
        
        PKIXValidationInformation infoSet = null;
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 1, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(3), infoSet.getVerificationDepth());
        
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 3, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 0, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
        
        assertTrue("Iterator was empty", iter.hasNext());
        infoSet = iter.next();
        assertEquals("Incorrect number of certificates", 4, infoSet.getCertificates().size());
        assertEquals("Incorrect number of CRL's", 1, infoSet.getCRLs().size());
        assertEquals("Incorrect VerifyDepth", new Integer(5), infoSet.getVerificationDepth());
        
        assertFalse("Iterator was not empty", iter.hasNext());
     } 
    
    private MetadataPKIXValidationInformationResolver getResolver(String fileName) {
        Document mdDoc = null;
        String mdFileName = DATA_PATH + "/security/" + fileName;
        try {
            mdDoc = parserPool.parse(MetadataPKIXValidationInformationResolverTest.class.getResourceAsStream(mdFileName));
        } catch (XMLParserException e) {
            fail("Failed to parse metadata document: " + e.getMessage());
        }
        
        DOMMetadataProvider mdProvider = new DOMMetadataProvider(mdDoc.getDocumentElement());
        try {
            mdProvider.initialize();
        } catch (MetadataProviderException e) {
            fail("Failed to initialize metadata provider");
        }
        
        return new MetadataPKIXValidationInformationResolver(mdProvider);
    }

}
