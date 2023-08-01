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

package net.shibboleth.idp.test.flows.exception;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.RequestContext;

/**
 * Test action that throws exception.
 */
public class ThrowException extends AbstractProfileAction {
    
    final boolean commitResponse;
    
    /**
     * Constructor.
     *
     * @param commit whether to lock the response
     */
    public ThrowException(final boolean commit) {
        commitResponse = commit;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (commitResponse) {
            final SpringRequestContext springContext = profileRequestContext.getSubcontext(SpringRequestContext.class);
            assert springContext!=null;
            final RequestContext requestContext = springContext.getRequestContext();
            assert requestContext != null;
            final MockHttpServletResponse response =
                    (MockHttpServletResponse) requestContext.getExternalContext().getNativeResponse();
            response.setOutputStreamAccessAllowed(false);
            response.setWriterAccessAllowed(false);
        }
        
        throw new NullPointerException("foo");
    }
    
}