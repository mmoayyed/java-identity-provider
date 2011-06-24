/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.relyingparty;

import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;

/** Base class for {@link ProfileConfiguration} implementations. */
public abstract class AbstractProfileConfiguration implements ProfileConfiguration {

    /** ID of the profile configured. */
    private String id;

    /** Whether this profile is enabled. */
    private boolean enabled;

    /**
     * Constructor.
     * 
     * @param profileId ID of the the communication profile, never null or empty
     */
    public AbstractProfileConfiguration(String profileId) {
        String trimmedId = StringSupport.trimOrNull(profileId);
        Assert.isNotNull(trimmedId, "Profile identifier can not be null or empty");
        id = profileId;

        enabled = true;
    }

    /** {@inheritDoc} */
    public String getProfileId() {
        return id;
    }

    /** {@inheritDoc} */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this profile is enabled.
     * 
     * @param isEnabled whether this profile is enabled
     */
    public void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AbstractProfileConfiguration)) {
            return false;
        }

        AbstractProfileConfiguration other = (AbstractProfileConfiguration) obj;
        return ObjectSupport.equals(id, other.getProfileId());
    }
}