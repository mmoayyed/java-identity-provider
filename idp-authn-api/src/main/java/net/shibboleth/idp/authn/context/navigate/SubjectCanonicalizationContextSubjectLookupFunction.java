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

package net.shibboleth.idp.authn.context.navigate;

import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;

/** A function that returns the {@link Subject} from a {@link SubjectCanonicalizationContext}. */
public class SubjectCanonicalizationContextSubjectLookupFunction
        implements ContextDataLookupFunction<SubjectCanonicalizationContext,Subject> {

    /** {@inheritDoc} */
    @Nullable public Subject apply(@Nullable final SubjectCanonicalizationContext input) {
        
        if (input != null) {
            return input.getSubject();
        }
        return null;
    }

}