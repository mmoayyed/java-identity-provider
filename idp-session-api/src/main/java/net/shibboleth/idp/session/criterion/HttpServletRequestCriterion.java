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

package net.shibboleth.idp.session.criterion;

import net.shibboleth.shared.resolver.Criterion;

/**
 * {@link Criterion} representing a session bound to a servlet request,
 * which is implicitly the "current" request known to the resolver.
 */
public final class HttpServletRequestCriterion implements Criterion {

}