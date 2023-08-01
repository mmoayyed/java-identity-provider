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

package net.shibboleth.idp.test.flows.c14n.actions;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;

/**
 *
 */
public class TestAfterC14N extends AbstractProfileAction {
    
    @Override
    protected void doExecute(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        
        SubjectCanonicalizationContext scc = profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc != null;
        SubjectContext sc = profileRequestContext.ensureSubcontext(SubjectContext.class);
        
        Assert.assertEquals(sc.getPrincipalName(), scc.getPrincipalName());
        
    }
    
}
