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

package net.shibboleth.idp.saml.authn.principal;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.principal.CloneablePrincipal;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.xml.SerializeSupport;

import org.opensaml.core.xml.XMLRuntimeException;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.core.xml.util.XMLObjectSupport.CloneOutputOption;
import org.opensaml.saml.saml1.core.NameIdentifier;

/**
 * Principal based on the SAML2 {@link NameIdentifier}.
 */
public class NameIdentifierPrincipal implements CloneablePrincipal {

    /** The NameIdentifier. */
    @Nonnull private NameIdentifier nameIdentifier;
    
    /** Serialized form of nameIdenitifier . */
    @Nonnull @NotEmpty private String name;
    
    /** Constructor. 
     * @param theNameIdentifier the NameIdentifier which is wrapped.
     */
    public NameIdentifierPrincipal(
            @Nonnull @ParameterName(name="theNameIdentifier") final NameIdentifier theNameIdentifier) {
        nameIdentifier = Constraint.isNotNull(theNameIdentifier, "Supplied NameIdentifier cannot be null");
        try {
            name = SerializeSupport.nodeToString(Constraint.isNotNull(XMLObjectSupport.getMarshaller(theNameIdentifier),
                    "No marshaller for NameIdentifier").marshall(theNameIdentifier));
        } catch (final MarshallingException e) {
            throw new XMLRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public String getName() {
        return name;
    }
    
    /** Get the {@link NameIdentifier}. 
     * @return the nameIdentifier
     */
    @Nonnull public NameIdentifier getNameIdentifier() {
        return nameIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return nameIdentifier.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (other instanceof NameIdentifierPrincipal) {
            return nameIdentifier.equals(((NameIdentifierPrincipal) other).getNameIdentifier());
        }

        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public NameIdentifierPrincipal clone() throws CloneNotSupportedException {
        final NameIdentifierPrincipal copy = (NameIdentifierPrincipal) super.clone();
        try {
            copy.nameIdentifier = XMLObjectSupport.cloneXMLObject(nameIdentifier, 
                    CloneOutputOption.RootDOMInNewDocument);
            copy.name = SerializeSupport.nodeToString(
                    Constraint.isNotNull(XMLObjectSupport.getMarshaller(copy.nameIdentifier),
                            "No marshaller for NameIdentifier").marshall(copy.nameIdentifier));
        } catch (final MarshallingException | UnmarshallingException e) {
            throw new XMLRuntimeException(e);
        }
        return copy;
    }

}
