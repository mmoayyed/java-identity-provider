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

package net.shibboleth.idp.metrics.impl;

import com.codahale.metrics.Meter;

/**
 * A stubbed out meter implementation.
 * 
 * @since 3.3.0
 */
public class DisabledMeter extends Meter implements DisabledMetric {

    /** {@inheritDoc} */
    @Override public void mark() {
        
    }

    /** {@inheritDoc} */
    @Override public void mark(final long n) {
        
    }

    /** {@inheritDoc} */
    @Override public long getCount() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public double getFifteenMinuteRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public double getFiveMinuteRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public double getMeanRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public double getOneMinuteRate() {
        return 0;
    }

}