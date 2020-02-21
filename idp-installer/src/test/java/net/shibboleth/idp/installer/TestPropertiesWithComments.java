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

package net.shibboleth.idp.installer;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * test for {@link PropertiesWithComments}.
 */
@SuppressWarnings("javadoc")
public class TestPropertiesWithComments {

    // We use a File to aid scrutabiloty in testing
    private File testFile;

    @BeforeMethod public void setup() throws IOException {
        testFile = File.createTempFile("test", ".properties");
    }

    @AfterMethod public void tearDown() {
        testFile.delete();
    }
    
    private InputStream getInputStream() {
        return getClass().getClassLoader().getResourceAsStream("net/shibboleth/idp/installer/file.properties");
    }
    
    private InputStream getNameReplacementStream() {
        return getClass().getClassLoader().getResourceAsStream("net/shibboleth/idp/installer/nameReplace.properties");
    }


    @Test public void testReplaceValues() throws FileNotFoundException, IOException {
        final PropertiesWithComments pwc = new PropertiesWithComments(Set.of("a", "b", "q"));

        pwc.load(getInputStream());

        Assert.assertTrue(pwc.replaceProperty("p", "321"));
        pwc.addComment("Comment");
        Assert.assertFalse(pwc.replaceProperty("nn", "123"));
        Assert.assertTrue(pwc.replaceProperty("yy", "123321"));
        
        pwc.store(new FileOutputStream(testFile));
        
        final Properties p = new Properties();
        
        p.load(new FileInputStream(testFile));
        
        Assert.assertEquals(p.stringPropertyNames().size(), 4);
        Assert.assertEquals(p.getProperty("p"), "321");
        Assert.assertEquals(p.getProperty("nn"), "123");
        Assert.assertEquals(p.getProperty("yy"), "123321");
        Assert.assertEquals(p.getProperty("q"), "elephants"); // "q" was blacklisted to be changed but is OK to be there
        
    }

    @Test public void testBlackList() throws IOException {
        final PropertiesWithComments pwc = new PropertiesWithComments(Set.of("x", "a", "b"));

        pwc.load(getInputStream());

        pwc.replaceProperty("c", "new C");
        try {
            pwc.replaceProperty("a", "new C");
            fail("Property Replacement with black listed name worked");
        } catch (ConstraintViolationException e) {
            // OK
        }

        Properties p = new Properties(1);
        p.setProperty("b", "new b");
        try {
            pwc.replaceProperties(p);
            fail("Property Replacement with black listed name failed");
        } catch (ConstraintViolationException e) {
            // OK
        }

    }

    @Test public void testReplaceNamesFail() throws FileNotFoundException, IOException {
        final PropertiesWithComments pwc = new PropertiesWithComments();
        pwc.load(getInputStream());
        try {
            pwc.loadNameReplacement(getInputStream());
            fail("Expected an IO Exception");
        } 
        catch (IOException e) {
            // Expected
        }
    }


    @Test public void testReplaceNames() throws FileNotFoundException, IOException {
        
        final PropertiesWithComments pwc = new PropertiesWithComments();
        pwc.loadNameReplacement(getNameReplacementStream());
        pwc.load(getInputStream());
        
        pwc.store(new FileOutputStream(testFile));
        
        final Properties p = new Properties();
        
        p.load(new FileInputStream(testFile));
        
        Assert.assertEquals(p.stringPropertyNames().size(), 2);
        Assert.assertEquals(p.getProperty("p"), "123");
        Assert.assertEquals(p.getProperty("elephantName"), "elephants");
        
    }

}
