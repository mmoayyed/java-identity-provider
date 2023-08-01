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

package net.shibboleth.idp.profile.interceptor.impl;

import java.util.Collection;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.shared.spring.config.IdentifiedComponentManager;

/**
 * Manager of {@link ProfileInterceptorFlowDescriptor} objects.
 * 
 * @since 4.1.0
 */
public class ProfileInterceptorFlowDescriptorManager
        extends IdentifiedComponentManager<ProfileInterceptorFlowDescriptor> {

    /**
     * Constructor.
     *
     * @param freeObjects free-standing objects to add
     */
    @Autowired
    public ProfileInterceptorFlowDescriptorManager(
            @Nullable final Collection<ProfileInterceptorFlowDescriptor> freeObjects) {
        super(freeObjects);
    }

}