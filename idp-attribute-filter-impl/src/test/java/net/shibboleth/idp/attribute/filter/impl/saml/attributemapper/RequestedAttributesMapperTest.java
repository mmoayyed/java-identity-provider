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

package net.shibboleth.idp.attribute.filter.impl.saml.attributemapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.shibboleth.idp.attribute.filter.RequestedAttribute;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * tests for {@link RequestedAttributesMapper}.
 */
public class RequestedAttributesMapperTest extends MappingTests {
    
    private RequestedAttributeMapper buildMapper(String name, String samlFormat, BaseAttributeValueMapper mapper) {
        
        RequestedAttributeMapper result = new RequestedAttributeMapper();
        result.setId(name);
        result.setSAMLName(samlFormat);
        result.setValueMapper(mapper);
        return result;
    }
    
    
    @Test public void setterGetter() throws ComponentInitializationException {
        
        RequestedAttributesMapper mapper = new RequestedAttributesMapper();
        mapper.setId("id");
        Assert.assertTrue(mapper.getMappers().isEmpty());
        mapper.setMappers(Collections.singletonList(buildMapper("name", "saml", new StringAttributeValueMapper())));
        
        mapper.initialize();
    }


    @Test public void map() throws ComponentInitializationException {
        
        RequestedAttributesMapper mapper = new RequestedAttributesMapper();
        mapper.setId("id");
   
        mapper.setMappers(Lists.newArrayList(buildMapper("id", SAML_NAME_ONE, new StringAttributeValueMapper()),
                buildMapper("id", SAML_NAME_THREE, new ScopedStringAttributeValueMapper()),
                buildMapper("id2", SAML_NAME_THREE, new StringAttributeValueMapper())));
        
        mapper.initialize();
        
        Multimap<String, RequestedAttribute> result = mapper.mapAttributes(loadFile("requestedAttributeValues.xml"));
        
        Assert.assertEquals(result.keySet().size(), 2);
        
        Collection<RequestedAttribute> id = result.get("id");
        Assert.assertEquals(id.size(), 2);
        Iterator<RequestedAttribute> itr = id.iterator();
        Assert.assertEquals(itr.next().getId(), "id");
        Assert.assertNull(itr.next());
        
        Assert.assertEquals(result.get("id2").size(), 1);
    }
}
