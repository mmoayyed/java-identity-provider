/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple logging mechanism used to log events such as performance measurements and audit data.
 * 
 * Each event is logged to a logger named <code>idp.event.</code> + {@link Event#getType()}. So, for example,
 * {@link PerformanceEvent} events are logged to <code>idp.event.performance</code>. All events are logged at the INFO
 * level.
 */
public class EventLogger {

    private final static String BASE_LOGGER_NAME = "idp.event";

    /**
     * Logs the event.
     * 
     * @param event
     */
    public void log(Event event) {
        // TODO consider whether this method should be asynchronous and the actual logging done by a background thread in order to eliminate possible slowdowns caused by this logging
        // TODO figure out if keeping a cache of these loggers is possible of if logger config reloading poses a problem
        Logger eventLog = LoggerFactory.getLogger(BASE_LOGGER_NAME + "." + event.getType());
        if(eventLog.isInfoEnabled()){
            eventLog.info(event.toString());
        }
    }
}