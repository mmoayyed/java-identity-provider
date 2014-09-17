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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1String;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link javax.security.auth.Subject}
 * into a principal name by searching for one and only one {@link X500Principal} custom principal.
 * 
 * <p>A list of OIDs is used to locate an RDN to extract from the DN and use as the principal name
 * after applying the transforms from the base class.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @pre <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) != null</pre>
 * @post <pre>SubjectCanonicalizationContext.getPrincipalName() != null
 *  || SubjectCanonicalizationContext.getException() != null</pre>
 */
public class X500SubjectCanonicalization extends AbstractSubjectCanonicalizationAction {

    /** Common Name (CN) OID. */
    private static final String CN_OID = "2.5.4.3";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(X500SubjectCanonicalization.class);
    
    /** Supplies logic for pre-execute test. */
    @Nonnull private final ActivationCondition embeddedPredicate;
    
    /** OIDs to search for. */
    @Nonnull @NonnullElements private List<String> objectIds;
    
    /** The custom Principal to operate on. */
    @Nullable private X500Principal x500Principal;
    
    /** Constructor. */
    public X500SubjectCanonicalization() {
        embeddedPredicate = new ActivationCondition();
        objectIds = Collections.singletonList(CN_OID);
    }
    
    /**
     * Set the OIDs to search for, in order of preference.
     * 
     * @param ids RDN OIDs to search for
     */
    public void setObjectIds(@Nonnull @NonnullElements final List<String> ids) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(ids, "OID list cannot be null");
        
        objectIds = Lists.newArrayListWithExpectedSize(ids.size());
        objectIds.addAll(StringSupport.normalizeStringCollection(ids));
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        if (embeddedPredicate.apply(profileRequestContext, c14nContext, true)) {
            x500Principal = c14nContext.getSubject().getPrincipals(X500Principal.class).iterator().next();
            return super.doPreExecute(profileRequestContext, c14nContext);
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        
        log.debug("{} Searching for RDN to extract from DN: {}", getLogPrefix(), x500Principal.getName());
        
        try (final ASN1InputStream asn1Stream = new ASN1InputStream(x500Principal.getEncoded())) {
            final ASN1Primitive dn = asn1Stream.readObject();
            for (final String oid : objectIds) {
                final String rdn = findRDN(dn, oid);
                if (rdn != null) {
                    log.debug("{} Extracted RDN with OID {}: {}", getLogPrefix(), oid, rdn);
                    c14nContext.setPrincipalName(applyTransforms(rdn));
                    return;
                }
            }
        } catch (final IOException e) {
            log.warn("{} Exception parsing DN", getLogPrefix(), e);
            c14nContext.setException(e);
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            return;
        }
    }
    
    /**
     * Find an RDN with the specified OID.
     * 
     * @param dn the DN object
     * @param oid the OID to look for
     * 
     * @return the first matching RDN value, or null
     */
    @Nullable protected String findRDN(@Nonnull final ASN1Primitive dn, @Nonnull @NotEmpty final String oid) {
        
         for (int i = 0; i < ((ASN1Sequence) dn).size(); i++) {
            final ASN1Primitive dnComponent = ((ASN1Sequence) dn).getObjectAt(i).toASN1Primitive();
            if (!(dnComponent instanceof ASN1Set)) {
                continue;
            }

            // Each DN component is a set
            for (int j = 0; j < ((ASN1Set) dnComponent).size(); j++) {
                final ASN1Sequence grandChild = (ASN1Sequence) ((ASN1Set) dnComponent).getObjectAt(j).toASN1Primitive();

                if (grandChild.getObjectAt(0) != null
                        && grandChild.getObjectAt(0).toASN1Primitive() instanceof ASN1ObjectIdentifier) {
                    final ASN1ObjectIdentifier componentId =
                            (ASN1ObjectIdentifier) grandChild.getObjectAt(0).toASN1Primitive();

                    if (oid.equals(componentId.getId())) {
                        // OK, this DN component is actually a matching attribute.
                        if (grandChild.getObjectAt(1) != null
                                && grandChild.getObjectAt(1).toASN1Primitive() instanceof ASN1String) {
                            return ((ASN1String) grandChild.getObjectAt(1).toASN1Primitive()).getString();
                        }
                    }
                }
            }
        }
         
        return null;
    }
     
    /** A predicate that determines if this action can run or not. */
    public static class ActivationCondition implements Predicate<ProfileRequestContext> {

        /** {@inheritDoc} */
        @Override
        public boolean apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final SubjectCanonicalizationContext c14nContext =
                        input.getSubcontext(SubjectCanonicalizationContext.class);
                if (c14nContext != null) {
                    return apply(input, c14nContext, false);
                }
            }
            
            return false;
        }

        /**
         * Helper method that runs either as part of the {@link Predicate} or directly from
         * the {@link X500SubjectCanonicalization#doPreExecute(ProfileRequestContext, SubjectCanonicalizationContext)}
         * method above.
         * 
         * @param profileRequestContext the current profile request context
         * @param c14nContext   the current c14n context
         * @param duringAction  true iff the method is run from the action above
         * @return true iff the action can operate successfully on the candidate contexts
         */
        public boolean apply(@Nonnull final ProfileRequestContext profileRequestContext,
                @Nonnull final SubjectCanonicalizationContext c14nContext, final boolean duringAction) {

            final Set<X500Principal> usernames;
            if (c14nContext.getSubject() != null) {
                usernames = c14nContext.getSubject().getPrincipals(X500Principal.class);
            } else {
                usernames = null;
            }
            
            if (duringAction) {
                if (usernames == null || usernames.isEmpty()) {
                    c14nContext.setException(
                            new SubjectCanonicalizationException("No X500Principals were found"));
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                    return false;
                } else if (usernames.size() > 1) {
                    c14nContext.setException(
                            new SubjectCanonicalizationException("Multiple X500Principals were found"));
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                    return false;
                }
                
                return true;
            } else {
                return usernames != null && usernames.size() == 1;
            }
        }
        
    }

}