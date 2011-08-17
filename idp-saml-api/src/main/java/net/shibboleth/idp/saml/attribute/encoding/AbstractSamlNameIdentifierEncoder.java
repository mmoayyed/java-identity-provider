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

package net.shibboleth.idp.saml.attribute.encoding;

import net.shibboleth.idp.attribute.AttributeEncoder;

import org.opensaml.util.StringSupport;

/**
 * Base class for attribute encoders that produce SAML name identifiers.
 * 
 * @param <NameIdType> type of name identifier produced by this encoder
 */
public abstract class AbstractSamlNameIdentifierEncoder<NameIdType> implements AttributeEncoder<NameIdType> {

    /** The format of the name identifier. */
    private String format;

    /** The security or administrative domain that qualifies the name identifier. */
    private String qualifier;

    /**
     * Gets the format of the name identifier.
     * 
     * @return format of the name identifier
     */
    public final String getNameFormat() {
        return format;
    }

    /**
     * Sets the format of the name identifier.
     * 
     * @param nameFormat format of the name identifier
     */
    public final void setNameFormat(String nameFormat) {
        format = StringSupport.trimOrNull(nameFormat);
    }

    /**
     * Gets the security or administrative domain that qualifies the name identifier.
     * 
     * @return security or administrative domain that qualifies the name identifier
     */
    public final String getNameQualifier() {
        return qualifier;
    }

    /**
     * Sets the security or administrative domain that qualifies the name identifier.
     * 
     * @param nameQualifier security or administrative domain that qualifies the name identifier
     */
    public final void setNameQualifier(String nameQualifier) {
        qualifier = StringSupport.trimOrNull(nameQualifier);
    }
}