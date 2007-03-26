/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.xmlobject;

import javax.xml.namespace.QName;

import org.opensaml.common.SAMLObject;
import org.opensaml.xml.schema.XSString;

/**
 * Represents a string value that contains an attribute containing a scope.
 */
public interface ShibbolethScopedValue extends XSString, SAMLObject {

    /** Local name of the XSI type. */
    public static final String TYPE_LOCAL_NAME = "ScopedValue";

    /** QName of the XSI type. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:attribute:encoder", TYPE_LOCAL_NAME,
            "shib");

    /** 
     * Gets the name of the scope attribute.
     * 
     * @return name of the scope attribute
     */
    public String getScopeAttributeName();
    
    /**
     * Sets the name of the scope attribute.
     * 
     * @param attribute name of the scope attribute
     */
    public void setScopeAttributeName(String attribute);
    
    /**
     * Gets the scope value.
     * 
     * @return scope value
     */
    public String getScope();
    
    /**
     * Sets the scope value.
     * 
     * @param scope scope value
     */
    public void setScope(String scope);
}