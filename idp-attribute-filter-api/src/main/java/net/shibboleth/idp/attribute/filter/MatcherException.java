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

package net.shibboleth.idp.attribute.filter;

/**
 * Specific exception to handle tristated results from filtering. <br />
 * When a filter fails due to bad input then is no safe value to return. Instead we have to signal the failure to the
 * upper layers. We do this via tristating. But to keep the code easy we use an exception. So as to not get in the way
 * of other structures we make it run time.<br/>
 * 
 * TODO - this represents a current position.  We need to revisit whether this is the correct call towards the end
 * of the development process.
 */
public class MatcherException extends RuntimeException {

    
    /**
     * version UID.
     */
    private static final long serialVersionUID = 599342584063832167L;

    /**
     * Constructor.
     *
     * @param string what to say
     */
    public MatcherException(String string) {
        super(string);
    }

}
