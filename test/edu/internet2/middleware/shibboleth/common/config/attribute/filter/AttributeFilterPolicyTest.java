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

package edu.internet2.middleware.shibboleth.common.config.attribute.filter;

import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;
import edu.internet2.middleware.shibboleth.common.config.filter.AttributeFilterPolicyGroup;

/**
 * Tests parsing an attribute filter policy configuration.
 */
public class AttributeFilterPolicyTest extends BaseConfigTestCase {

    public void testParsePolicy3(){
        String[] configs = { DATA_PATH + "/attribute/filtering/policy3.xml", };
        ApplicationContext appContext = createSpringContext(configs);
        
        assertNotNull(appContext.containsBean("PolicyExample3"));
        AttributeFilterPolicyGroup policy = (AttributeFilterPolicyGroup) appContext.getBean("PolicyExample3");
    }
}