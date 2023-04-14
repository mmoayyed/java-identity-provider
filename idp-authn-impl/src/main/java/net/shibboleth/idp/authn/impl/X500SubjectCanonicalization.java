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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.cryptacular.x509.dn.Attribute;
import org.cryptacular.x509.dn.NameReader;
import org.cryptacular.x509.dn.RDN;
import org.cryptacular.x509.dn.RDNSequence;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.x509.X509Support;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;


/**
 * An action that operates on a {@link SubjectCanonicalizationContext} child of the current
 * {@link ProfileRequestContext}, and transforms the input {@link javax.security.auth.Subject}
 * into a principal name by searching for one and only one {@link X509Certificate} public credential,
 * or in its absence one and only one {@link X500Principal}.
 * 
 * <p>A list of OIDs is used to locate an RDN to extract from the Subject DN and use as the principal name
 * after applying the transforms from the base class.</p>
 * 
 * <p>Alternatively, a list of subjectAltName extension types may be specified, which takes precedence
 * over the subject, if a match is found.</p>
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
    
    /** subjectAltName types to search for. */
    @Nonnull @NonnullElements private List<Integer> subjectAltNameTypes;
    
    /** OIDs to search for. */
    @Nonnull @NonnullElements private List<String> objectIds;
    
    /** The certificate to operate on. */
    @Nullable private X509Certificate certificate;
    
    /** The subject DN to operate on. */
    @NonnullBeforeExec private X500Principal x500Principal;
    
    /** Constructor. */
    public X500SubjectCanonicalization() {
        subjectAltNameTypes = CollectionSupport.emptyList();
        objectIds = CollectionSupport.singletonList(CN_OID);
    }

    /**
     * Set the subjectAltName types to search for, in order of preference.
     * 
     * @param types types to search for
     */
    public void setSubjectAltNameTypes(@Nullable @NonnullElements final List<Integer> types) {
        checkSetterPreconditions();
        if (types != null) {
            subjectAltNameTypes = CollectionSupport.copyToList(types);
        } else {
            subjectAltNameTypes = CollectionSupport.emptyList();
        }
    }

    /**
     * Set the OIDs to search for, in order of preference.
     * 
     * @param ids RDN OIDs to search for
     */
    public void setObjectIds(@Nullable @NonnullElements final List<String> ids) {
        checkSetterPreconditions();
        objectIds = CollectionSupport.copyToList(StringSupport.normalizeStringCollection(ids));
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        final Subject subject = c14nContext.getSubject();
        assert subject != null;
        final Set<X509Certificate> certificates =
                subject.getPublicCredentials(X509Certificate.class);
        if (certificates != null && certificates.size() == 1) {
            certificate = certificates.iterator().next();
            assert certificate != null;
            x500Principal = certificate.getSubjectX500Principal();
        } else {
            final Set<X500Principal> principals = subject.getPrincipals(X500Principal.class);
            if (principals != null && principals.size() == 1) {
                x500Principal = principals.iterator().next();
            }
        }
        
        if (x500Principal != null) {
            return super.doPreExecute(profileRequestContext, c14nContext);
        }
        
        c14nContext.setException(new SubjectCanonicalizationException(
                "Neither a single X509Certificate nor X500Principal were found"));
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
        return false;
    }
    
    /** {@inheritDoc} */
 // Checkstyle: ReturnCount OFF
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext, 
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        final X509Certificate localCopy = certificate;
        if (localCopy != null && !subjectAltNameTypes.isEmpty()) {
            log.debug("{} Searching for subjectAltName types ({})", getLogPrefix(), subjectAltNameTypes);
            final List<?> altnames = X509Support.getAltNames(localCopy, subjectAltNameTypes.toArray(new Integer[0]));
            assert altnames != null;
            for (final Object altname : altnames) {
                if (altname instanceof String) {
                    log.debug("{} Extracted String-valued subjectAltName: {}", getLogPrefix(), altname);
                    c14nContext.setPrincipalName(applyTransforms((String) altname));
                    return;
                }
            }
            log.debug("{} No suitable subjectAltName extension");
        }
        final String x509PrincipalName = x500Principal.getName();
        log.debug("{} Searching for RDN to extract from DN: {}", getLogPrefix(), x509PrincipalName);
        
        try {
            final RDNSequence dnAttrs = NameReader.readX500Principal(x500Principal);
            assert dnAttrs != null;
            for (final String oid : objectIds) {
                assert oid != null;
                final String rdn = findRDN(dnAttrs, oid);
                if (rdn != null) {
                    log.debug("{} Extracted RDN with OID {}: {}", getLogPrefix(), oid, rdn);
                    c14nContext.setPrincipalName(applyTransforms(rdn));
                    return;
                }
            }
            
            log.warn("{} Unable to extract a suitable RDN from DN: {}", getLogPrefix(), x509PrincipalName);
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            
        } catch (final IllegalArgumentException e) {
            log.warn("{} Unable to parse subject DN: {}", getLogPrefix(),  x509PrincipalName, e);
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            return;
        }
    }
 // Checkstyle: ReturnCount ON
    
    /**
     * Find an RDN with the specified OID.
     * 
     * @param sequence the DN components
     * @param oid the OID to look for
     * 
     * @return the first matching RDN value, or null
     */
    @Nullable protected String findRDN(@Nonnull final RDNSequence sequence, @Nonnull @NotEmpty final String oid) {
        
        // We use backward() here because otherwise the library returns attributes in least to most-specific order.
        for (final RDN rdn : sequence.backward()) {
            for (final Attribute attribute : rdn.getAttributes()) {
                if (attribute.getType().getOid().equals(oid)) {
                    return attribute.getValue();
                }
            }
        }
        
        return null;
    }
     
    /** A predicate that determines if this action can run or not. */
    public static class ActivationCondition implements Predicate<ProfileRequestContext> {

        /** {@inheritDoc} */
        public boolean test(@Nullable final ProfileRequestContext input) {
            
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

            final Subject subject = c14nContext.getSubject();
            if (subject != null) {
                final Set<X509Certificate> certificates =
                        subject.getPublicCredentials(X509Certificate.class);
                if (certificates != null && certificates.size() == 1) {
                    return true;
                }
                
                final Set<X500Principal> principals = subject.getPrincipals(X500Principal.class);
                if (principals != null && principals.size() == 1) {
                    return true;
                }
            }
            
            c14nContext.setException(new SubjectCanonicalizationException(
                    "Neither a single X509Certificate nor X500Principal were found"));
            if (duringAction) {
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            }
            
            return false;
        }
        
    }

}
