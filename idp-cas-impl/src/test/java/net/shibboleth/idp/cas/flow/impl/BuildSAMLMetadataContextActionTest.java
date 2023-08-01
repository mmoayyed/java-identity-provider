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

package net.shibboleth.idp.cas.flow.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.function.Function;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.metadata.EntityGroupName;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;

/**
 * Unit test for {@link BuildSAMLMetadataContextAction}.
 *
 */
@SuppressWarnings("javadoc")
public class BuildSAMLMetadataContextActionTest extends AbstractFlowActionTest {

    @Autowired
    private BuildSAMLMetadataContextAction<?,?> action;

    private Function<ProfileRequestContext, SAMLMetadataContext> mdLookupFunction =
            new SAMLMetadataContextLookupFunction();

    @Test
    public void testServiceWithGroup() throws Exception {
        final Service service = new Service("https://service-1.example.org/", "group-1", true);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addRelyingPartyContext(service.getName(), true, null)
                .addServiceContext(service)
                .build();
        assertNull(action.execute(context));
        final SAMLMetadataContext mdc = mdLookupFunction.apply(getProfileContext(context));
        assertNotNull(mdc);
        final EntityDescriptor ed = mdc.getEntityDescriptor();
        assert ed != null;
        final List<EntityGroupName> groups = ed.getObjectMetadata().get(EntityGroupName.class);
        assertEquals(groups.size(), 1);
        assertEquals(groups.get(0).getName(), service.getGroup());
    }

    @Test
    public void testServiceWithoutGroup() throws Exception {
        final Service service = new Service("https://service-2.example.org/", null, true);
        final RequestContext context = new TestContextBuilder(LoginConfiguration.PROFILE_ID)
                .addRelyingPartyContext(service.getName(), true, null)
                .addServiceContext(service)
                .build();
        assertNull(action.execute(context));
        final SAMLMetadataContext mdc = mdLookupFunction.apply(getProfileContext(context));
        assert mdc!=null;;
        final EntityDescriptor ed = mdc.getEntityDescriptor();
        assert ed != null;
        final List<EntityGroupName> groups = ed.getObjectMetadata().get(EntityGroupName.class);
        assertTrue(groups.isEmpty());
    }
}