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

package edu.internet2.middleware.shibboleth.idp.log;

import net.jcip.annotations.NotThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/**
 * An event recording the performance of some operation.
 * 
 * This event produces an event string with the following format:
 * <code>operation|starTime|stopTime|elapsedTime|operationSuccess|message</code> where all times are given in
 * milliseconds, start and stop times are milliseconds since the epoch, and operation success is a 1 or 0.
 */
@NotThreadSafe
public class PerformanceEvent extends BaseEvent {

    /** Type for a performance event. */
    public static final String EVENT_TYPE = "performance";

    /** Character used to separate the fields of the event. */
    private static final String FIELD_SEPERATOR = "|";

    /** Identifier of the operation being timed. */
    private String operation;

    /** Whether the operation completed successfully or failed. */
    private boolean successfulOperation;

    /** System local time in milliseconds when the performance measurement started. */
    private long startTime;

    /** System local time in milliseconds when the performance measurement stopped. */
    private long stopTime;

    /** Total elapsed time, in milliseconds, of the operation. */
    private long elapsedTime;

    /**
     * Constructor.
     * 
     * @param operationId unique ID of the operation, must never be null or empty
     */
    public PerformanceEvent(String operationId) {
        operation = StringSupport.trimOrNull(operationId);
        Assert.isNotNull(operation, "Operation identifier may not be null or empty");

        successfulOperation = false;
        startTime = -1;
        stopTime = -1;
        elapsedTime = -1;
    }

    /** {@inheritDoc} */
    public String getType() {
        return EVENT_TYPE;
    }

    /**
     * Gets the operation being timed.
     * 
     * @return operation being timed
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Gets whether the operation was successful.
     * 
     * @return whether the operation was successful
     */
    public boolean isOperationSuccessful() {
        return successfulOperation;
    }

    /** Records the timer of the operation. May not be called more than one per event. */
    public void startTime() {
        if (startTime != -1) {
            throw new IllegalStateException("Event timer has already been started");
        }
        startTime = System.currentTimeMillis();
    }

    /**
     * Gets the system local time in milliseconds when the performance measurement started.
     * 
     * @return start time of the operation or -1 if {@link #startTime()} has not been called yet
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Records the completion time of the operation and whether the operation was successful.
     * 
     * @param wasSuccess whether the operation completed successfully
     */
    public void stopTime(boolean wasSuccess) {
        if (startTime == -1) {
            throw new IllegalStateException("Event timer has not been started");
        }

        stopTime = System.currentTimeMillis();
        elapsedTime = stopTime - startTime;
        successfulOperation = wasSuccess;
    }

    /**
     * Gets the System local time in milliseconds when the performance measurement stopped.
     * 
     * @return completion time of the operation or -1 if the {@link #stopTime(boolean)} has not been called yet
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * Gets the total elapsed time, in milliseconds, of the operation.
     * 
     * @return total elapsed time, in milliseconds, of the operation or -1 if {@link #stopTime(boolean)} has not been
     *         called yet
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuilder entry = new StringBuilder();
        entry.append(getOperation()).append(FIELD_SEPERATOR);
        entry.append(getStartTime()).append(FIELD_SEPERATOR);
        entry.append(getStopTime()).append(FIELD_SEPERATOR);
        entry.append(getElapsedTime()).append(FIELD_SEPERATOR);
        
        if(isOperationSuccessful()){
            entry.append(1).append(FIELD_SEPERATOR);
        }else{
            entry.append(0).append(FIELD_SEPERATOR);
        }
        
        if(getMessage() != null){
            entry.append(getMessage());
        }
        
        return entry.toString();
    }
}