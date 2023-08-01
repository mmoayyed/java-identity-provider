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

import javax.annotation.Nonnull;

import com.google.common.base.MoreObjects;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;

/**
 * A descriptor for a logout propagation flow.
 * 
 * <p>
 * A flow models a sequence of profile actions that performs logout propagation of an {@link SPSession}.
 * The suitability of a particular flow given an instance of {@link SPSession} is determined by
 * {@link #getSessionType()}.
 * </p>
 */
public class LogoutPropagationFlowDescriptor extends AbstractIdentifiableInitializableComponent {

    /** Type of session handled by this flow. */
    @Nonnull private final Class<? extends SPSession> sessionType;

    /**
     * Constructor.
     *
     * @param type type of {@link SPSession} associated with this flow
     */
    public LogoutPropagationFlowDescriptor(@ParameterName(name="type") final Class<? extends SPSession> type) {
        sessionType = Constraint.isNotNull(type, "SPSession type cannot be null");
    }

    /**
     * Get the type of {@link SPSession} supported by this flow descriptor.
     * 
     * @return type of session supported by this flow descriptor
     */
    @Nonnull public Class<? extends SPSession> getSessionType() {
        return sessionType;
    }
    
    /**
     * Test an input session to determine if this flow supports it.
     * 
     * @param session input session
     * 
     * @return true iff this flow should be used to propagate a logout to the corresponding SP
     */
    public boolean isSupported(@Nonnull final SPSession session) {
        if (sessionType.isInstance(session)) {
            return session.supportsLogoutPropagation();
        }
        
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof LogoutPropagationFlowDescriptor) {
            return getId().equals(((LogoutPropagationFlowDescriptor) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("flowId", getId()).toString();
    }
    
}