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

package net.shibboleth.idp.profile.spring.failfast;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.IOException;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

@SuppressWarnings("unchecked")
public class FilterFailFastTest extends AbstractFailFastTest {
    @Test public void workingFilter() throws IOException {
        
        final Object bean = getBean(propertySource("ServiceConfiguration", makePath("attributeFilterGood.xml")), "filterBeansDefaultFF.xml");
        final ReloadableService<AttributeFilter > service = (ReloadableService<AttributeFilter>) bean;
        assertNotNull(service);
        final AttributeFilter resolver = service.getServiceableComponent().getComponent();
        assertNotNull(resolver);
    }

    private void badFilter(final Boolean failFast, String filterFile) throws IOException {
        final String beanPath;
        if (failFast == null) {
            beanPath = "filterBeansDefaultFF.xml";
        } else {
            beanPath = "filterBeans.xml";
        }

        final Object bean = getBean(propertySource("ServiceConfiguration", makePath(filterFile)), failFast, beanPath);
        final ReloadableService<AttributeFilter > service = (ReloadableService<AttributeFilter>) bean;
        if (null != failFast && failFast) {
            assertNull(service);
            return;
        }
        assertNotNull(service);
        final ServiceableComponent<AttributeFilter> component = service.getServiceableComponent();
        assertNull(component);
    }
    private void badFilter(final Boolean failFast) throws IOException {
        badFilter(failFast, "attributeFilterBad.xml");
    }
    
    @Test public void badFilterFailFast() throws IOException {
        badFilter(true);
    }

    @Test public void badFilterNoFailFast() throws IOException {
        badFilter(false);
    }

    @Test public void badFilterDefaultFailFast() throws IOException {
        badFilter(null);
    }

    private void badScript(final Boolean failFast) throws IOException {
        badFilter(failFast, "attributeFilterBadScript.xml");
    }
    
    @Test public void badScriptFailFast() throws IOException {
        badScript(true);
    }

    @Test public void badScriptNoFailFast() throws IOException {
        badScript(false);
    }

    @Test public void badScriptDefaultFailFast() throws IOException {
        badScript(null);
    }

}
