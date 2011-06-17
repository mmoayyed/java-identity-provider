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

package net.shibboleth.idp.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple logging mechanism used to log events such as performance measurements and audit data.
 * 
 * Each event is logged to a logger named <code>idp.event.</code> + {@link BaseEvent#getType()}. So, for example,
 * {@link PerformanceEvent} events are logged to <code>idp.event.performance</code>. All events are logged at the INFO
 * level.
 */
public final class EventLogger {

    /**
     * Base name of the logger to which events are logged. The full logger name is this base, {@value} + "." +
     * {@link BaseEvent#getType()}
     */
    private static final String BASE_LOGGER_NAME = "idp.event";
    
    /** Constructor. */
    private EventLogger(){}

    /**
     * Logs the event.
     * 
     * @param event the event to be logged
     */
    public static void log(BaseEvent event) {
        // TODO consider whether this method should be asynchronous and the actual logging done by a background thread
        // in order to eliminate possible slowdowns caused by this logging
        
        Logger eventLog = LoggerFactory.getLogger(BASE_LOGGER_NAME + "." + event.getType());
        if (eventLog.isInfoEnabled()) {
            eventLog.info(event.toString());
        }
    }
}