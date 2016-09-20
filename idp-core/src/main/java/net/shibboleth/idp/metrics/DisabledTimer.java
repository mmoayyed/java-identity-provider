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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

/**
 * A stubbed out timer implementation.
 * 
 * @since 3.3.0
 */
public class DisabledTimer extends Timer implements DisabledMetric {

    /**
     * Constructor.
     */
    public DisabledTimer() {
        super(null);
    }
    
    /** {@inheritDoc} */
    @Override
    public void update(long duration, TimeUnit unit) {
        
    }

    /** {@inheritDoc} */
    @Override
    public <T> T time(Callable<T> event) throws Exception {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Context time() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public long getCount() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double getFifteenMinuteRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double getFiveMinuteRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double getMeanRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double getOneMinuteRate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public Snapshot getSnapshot() {
        return null;
    }
    
}