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

package net.shibboleth.idp.attribute.resolver;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;

import com.google.common.base.Function;

/**
 * The definition of the API for all SAML Subject Name Connectors.<b/> TODO This is highly liklyl to change, or even
 * become irrelevant.
 */
public interface SubjectNamePrincipalConnectorDefinition extends ResolverPlugin<String>,
        PrincipalConnectorDefinition<AttributeResolutionContext> {

    /**
     * Get NameID format.
     * 
     * @return the NameID format
     */
    @Nullable String getFormat();

    /**
     * Set NameID format.
     * 
     * @param format the NameID format
     */
    void setFormat(@Nonnull final String format);

    /**
     * Get relying parties this connector is valid for.
     * 
     * @return set of relying parties
     */
    @Unmodifiable @Nonnull Set<String> getRelyingParties();

    /**
     * Set relying parties this connector is valid for.
     * 
     * @param parties set of relying parties.
     */
    void setRelyingParties(@Nonnull final Set<String> parties);

    /**
     * Function to set up the context navigation mechanism. This is used to go from a {@link AttributeResolutionContext}
     * to a {@link MessageContext} containing a SAML protocol message represented by a {@link SAMLObject}.
     * 
     * @param function the navigation function.
     */
    void setContextFinderStrategy(Function<AttributeResolutionContext, MessageContext<SAMLObject>> function);

    /**
     * Helper function to find the IssuerId ("entityID of SP") for this message. This allow the attribute resolver to
     * not be involved in the required navigation.
     * 
     * @param context the resolution context.
     * @return the IssuerID never null
     * @throws ResolutionException if we could not navigate the structures directly.
     * 
     @Nonnull public String issuerIdOf(final AttributeResolutionContext context) throws ResolutionException { return
     *          locateSamlMessageContext(context).getSubcontext(SAMLPeerEntityContext.class, true).getEntityId(); }
     */

    /**
     * Helper function to find the format (from the {@link NameID} or {@link NameIdentifier}) for this message. This
     * allow the attribute resolver to not be involved in the required navigation.
     * 
     * @param context the resolution context.
     * @return the format never null
     * @throws ResolutionException if we could not navigate the structures directly.
     * 
     @Nonnull public String formatOf(final AttributeResolutionContext context) throws ResolutionException { final
     *          SAMLObject object =
     *          locateSamlMessageContext(context).getSubcontext(SAMLSubjectNameIdentifierContext.class, true)
     *          .getSubjectNameIdentifier();
     * 
     *          if (object instanceof NameID) { final NameID nameId = (NameID) object; return nameId.getFormat(); } else
     *          if (object instanceof NameIdentifier) { final NameIdentifier nameIdentifier = (NameIdentifier) object;
     *          return nameIdentifier.getFormat(); } throw new ResolutionException(getLogPrefix() + " message was a " +
     *          (object == null ? "null" : object.getClass().toString()) + ", not a NameId or a NameIdentifier"); }
     */

}
