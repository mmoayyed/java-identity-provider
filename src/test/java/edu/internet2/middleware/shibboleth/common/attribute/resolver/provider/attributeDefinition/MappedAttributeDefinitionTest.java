/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.ArrayList;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StaticDataConnector;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;
import junit.framework.TestCase;

/** Unit test for {@link MappedAttributeDefinition}. */
public class MappedAttributeDefinitionTest extends TestCase{

    private ShibbolethResolutionContext resolutionContext;
    
    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        BasicAttribute<String> affiliation = new BasicAttribute<String>("affiliation");
        affiliation.getValues().add("staff");
        affiliation.getValues().add("student");
        affiliation.getValues().add("partime-employee");
        
        ArrayList<BaseAttribute<String>> attributes = new ArrayList<BaseAttribute<String>>();
        attributes.add(affiliation);
        
        StaticDataConnector data = new StaticDataConnector(attributes);
        
        SAMLProfileRequestContext requestCtx = new BaseSAMLProfileRequestContext();
        resolutionContext = new ShibbolethResolutionContext(requestCtx);
        resolutionContext.getResolvedPlugins().put("affiliation", data);
    }
    
    public void testBasicMapping() throws AttributeResolutionException{
        String foo = "foo";
        String bar = "bar";
        String baz = "baz";
        
        ValueMap fooValueMap = new ValueMap();
        fooValueMap.setReturnValue(foo);
        fooValueMap.getSourceValues().add(fooValueMap.new SourceValue("staff", false, false));
        
        ValueMap barValueMap = new ValueMap();
        barValueMap.setReturnValue(bar);
        barValueMap.getSourceValues().add(barValueMap.new SourceValue("student", false, false));
        
        MappedAttributeDefinition attributeDefinition = new MappedAttributeDefinition();
        attributeDefinition.setId("map1");
        attributeDefinition.setSourceAttributeID("affiliation");
        attributeDefinition.getDependencyIds().add("affiliation");
        attributeDefinition.setDefaultValue("baz");
        attributeDefinition.getValueMaps().add(fooValueMap);
        attributeDefinition.getValueMaps().add(barValueMap);
        
        BaseAttribute mapAttrib = attributeDefinition.resolve(resolutionContext);
        assertEquals("map1", mapAttrib.getId());
        assertEquals(3, mapAttrib.getValues().size());
        assertEquals(true, mapAttrib.getValues().contains(foo));
        assertEquals(true, mapAttrib.getValues().contains(bar));
        assertEquals(true, mapAttrib.getValues().contains(baz));
    }
    
    public void testCaseInsensitveMatch() throws AttributeResolutionException {
        String fooValue = "foo";
        String barValue = "bar";
        
        ValueMap fooValueMap = new ValueMap();
        fooValueMap.setReturnValue(fooValue);
        fooValueMap.getSourceValues().add(fooValueMap.new SourceValue("STAFF", true, false));
        
        ValueMap barValueMap = new ValueMap();
        barValueMap.setReturnValue(barValue);
        barValueMap.getSourceValues().add(barValueMap.new SourceValue("STUDENT", false, false));
        
        MappedAttributeDefinition attributeDefinition = new MappedAttributeDefinition();
        attributeDefinition.setId("map1");
        attributeDefinition.setSourceAttributeID("affiliation");
        attributeDefinition.getDependencyIds().add("affiliation");
        attributeDefinition.getValueMaps().add(fooValueMap);
        attributeDefinition.getValueMaps().add(barValueMap);
        
        BaseAttribute mapAttrib = attributeDefinition.resolve(resolutionContext);
        assertEquals("map1", mapAttrib.getId());
        assertEquals(1, mapAttrib.getValues().size());
        assertEquals(true, mapAttrib.getValues().contains(fooValue));
        assertEquals(false, mapAttrib.getValues().contains(barValue));
    }
    
    public void testPartialMatch() throws AttributeResolutionException {
        String fooValue = "foo";
        
        ValueMap fooValueMap = new ValueMap();
        fooValueMap.setReturnValue(fooValue);
        fooValueMap.getSourceValues().add(fooValueMap.new SourceValue("employee", false, true));
        
        MappedAttributeDefinition attributeDefinition = new MappedAttributeDefinition();
        attributeDefinition.setId("map1");
        attributeDefinition.setSourceAttributeID("affiliation");
        attributeDefinition.getDependencyIds().add("affiliation");
        attributeDefinition.getValueMaps().add(fooValueMap);
        
        BaseAttribute mapAttrib = attributeDefinition.resolve(resolutionContext);
        assertEquals("map1", mapAttrib.getId());
        assertEquals(1, mapAttrib.getValues().size());
        assertEquals(true, mapAttrib.getValues().contains(fooValue));
    }
    
    public void testPassThrough() throws AttributeResolutionException{
        String foo = "foo";
        String bar = "bar";
        
        ValueMap fooValueMap = new ValueMap();
        fooValueMap.setReturnValue(foo);
        fooValueMap.getSourceValues().add(fooValueMap.new SourceValue("staff", false, false));
        
        ValueMap barValueMap = new ValueMap();
        barValueMap.setReturnValue(bar);
        barValueMap.getSourceValues().add(barValueMap.new SourceValue("student", false, false));
        
        MappedAttributeDefinition attributeDefinition = new MappedAttributeDefinition();
        attributeDefinition.setId("map1");
        attributeDefinition.setSourceAttributeID("affiliation");
        attributeDefinition.getDependencyIds().add("affiliation");
        attributeDefinition.setPassThru(true);
        attributeDefinition.getValueMaps().add(fooValueMap);
        attributeDefinition.getValueMaps().add(barValueMap);
        
        BaseAttribute mapAttrib = attributeDefinition.resolve(resolutionContext);
        assertEquals("map1", mapAttrib.getId());
        assertEquals(3, mapAttrib.getValues().size());
        assertEquals(true, mapAttrib.getValues().contains(foo));
        assertEquals(true, mapAttrib.getValues().contains(bar));
        assertEquals(true, mapAttrib.getValues().contains("partime-employee"));
    }
}
