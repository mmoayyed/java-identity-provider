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

package net.shibboleth.idp.profile.context.navigate;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A lookup function that fetches a SWF flow scope parameters.
 * 
 * @since 4.2.0
 */
public class SpringFlowScopeLookupFunction implements Function<ProfileRequestContext,String> {

    /** Parameter name. */
    @Nonnull @NotEmpty private final String paramName;
    
    /**
     * Constructor.
     *
     * @param name parameter name
     */
    public SpringFlowScopeLookupFunction(@Nonnull @NotEmpty @ParameterName(name="name") final String name) {
        paramName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Parameter name cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nullable public String apply(final @Nullable ProfileRequestContext profileRequestContext) {
        final SpringRequestContext springRequestContext =
                profileRequestContext != null ? profileRequestContext.getSubcontext(SpringRequestContext.class) : null;
        if (springRequestContext == null) {
            return null;
        }

        final RequestContext requestContext = springRequestContext.getRequestContext();
        if (requestContext == null) {
            return null;
        }

        return (String) requestContext.getFlowScope().get(paramName);
    }
    
}