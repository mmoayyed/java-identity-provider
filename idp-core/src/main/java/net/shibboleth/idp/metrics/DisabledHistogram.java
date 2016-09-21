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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;

/**
 * A stubbed out histogram implementation.
 * 
 * @since 3.3.0
 */
public class DisabledHistogram extends Histogram implements DisabledMetric {

    /**
     * Constructor.
     */
    public DisabledHistogram() {
        super(null);
    }

    /** {@inheritDoc} */
    @Override public void update(int value) {
        
    }

    /** {@inheritDoc} */
    @Override public void update(long value) {
        
    }

    /** {@inheritDoc} */
    @Override public long getCount() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public Snapshot getSnapshot() {
        return null;
    }
    
}