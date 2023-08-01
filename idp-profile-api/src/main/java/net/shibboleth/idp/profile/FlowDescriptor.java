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

package net.shibboleth.idp.profile;

import net.shibboleth.shared.component.DestructableComponent;
import net.shibboleth.shared.component.IdentifiableComponent;
import net.shibboleth.shared.component.InitializableComponent;

/**
 * Marker interface for a descriptor for a webflow allowing managed injection of configuration settings.
 * 
 * @since 4.0.0
 */
public interface FlowDescriptor extends IdentifiableComponent, InitializableComponent, DestructableComponent {

}