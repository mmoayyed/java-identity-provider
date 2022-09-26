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

package net.shibboleth.idp.saml.saml1.profile.impl;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Request;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.spring.custom.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.shared.testing.MockReloadableService;
import net.shibboleth.shared.xml.XMLParserException;

/** Tests for {@link FilterByQueriedAttributeDesignators} */
public class FilterByQueriedAttributeDesignatorsTest extends XMLObjectBaseTestCase {

    static final String PATH = "/net/shibboleth/idp/saml/impl/profile/";
    
    private Request query;
    
    private ReloadableService<AttributeTranscoderRegistry> registry;
    
    private FilterByQueriedAttributeDesignators action;
    
    private RequestContext rc;
    
    private ProfileRequestContext prc;

    private List<GenericApplicationContext> contexts = new ArrayList<>();

    protected <Type> Type getBean(String fileName, Class<Type> claz) {

        final GenericApplicationContext context = new GenericApplicationContext();
        contexts.add(context);
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(fileName);

        context.refresh();

        Collection<Type> beans = context.getBeansOfType(claz).values();
        Assert.assertEquals(beans.size(), 1);

        return beans.iterator().next();
    }
    
    @AfterClass public void teardown() {
        for (final GenericApplicationContext ctx : contexts) {
            ctx.close();
        }
    }
        
    @BeforeClass public void setup() {
        registry = new MockReloadableService<>(getBean(PATH + "saml1Mapper.xml", AttributeTranscoderRegistryImpl.class));
    }
    
    @BeforeMethod public void setUpMethod() throws ComponentInitializationException, XMLParserException, UnmarshallingException {
        query = unmarshallElement(PATH + "AttributeQuerySaml1.xml", true);

        action = new FilterByQueriedAttributeDesignators();
        action.setTranscoderRegistry(registry);
        action.initialize();
        
        rc = new RequestContextBuilder().setInboundMessage(query).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);
    }

    @Test public void noAttributes() {
        prc.getSubcontext(RelyingPartyContext.class,true);
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void noValues() {
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class,true);
        final AttributeContext ac = rpc.getSubcontext(AttributeContext.class,true);
        final List<IdPAttribute> attributes = List.of(
                new IdPAttribute("eduPersonAssurance"),
                new IdPAttribute("flooby"),
                new IdPAttribute("eduPersonScopedAffiliation"), 
                new IdPAttribute("eduPersonTargetedID"));
        ac.setIdPAttributes(attributes);
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        assertEquals(ac.getIdPAttributes().size(), 3);
    }

    /** This is a non-issue for SAML 1, but just for completeness. */
    @Test public void values() {
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class,true);
        final AttributeContext ac = rpc.getSubcontext(AttributeContext.class,true);
        final IdPAttribute eduPersonAssurance = new IdPAttribute("eduPersonAssurance");
        eduPersonAssurance.setValues(List.of(new StringAttributeValue("green-blue"))); // not turquoise
        final IdPAttribute flooby = new IdPAttribute("flooby");
        final IdPAttribute eduPersonScopedAffiliation = new IdPAttribute("eduPersonScopedAffiliation");
        eduPersonScopedAffiliation.setValues(List.of(new ScopedStringAttributeValue("blue", "yellow")));
        final IdPAttribute eduPersonTargetedID = new IdPAttribute("eduPersonTargetedID");
        eduPersonTargetedID.setValues(List.of(new StringAttributeValue("green-blue")));
        final List<IdPAttribute> attributes = List.of(eduPersonAssurance, flooby,eduPersonScopedAffiliation, eduPersonTargetedID);
        ac.setIdPAttributes(attributes);
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        assertEquals(ac.getIdPAttributes().size(), 3);
    }

}