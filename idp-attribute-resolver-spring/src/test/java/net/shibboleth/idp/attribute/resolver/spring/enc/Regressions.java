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

import static org.testng.Assert.*;

import java.util.Collection;
import java.util.Map;

import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.saml.attribute.transcoding.impl.SAML2StringAttributeTranscoder;

/**
 * Test for regressions identified and patched.
 */
public class Regressions extends BaseAttributeDefinitionParserTest {

    @Test public void idp571() {
        GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        loadFile(ENCODER_FILE_PATH + "resolver/idp-571.xml", context);
        context.refresh();
     
        Collection<AttributeDefinition> definitions = context.getBeansOfType(AttributeDefinition.class).values();
        Collection<TranscodingRule> transcoderRules = context.getBeansOfType(TranscodingRule.class).values();
        
        assertEquals(definitions.size(), 1);
        assertEquals(transcoderRules.size(), 1);
        
        final Map<String,Object> rule = transcoderRules.iterator().next().getMap();
        assertEquals(rule.get(AttributeTranscoderRegistry.PROP_ID), "skillsoftdept");
        assertTrue(rule.get(AttributeTranscoderRegistry.PROP_TRANSCODER) instanceof SAML2StringAttributeTranscoder);
    }

}