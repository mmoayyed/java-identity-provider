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

package net.shibboleth.idp.attribute.filtering.impl.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringEngine;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.dc.attribute.BaseMappedAttribute;
import net.shibboleth.idp.attribute.resolver.impl.dc.attribute.SAML2AttributeDataConnector;
import net.shibboleth.idp.attribute.resolver.impl.dc.attribute.StringMappedAttribute;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;

import com.google.common.base.Function;

/**
 * Base class for testing complex attribute filter operations.
 */
public class BaseComplexAttributeFilterTestCase extends XMLObjectBaseTestCase {

    private static final String PATH = "/data/net/shibboleth/idp/filter/impl/complex/";

    /**
     * Helper function to return attributes pulled from a file (on the classpath). The file is expected to contain a
     * single <mdattr:EntityAttributes/> statement (for ease).
     * 
     * @param xmlFileName the file within the test directory.
     * @return the att
     * @throws ComponentInitializationException
     * @throws ResolutionException
     */
    protected Map<String, Attribute> getAttributes(String xmlFileName) throws ComponentInitializationException,
            ResolutionException {

        final EntityAttributes obj = (EntityAttributes) unmarshallElement(PATH + xmlFileName);

        SAML2AttributeDataConnector connector = new SAML2AttributeDataConnector();
        connector.setId(xmlFileName);
        final List<BaseMappedAttribute> attributeMap = new ArrayList<BaseMappedAttribute>(2);
        StringMappedAttribute map = new StringMappedAttribute();
        map.setSamlName("eduPersonAffiliation");
        map.setIds(Collections.singletonList("eduPersonAffiliation"));
        attributeMap.add(map);
        map = new StringMappedAttribute();
        map.setSamlName("uid");
        map.setIds(Collections.singletonList("uid"));
        attributeMap.add(map);
        connector.setMap(attributeMap);
        connector.setAttributesStrategy(new Function<AttributeResolutionContext, List<XMLObject>>() {
            @Nullable public List<XMLObject> apply(@Nullable AttributeResolutionContext input) {
                return (List) obj.getAttributes();
            }
        });

        connector.initialize();

        return connector.doResolve(null);
    }

    protected AttributeFilteringEngine getPolicy(String xmlFileName) {
        // TODO this or something like
        return null;
    }

}
