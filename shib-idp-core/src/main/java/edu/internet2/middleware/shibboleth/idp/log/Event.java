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

import org.opensaml.util.Strings;

/** 
 * An event to be logged. 
 * 
 * An event message is logged by calling the {@link #toString()} method on the Event.  It is expected 
 * that most events will result in a character separated list of name/value pairs fit for subsequent
 * processing by scripts or analysis software.  This is NOT requirement, just a general observation. 
 */
public abstract class Event {
    
    /** A human readable message associated with the event. This message should always come last in the event string. */
    private String message;

    /**
     * Gets the type of event.
     * 
     * @return the type of the event, used to help determine which log will receive the event.
     */
    public abstract String getType();
    
    /**
     * Gets the message associated with the event.
     * 
     * @return message associated with the event
     */
    public String getMessage(){
        return message;
    }
    
    /**
     * Sets the message associated with the event.
     * 
     * @param msg message associated with the event
     */
    public void setMessage(String msg){
        message = Strings.trimOrNull(msg);
    }
}