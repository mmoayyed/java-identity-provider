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

import java.util.HashSet;
import java.util.Set;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.AbstractResolutionPlugIn;


/**
 * Base class for {@link PrincipalConnector} plug-ins.
 */
public abstract class BasePrincipalConnector extends AbstractResolutionPlugIn<String> implements
        PrincipalConnector {
    
    /** NameID Format. */
    private String format;

    /** Relying parties this connector is valid for. */
    private Set<String> relyingParties;

    /** Constructor. */
    public BasePrincipalConnector() {
        relyingParties = new HashSet<String>();
    }

    /**
     * Set NameID format.
     * 
     * @param newFormat new NameID format
     */
    public void setFormat(String newFormat) {
        format = newFormat;
    }

    /** {@inheritDoc} */
    public String getFormat() {
        return format;
    }

    /** {@inheritDoc} */
    public Set<String> getRelyingParties() {
        return relyingParties;
    }  
}