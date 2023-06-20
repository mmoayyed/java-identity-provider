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

package net.shibboleth.idp.session.logic;

import java.util.Collection;
import java.util.function.BiPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.net.InetAddresses;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.net.IPRange;

/** A {@link BiPredicate} that checks if a pair of addresses are either equal, or share an {@link IPRange}. */
public class IPRangeBiPredicate implements BiPredicate<String,String> {
    
    /** IP ranges to match against. */
    @Nonnull private Collection<IPRange> addressRanges;

    /** Constructor. */
    IPRangeBiPredicate() {
        addressRanges = CollectionSupport.emptyList();
    }
    
    /**
     * Set the address ranges to check against.
     * 
     * @param ranges    address ranges to check against
     */
    public void setRanges(@Nonnull final Collection<IPRange> ranges) {
        Constraint.isNotNull(ranges, "Address range collection cannot be null");
        
        addressRanges = CollectionSupport.copyToList(ranges);
    }
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final String input1, @Nullable final String input2) {
        
        if (input1 == null || input2 == null) {
            return false;
        } else if (input1.equals(input2)) {
            return true;
        }
        
        for (final IPRange range : addressRanges) {
            if (range.contains(InetAddresses.forString(input1)) && range.contains(InetAddresses.forString(input2))) {
                return true;
            }
        }
        
        return false;
    }
    
}