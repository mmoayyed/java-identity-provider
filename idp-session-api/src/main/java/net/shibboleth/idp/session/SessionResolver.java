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

package net.shibboleth.idp.session;

import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.Resolver;

/** A resolver that is capable of finding {@link IdPSession} objects that meet certain criteria. */
@ThreadSafe
public interface SessionResolver extends Resolver<IdPSession, CriteriaSet> {

}