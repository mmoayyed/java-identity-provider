/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.AbstractResolutionPlugIn;


/**
 * Base class for {@link PrincipalConnector} plug-ins.
 */
public abstract class BasePrincipalConnector extends AbstractResolutionPlugIn<String> implements
        PrincipalConnector {
    
    /** NameID Format. **/
    private String nameIDFormat;

    /**
     * Get the NameID format.
     * 
     * @return the nameIDFormat.
     */
    public String getNameIDFormat() {
        return nameIDFormat;
    }

    /**
     * Set the NameID format.
     * 
     * @param newFormat the new nameIDFormat to set.
     */
    public void setNameIDFormat(String newFormat) {
        nameIDFormat = newFormat;
    }
       
}