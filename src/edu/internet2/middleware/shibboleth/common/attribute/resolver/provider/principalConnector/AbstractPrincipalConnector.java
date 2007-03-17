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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.NameID;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.AbstractResolutionPlugIn;

/**
 * Abstract base class for principal connectors.
 */
public abstract class AbstractPrincipalConnector extends AbstractResolutionPlugIn<String> implements PrincipalConnector {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(AbstractPrincipalConnector.class);

    /** NameID Format. */
    private String format;

    /** Relying parties this connector is valid for. */
    private Set<String> relyingParties;

    /** Constructor. */
    public AbstractPrincipalConnector() {
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

    /**
     * Verify that the provided NameID is valid for this Identity Provider.
     * 
     * @param subject NameID to verify
     * @param provider the local provider ID
     * @throws AttributeResolutionException if subject is not valid for this provider
     */
    protected void verifyQualifier(NameID subject, String provider) throws AttributeResolutionException {

        // TODO: how do we know if we should compare against the nameQualifier or spNameQualifier? The resolver will
        // be used in both the IdP and SP

        String nameQualifier = subject.getNameQualifier();
        // String nameQualifier = subject.getSPNameQualifier();

        if (provider == null || !provider.equals(nameQualifier)) {
            log.error("The name qualifier (" + nameQualifier
                    + ") for the referenced subject is not valid for this provider.");
            throw new AttributeResolutionException("The name qualifier (" + nameQualifier
                    + ") for the referenced subject is not valid for this provider.");
        }
    }

}