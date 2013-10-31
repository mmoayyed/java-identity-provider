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

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SamlPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SamlSubjectNameIdentifierContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * The base implementation of all SAML Subject Name Connectors.<b/> This takes on the heavy lifting of finding the
 * MessageContext containing the SAML message being processed, looking up the nameID and so forth. Concrete
 * implementations take care of the other plumbing.
 * 
 * TODO This is highly likely to change, or even become irrelevant.
 */
public abstract class AbstractSubjectNamePrincipalConnectorDefinition extends AbstractResolverPlugin<String> implements
        SubjectNamePrincipalConnectorDefinition {

    /**
     * The nameID format we are interested in.
     */
    private String nameIDFormat;

    /** The log prefix. */
    private String logPrefix;

    /**
     * Those relying parties we serve.
     */
    private Set<String> relyingParties = Collections.EMPTY_SET;

    /**
     * The strategy to resolve the message context containing the SAML message being processed.
     */
    private Function<AttributeResolutionContext, MessageContext<SAMLObject>> contextFinderStrategy;

    /**
     * Get NameID format.
     * 
     * @return the NameID format
     */
    @Nullable public String getFormat() {
        return nameIDFormat;
    }

    /**
     * Set NameID format.
     * 
     * @param format the NameID format
     */
    public void setFormat(@Nonnull final String format) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIDFormat = StringSupport.trimOrNull(format);
    }

    /**
     * Get relying parties this connector is valid for.
     * 
     * @return set of relying parties
     */
    @Unmodifiable @Nonnull public Set<String> getRelyingParties() {
        return relyingParties;
    }

    /**
     * Set relying parties this connector is valid for.
     * 
     * @param parties set of relying parties.
     */
    public void setRelyingParties(@Nonnull final Set<String> parties) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        relyingParties = ImmutableSet.copyOf(Constraint.isNotNull(parties, "relying parties cannot be null"));
    }

    /**
     * Function to set up the context navigation mechanism. This is used to go from a {@link AttributeResolutionContext}
     * to a {@link MessageContext} containing a SAML protocol message represented by a {@link SAMLObject}.
     * 
     * @param function the navigation function.
     */
    public void
            setContextFinderStrategy(final Function<AttributeResolutionContext, MessageContext<SAMLObject>> function) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        contextFinderStrategy = function;
    }

    /**
     * Helper function to get the {@link MessageContext} from our context.
     * 
     * @param inputContext What we are passed
     * @return the {@link MessageContext} never null.
     * @throws ResolutionException if we could not find the context.
     */
    @Nonnull protected MessageContext<SAMLObject>
            locateSamlMessageContext(final AttributeResolutionContext inputContext) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        final MessageContext<SAMLObject> samlContext = contextFinderStrategy.apply(inputContext);
        if (null == samlContext) {
            throw new ResolutionException(getLogPrefix() + " could not locate input message");
        }
        return samlContext;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        logPrefix = null;
        if (null == nameIDFormat) {
            throw new ComponentInitializationException(getLogPrefix() + " no valid format supplied");
        }
        if (null == contextFinderStrategy) {
            throw new ComponentInitializationException(getLogPrefix()
                    + " no valid SAML context finder strategy supplied");
        }
    }

    /**
     * Helper function to find the IssuerId ("entityID of SP") for this message. This allow the attribute resolver to
     * not be involved in the required navigation.
     * 
     * @param context the resolution context.
     * @return the IssuerID never null
     * @throws ResolutionException if we could not navigate the structures directly.
     */
    @Nonnull public String issuerIdOf(final AttributeResolutionContext context) throws ResolutionException {
        return locateSamlMessageContext(context).getSubcontext(SamlPeerEntityContext.class, true).getEntityId();
    }

    /**
     * Helper function to find the format (from the {@link NameID} or {@link NameIdentifier}) for this message. This
     * allow the attribute resolver to not be involved in the required navigation.
     * 
     * @param context the resolution context.
     * @return the format never null
     * @throws ResolutionException if we could not navigate the structures directly.
     */
    @Nonnull public String formatOf(final AttributeResolutionContext context) throws ResolutionException {
        final SAMLObject object =
                locateSamlMessageContext(context).getSubcontext(SamlSubjectNameIdentifierContext.class, true)
                        .getSubjectNameIdentifier();

        if (object instanceof NameID) {
            final NameID nameId = (NameID) object;
            return nameId.getFormat();
        } else if (object instanceof NameIdentifier) {
            final NameIdentifier nameIdentifier = (NameIdentifier) object;
            return nameIdentifier.getFormat();
        }
        throw new ResolutionException(getLogPrefix() + " message was a "
                + (object == null ? "null" : object.getClass().toString()) + ", not a NameId or a NameIdentifier");
    }

    /**
     * Helper function to find the context (from the {@link NameID} or {@link NameIdentifier}) for this message. The
     * superclasses may use this to do the lookup.
     * 
     * @param context the resolution context.
     * @return the format never null
     * @throws ResolutionException if we could not navigate the structures directly.
     */
    @Nonnull protected String contentOf(final AttributeResolutionContext context) throws ResolutionException {
        final SAMLObject object =
                locateSamlMessageContext(context).getSubcontext(SamlSubjectNameIdentifierContext.class, true)
                        .getSubjectNameIdentifier();

        if (object instanceof NameID) {
            final NameID nameId = (NameID) object;
            return nameId.getValue();
        } else if (object instanceof NameIdentifier) {
            final NameIdentifier nameIdentifier = (NameIdentifier) object;
            return nameIdentifier.getNameIdentifier();
        }
        throw new ResolutionException(getLogPrefix() + " message was a "
                + (object == null ? "null" : object.getClass().toString()) + ", not a NameId or a NameIdentifier");
    }

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Principal Connector '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing of per class cache.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Principal Connector '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }
}
