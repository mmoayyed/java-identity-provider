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

package net.shibboleth.idp.consent.flow.impl;

import java.time.Duration;

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;

/**
 * Descriptor for a consent flow.
 * 
 * A consent flow models a sequence of actions which retrieves consent from storage as well as extracts consent from
 * user input.
 */
public class ConsentFlowDescriptor extends ProfileInterceptorFlowDescriptor {

    /** Whether consent equality includes comparing consent values. */
    private boolean compareValues;

    /** Time to expire consent storage records. Default value: 1 year. */
    @Nullable private Duration lifetime;

    /** Maximum number of records stored in the storage service. */
    private int maxStoredRecords;

    /** Expanded maximum number of records stored in the storage service. */
    private int expandedMaxStoredRecords;
    
    /** Value size at which expanded maximum takes effect. Default value : 1024 * 1024 */
    private long expandedStorageThreshold;

    /** Constructor. */
    public ConsentFlowDescriptor() {
        expandedStorageThreshold = 1024 * 1024;
    }

    /**
     * Whether consent equality includes comparing consent values.
     * 
     * @return true if consent equality includes comparing consent values
     */
    public boolean compareValues() {
        return compareValues;
    }

    /**
     * Time to expire consent storage records.
     * 
     * @return time to expire consent storage records, null for infinite.
     */
    @Nullable public Duration getLifetime() {
        return lifetime;
    }

    /**
     * Get the maximum number of records to keep in the storage service if the expanded size threshold is not met.
     * 
     * @return the maximum number of records, or &lt;=0 for no limit
     */
    public int getMaximumNumberOfStoredRecords() {
        return maxStoredRecords;
    }

    /**
     * Get the maximum number of records to keep in the storage service if the expanded size threshold is met.
     * 
     * @return the maximum number of records, or &lt;=0 for no limit
     */
    public int getExpandedNumberOfStoredRecords() {
        return expandedMaxStoredRecords;
    }
    
    /**
     * Get the storage value size at which the expanded maximum record size kicks in.
     * 
     * @return  storage value size to enable expanded record maximum
     */
    public long getExpandedStorageThreshold() {
        return expandedStorageThreshold;
    }

    /**
     * Set whether consent equality includes comparing consent values.
     * 
     * @param flag true if consent equality includes comparing consent values
     */
    public void setCompareValues(final boolean flag) {
        checkSetterPreconditions();
        compareValues = flag;
    }

    /**
     * Set time to expire consent storage records.
     * 
     * @param consentLifetime time to expire consent storage records.  null means infinite
     */
    public void setLifetime(@Nullable final Duration consentLifetime) {
        checkSetterPreconditions();
        lifetime = consentLifetime;
    }

    /**
     * Set the maximum number of records to keep in the storage service if the expanded size threshold is not met.
     * 
     * @param maximum the maximum number of records, or &lt;=0 for no limit
     */
    public void setMaximumNumberOfStoredRecords(final int maximum) {
        checkSetterPreconditions();
        maxStoredRecords = maximum;
    }

    /**
     * Set the maximum number of records to keep in the storage service if the expanded size threshold is met.
     * 
     * @param maximum the maximum number of records, or &lt;=0 for no limit
     */
    public void setExpandedNumberOfStoredRecords(final int maximum) {
        checkSetterPreconditions();
        expandedMaxStoredRecords = maximum;
    }

    /**
     * Set the storage value size at which the expanded maximum record size kicks in.
     * 
     * <p>Defaults to 1024^2</p>
     * 
     * @param size size threshold
     */
    public void setExpandedStorageThreshold(final long size) {
        checkSetterPreconditions();
        expandedStorageThreshold = size;
    }
}