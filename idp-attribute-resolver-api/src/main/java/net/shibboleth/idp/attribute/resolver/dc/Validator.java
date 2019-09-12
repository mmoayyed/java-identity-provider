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

package net.shibboleth.idp.attribute.resolver.dc;

/** Used to determine whether a Data Connector initialized properly and continues to be fit for use. */
public interface Validator {

    /**
     * Probe the data connector and conditionally fails if it is not valid and ready for use.
     * 
     * @throws ValidationException thrown if validation fails and {@link #isThrowValidateError()} is true
     */
    void validate() throws ValidationException;
    
    /**
     * Sets whether {@link #validate()} should throw or log errors.
     *
     * @param what whether {@link #validate()} should throw or log errors
     */
    public void setThrowValidateError(final boolean what);

    /**
     * Returns whether {@link #validate()} should throw or log errors.
     *
     * @return whether {@link #validate()} should throw or log errors
     */
    public boolean isThrowValidateError();
}