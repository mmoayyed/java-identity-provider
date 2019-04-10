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

package net.shibboleth.idp.attribute.resolver.spring.enc;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.opensaml.saml.saml2.core.Attribute;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2StringAttributeEncoderParser;
import net.shibboleth.idp.profile.logic.ScriptedPredicate;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML2StringAttributeEncoder;

/**
 * Test for {@link SAML2StringAttributeEncoderParser}.
 */
public class SAML2StringAttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {
  
    @Test public void resolver() {
        final SAML2StringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2String.xml", SAML2StringAttributeEncoder.class);

        assertEquals(encoder.getName(), "Saml2String_ATTRIBUTE_NAME");
        assertEquals(encoder.getFriendlyName(),"Saml2String_ATTRIBUTE_FRIENDLY_NAME"); 
        assertEquals(encoder.getNameFormat(),"Saml2String_ATTRIBUTE_NAME_FORMAT");
    }
    
    @Test public void defaultCase() {
        final SAML2StringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2StringDefault.xml", SAML2StringAttributeEncoder.class);

        assertSame(encoder.getActivationCondition(), Predicates.alwaysTrue());
        assertTrue(encoder.getActivationCondition().test(null));
        assertEquals(encoder.getName(), "Saml2StringName");
        assertNull(encoder.getFriendlyName()); 
        assertEquals(encoder.getNameFormat(), Attribute.URI_REFERENCE);
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,})  public void noName() {
        getAttributeEncoder("resolver/saml2StringNoName.xml", SAML2StringAttributeEncoder.class);
    }
    
    @Test public void conditional() {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);

        loadFile(ENCODER_FILE_PATH + "predicates.xml", context);
        
        final SAML2StringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2StringConditional.xml", SAML2StringAttributeEncoder.class, context);

        assertSame(encoder.getActivationCondition(), Predicates.alwaysFalse());
        assertFalse(encoder.getActivationCondition().test(null));
    }

    @Test public void conditionalScript() {
        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        
        final SAML2StringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2String.xml", SAML2StringAttributeEncoder.class, context);

        assertTrue(encoder.getActivationCondition() instanceof ScriptedPredicate);
        assertFalse(encoder.getActivationCondition().test(null));
    }

}
