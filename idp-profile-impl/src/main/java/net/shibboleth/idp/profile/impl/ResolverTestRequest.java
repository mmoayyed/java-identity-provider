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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/**
 * Object representing a request to run the attribute resolution and filtering components.
 * 
 * <p>This abstracts the parameters used to populate the context tree and query attributes
 * for a subject, optionally on behalf of a specific requester for a specific protocol.</p>
 */
@ThreadSafe
public class ResolverTestRequest {
    
    /** The name of the subject. */
    @Nonnull @NotEmpty private final String principal;

    /** The ID of the requester. */
    @Nonnull @NotEmpty private final String requesterId;

    /** The <code>&lt;AttributeConsumingService&gt;</code> index into metadata. */
    @Nullable private final Integer acsIndex;
    
    /** Protocol identifier to simulate a response for. */
    @Nullable private final String protocol;
    
    /** Unfiltered flag. */
    @Nullable private final Boolean unfiltered;

    /**
     * Constructor.
     * 
     * @param princ name of subject
     * @param requester ID of requester
     * @param index <code>&lt;AttributeConsumingService&gt;</code> index
     * @param prot protocol ID
     * @param unfilteredFlag unfiltered flag
     */
    public ResolverTestRequest(@Nonnull @NotEmpty final String princ, @Nonnull @NotEmpty final String requester,
            @Nullable final Integer index, @Nullable final String prot, @Nullable final Boolean unfilteredFlag) {
        
        principal = Constraint.isNotNull(StringSupport.trimOrNull(princ), "Principal name cannot be null or empty");
        requesterId = Constraint.isNotNull(StringSupport.trimOrNull(requester),
                "Requester name cannot be null or empty");
        acsIndex = index;
        protocol = StringSupport.trimOrNull(prot);
        unfiltered = unfilteredFlag;
    }

    /**
     * Get the name of the subject.
     * 
     * @return name of the subject
     */
    @Nonnull @NotEmpty public String getPrincipal() {
        return principal;
    }

    /**
     * Get the ID of the requesting relying party.
     * 
     * @return ID of the requesting relying party
     */
    @Nonnull @NotEmpty public String getRequesterId() {
        return requesterId;
    }

    /**
     * Get the <code>&lt;AttributeConsumingService&gt;</code> index into metadata to apply.
     * 
     * @return <code>&lt;AttributeConsumingService&gt;</code> index into metadata, or null
     */
    @Nullable public Integer getAttributeConsumingServiceIndex() {
        return acsIndex;
    }

    /**
     * Get the protocol to apply to the results.
     * 
     * @return protocol to apply to the results
     */
    @Nullable public String getProtocol() {
        return protocol;
    }
    
    /**
     * Get whether to dump unfiltered attributes in protocol-neutral output.
     * 
     * @return whether to dump unfiltered attributes
     * 
     * @since 5.1.0
     */
    @Nullable public Boolean getUnfiltered() {
        return unfiltered;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("principal", principal)
            .add("requesterId", requesterId)
            .add("acsIndex", acsIndex)
            .add("protocol", protocol)
            .add("unfiltered", unfiltered)
            .toString();
    }
    
}