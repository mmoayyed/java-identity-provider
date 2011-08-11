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

package net.shibboleth.idp.saml.metadata;

import javax.xml.namespace.QName;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.criteria.Criterion;

/** Represents a criteria that a particular role is supported. */
public class SupportedRoleCriterion implements Criterion {

    /** Role that must be supported by the entity. */
    private QName role;

    /**
     * Constructor.
     * 
     * @param supportedRole role that must be supported by the entity, never null and must contain a namespace
     */
    public SupportedRoleCriterion(QName supportedRole) {
        Assert.isNotNull(role, "Supported role can not be null");
        Assert.isFalse(StringSupport.isNullOrEmpty(supportedRole.getLocalPart()), "Role QName must have a local name");
        Assert.isFalse(StringSupport.isNullOrEmpty(supportedRole.getNamespaceURI()), "Role QName must have a namespace");
        role = supportedRole;
    }

    /**
     * Gets the role that must be supported by the entity.
     * 
     * @return role that must be supported by the entity, never null or empty
     */
    public QName getSupportedRole() {
        return role;
    }
}