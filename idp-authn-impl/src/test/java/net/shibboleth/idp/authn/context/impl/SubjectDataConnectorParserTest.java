/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.authn.context.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.springframework.beans.factory.BeanCreationException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.dc.impl.ContextDerivedDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.SubjectDataConnectorParser;

/**
 * test for {@link SubjectDataConnectorParser}
 */
@SuppressWarnings("javadoc")
public class SubjectDataConnectorParserTest extends BaseAttributeDefinitionParserTest {
    
    @Test public void simple() {
        final ContextDerivedDataConnector connector = getDataConnector("subjectAttributes.xml", ContextDerivedDataConnector.class);

        assertEquals(connector.getExportAttributes().size(), 2);
        assertTrue(connector.getExportAttributes().contains("foo"));
        assertTrue(connector.getExportAttributes().contains("bar"));
        assertTrue(connector.isNoResultIsError());
        
        final SubjectDerivedAttributesFunction fn = (SubjectDerivedAttributesFunction) connector.getAttributesFunction();
        
        assertTrue(fn.isForCanonicalization());
    }
    
    @Test(expectedExceptions = {BeanCreationException.class}) public void emptyNoResultIsError() {
        getDataConnector("subjectAttributesNull.xml", ContextDerivedDataConnector.class);
    }
    
}