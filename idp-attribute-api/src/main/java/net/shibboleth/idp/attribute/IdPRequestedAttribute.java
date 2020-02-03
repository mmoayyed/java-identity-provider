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

package net.shibboleth.idp.attribute;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

/**
 * IdP Representation of the SAML2 RequestedAttribute.
 */
public final class IdPRequestedAttribute extends IdPAttribute {

    /** Is this attribute required. */
    private boolean isRequired;

    /**
     * Constructor.
     *
     * @param attributeId the id
     */
    public IdPRequestedAttribute(final @ParameterName(name="attributeId")  String attributeId) {
        super(attributeId);
    }

    /**
     * Is this attribute marked as required?
     * 
     * @return the isRequired flag
     * 
     * @since 4.0.0
     */
    public boolean isRequired() {
        return isRequired;
    }
    
    /**
     * Is this attribute marked as required?
     * 
     * @return the isRequired flag
     * 
     * @deprecated remove in V5
     */
    @Deprecated(forRemoval=true,since="4.0.0")
    public boolean getIsRequired() {
        DeprecationSupport.warn(ObjectType.METHOD, "getIsRequired", getClass().getName(), "isRequired");
        return isRequired;
    }

    /**
     * Set whether this attribute is to be marked as required.
     * 
     * @param required The flag to set.
     */
    public void setRequired(final boolean required) {
        isRequired = required;
    }

}