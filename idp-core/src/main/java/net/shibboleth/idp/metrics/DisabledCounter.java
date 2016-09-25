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

package net.shibboleth.idp.metrics;

import com.codahale.metrics.Counter;

/**
 * A stubbed out counter implementation.
 * 
 * @since 3.3.0
 */
public class DisabledCounter extends Counter implements DisabledMetric {

    /** {@inheritDoc} */
    @Override public void inc() {
        
    }

    /** {@inheritDoc} */
    @Override public void inc(final long n) {
        
    }

    /** {@inheritDoc} */
    @Override public void dec() {
        
    }

    /** {@inheritDoc} */
    @Override public void dec(final long n) {
        
    }

    /** {@inheritDoc} */
    @Override public long getCount() {
        return 0;
    }

}