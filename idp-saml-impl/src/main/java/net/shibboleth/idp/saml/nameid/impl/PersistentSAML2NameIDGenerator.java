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

package net.shibboleth.idp.saml.nameid.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.profile.AbstractSAML2NameIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.attribute.PairwiseIdStore;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Generator for "persistent" Format {@link NameID} objects that provides a source/seed ID based on {@link IdPAttribute}
 * data.
 */
@ThreadSafeAfterInit
public class PersistentSAML2NameIDGenerator extends AbstractSAML2NameIDGenerator {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PersistentSAML2NameIDGenerator.class);

    /** Strategy function to lookup SubjectContext. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> subjectContextLookupStrategy;

    /** Strategy function to lookup AttributeContext. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;

    /** Attribute(s) to use as an identifier source. */
    @Nonnull @NonnullElements private List<String> attributeSourceIds;

    /** Store for IDs. */
    @NonnullAfterInit private PairwiseIdStore pidStore;
    
    /** A DataSource to auto-provision a {@link JDBCPairwiseIdStore} instance. */
    @Nullable private DataSource dataSource;

    /** Predicate to select whether to look at filtered or unfiltered attributes. */
    private boolean useUnfilteredAttributes;

    /** Constructor. */
    public PersistentSAML2NameIDGenerator() {
        setFormat(NameID.PERSISTENT);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        attributeContextLookupStrategy =
                new ChildContextLookup<>(AttributeContext.class).compose(
                        new ChildContextLookup<>(RelyingPartyContext.class));
        attributeSourceIds = Collections.emptyList();
        setDefaultIdPNameQualifierLookupStrategy(new ResponderIdLookupFunction());
        setDefaultSPNameQualifierLookupStrategy(new RelyingPartyIdLookupFunction());
        useUnfilteredAttributes = true;
    }

    /**
     * Set the lookup strategy to use to locate the {@link SubjectContext}.
     * 
     * @param strategy lookup function to use
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Set the lookup strategy to use to locate the {@link AttributeContext}.
     * 
     * @param strategy lookup function to use
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
    }

    /**
     * Set the attribute sources to pull from.
     * 
     * @param ids attribute IDs to pull from
     */
    public void setAttributeSourceIds(@Nonnull @NonnullElements final List<String> ids) {
        checkSetterPreconditions();
        attributeSourceIds = List.copyOf(Constraint.isNotNull(ids, "Attribute ID collection cannot be null"));
    }

    /**
     * Set a {@link PairwiseIdStore} to use.
     * 
     * @param store the id store
     */
    public void setPersistentIdStore(@Nullable final PairwiseIdStore store) {
        checkSetterPreconditions();
        pidStore = store;
    }

    /**
     * Set a data source to inject into an auto-provisioned instance of {@link JDBCPairwiseIdStore}
     * to use as the store.
     * 
     * @param source data source
     */
    public void setDataSource(@Nullable final DataSource source) {
        checkSetterPreconditions();
        dataSource = source;
    }
    
    /**
     * Set whether to source the input attributes from the unfiltered set.
     * 
     * <p>Defaults to true, since the input is not directly exposed.</p>
     * 
     * @param flag flag to set
     */
    public void setUseUnfilteredAttributes(final boolean flag) {
        useUnfilteredAttributes = flag;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (attributeSourceIds.isEmpty()) {
            throw new ComponentInitializationException("Attribute source ID list cannot be empty");
        }

        if (null == pidStore) {
            if (dataSource != null) {
                log.debug("Creating JDBCPersistentStoreEx instance around supplied DataSource");
                final JDBCPairwiseIdStore newStore = new JDBCPairwiseIdStore();
                newStore.setDataSource(dataSource);
                newStore.initialize();
                pidStore = newStore;
            }
            
            if (null == pidStore) {
                throw new ComponentInitializationException("PairwiseIdStore cannot be null");
            }
        }
    }

    // Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override @Nullable protected String getIdentifier(@Nonnull final ProfileRequestContext profileRequestContext)
            throws SAMLException {

        Function<ProfileRequestContext,String> lookup = getDefaultIdPNameQualifierLookupStrategy();
        final String responderId = lookup != null ? lookup.apply(profileRequestContext) : null;
        if (responderId == null) {
            log.debug("No responder identifier, can't generate persistent ID");
            return null;
        }

        // Effective qualifier may override default in the case of an Affiliation.
        String relyingPartyId = getEffectiveSPNameQualifier(profileRequestContext);
        if (relyingPartyId == null) {
            lookup = getDefaultSPNameQualifierLookupStrategy();
            relyingPartyId = lookup != null ? lookup.apply(profileRequestContext) : null;
        }
        if (relyingPartyId == null) {
            log.debug("No relying party identifier, can't generate persistent ID");
            return null;
        }

        final SubjectContext subjectCtx = subjectContextLookupStrategy.apply(profileRequestContext);
        if (subjectCtx == null || subjectCtx.getPrincipalName() == null) {
            log.debug("No principal name, can't generate persistent ID");
            return null;
        }

        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeCtx == null) {
            log.debug("No attribute context, can't generate persistent ID");
            return null;
        }

        final Map<String,IdPAttribute> attributes = useUnfilteredAttributes ? attributeCtx.getUnfilteredIdPAttributes()
                : attributeCtx.getIdPAttributes();

        for (final String sourceId : attributeSourceIds) {
            log.debug("Checking for source attribute {}", sourceId);

            final IdPAttribute attribute = attributes.get(sourceId);
            if (attribute == null) {
                continue;
            }

            PairwiseId pid = new PairwiseId();
            pid.setIssuerEntityID(responderId);
            pid.setRecipientEntityID(relyingPartyId);
            pid.setPrincipalName(subjectCtx.getPrincipalName());
            
            final List<IdPAttributeValue> values = attribute.getValues();
            for (final IdPAttributeValue value : values) {
                try {
                    if (value instanceof ScopedStringAttributeValue) {
                        log.debug("Generating persistent NameID from Scoped String-valued attribute {}", sourceId);
                        pid.setSourceSystemId(((ScopedStringAttributeValue) value).getValue() + '@'
                                + ((ScopedStringAttributeValue) value).getScope());
                        pid = pidStore.getBySourceValue(pid, true);
                        return pid.getPairwiseId();
                    } else if (value instanceof StringAttributeValue) {
                        // Check for all whitespace, but don't trim the value used.
                        if (StringSupport.trimOrNull(((StringAttributeValue) value).getValue()) == null) {
                            log.debug("Skipping all-whitespace string value");
                            continue;
                        }
                        log.debug("Generating persistent NameID from String-valued attribute {}", sourceId);
                        pid.setSourceSystemId(((StringAttributeValue) value).getValue());
                        pid = pidStore.getBySourceValue(pid, true);
                        return pid.getPairwiseId();
                    } else {
                        log.info("Unrecognized attribute value type: {}", value.getClass().getName());
                    }
                } catch (final IOException e) {
                    throw new SAMLException(e);
                }
            }
        }

        log.info("Attribute sources {} did not produce a usable source identifier", attributeSourceIds);
        return null;
    }
    // Checkstyle: CyclomaticComplexity|MethodLength ON

}
