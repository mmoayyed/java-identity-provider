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

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringEngine;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.dc.SAMLAttributeDataConnector;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Base class for testing complex attribute filter operations.
 */
public class BaseComplexAttributeFilterTestCase extends XMLObjectBaseTestCase {
    
    private static final String PATH = "/data/net/shibboleth/idp/filter/impl/complex/";
    
    /**
     * Helper function to return attributes pulled from a file (on the classpath).  The file is
     * expected to contain a single <mdattr:EntityAttributes/> statement (for ease).
     *   
     * @param xmlFileName the file within the test directory.
     * @return the att
     * @throws ComponentInitializationException
     * @throws ResolutionException
     */
    protected Optional<Map<String, Attribute>> getAttributes(String xmlFileName) throws ComponentInitializationException, ResolutionException {
        
        final EntityAttributes obj = (EntityAttributes) unmarshallElement(PATH + xmlFileName);

        SAMLAttributeDataConnector connector = new SAMLAttributeDataConnector();
        connector.setId(xmlFileName);
        connector.setAttributesStrategy(new Function<AttributeResolutionContext, List<org.opensaml.saml.saml2.core.Attribute>>() {
                        @Nullable
            public 
            
            List<org.opensaml.saml.saml2.core.Attribute> apply(@Nullable AttributeResolutionContext input) {
                return obj.getAttributes();
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
