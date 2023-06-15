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

package net.shibboleth.idp.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A base class for subject canonicalization actions.
 * 
 * In addition to the work performed by {@link AbstractProfileAction}, this action also looks up and makes available the
 * {@link SubjectCanonicalizationContext}.
 * 
 * Authentication action implementations should override
 * {@link #doExecute(ProfileRequestContext, SubjectCanonicalizationContext)}
 * 
 * @event {@link AuthnEventIds#INVALID_SUBJECT_C14N_CTX}
 */
public abstract class AbstractSubjectCanonicalizationAction
        extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSubjectCanonicalizationAction.class);
    
    /**
     * Strategy used to find the {@link SubjectCanonicalizationContext} from the
     * {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,SubjectCanonicalizationContext> scCtxLookupStrategy;
    
    /** {@link SubjectCanonicalizationContext} to operate on. */
    @NonnullBeforeExec private SubjectCanonicalizationContext scContext;

    /** Match patterns and replacement strings to apply. */
    @Nonnull private List<Pair<Pattern,String>> transforms;

    /** Convert to uppercase prior to transforms? */
    private boolean uppercase;
    
    /** Convert to lowercase prior to transforms? */
    private boolean lowercase;
    
    /** Trim prior to transforms? */
    private boolean trim;
    
    /** Constructor. */
    public AbstractSubjectCanonicalizationAction() {
        scCtxLookupStrategy = new ChildContextLookup<>(SubjectCanonicalizationContext.class, false);
        transforms = CollectionSupport.emptyList();

        uppercase = false;
        lowercase = false;
        trim = false;
    }

    /** Null safe getter.
     * @return Returns the scContext.
     */
    @SuppressWarnings("null")
    @Nonnull private SubjectCanonicalizationContext getSubjectCanonicalizationContext() {
        assert isPreExecuteCalled();
        return scContext;
    }

    /**
     * Set the context lookup strategy.
     * 
     * @param strategy  lookup strategy function for {@link SubjectCanonicalizationContext}.
     */
    public void setLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectCanonicalizationContext> strategy) {
        checkSetterPreconditions();
        scCtxLookupStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");
    }

    /**
     * A collection of regular expression and replacement pairs.
     * 
     * @param newTransforms collection of replacement transforms
     */
    public void setTransforms(@Nullable final Collection<Pair<String, String>> newTransforms) {
        checkSetterPreconditions();
        if (newTransforms != null) {
            transforms = new ArrayList<>();
            for (final Pair<String,String> p : newTransforms) {
                final Pattern pattern = Pattern.compile(StringSupport.trimOrNull(p.getFirst()));
                transforms.add(new Pair<>(pattern, Constraint.isNotNull(
                        StringSupport.trimOrNull(p.getSecond()), "Replacement expression cannot be null")));
            }
        } else {
            transforms = CollectionSupport.emptyList();
        }
    }

    /**
     * Controls conversion to uppercase prior to applying any transforms.
     * 
     * @param flag  uppercase flag
     */
    public void setUppercase(final boolean flag) {
        checkSetterPreconditions();
        uppercase = flag;
    }

    /**
     * Controls conversion to lowercase prior to applying any transforms.
     * 
     * @param flag lowercase flag
     */
    public void setLowercase(final boolean flag) {
        checkSetterPreconditions();
        lowercase = flag;
    }
    
    /**
     * Controls whitespace trimming prior to applying any transforms.
     * 
     * @param flag trim flag
     */
    public void setTrim(final boolean flag) {
        checkSetterPreconditions();
        trim = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (super.doPreExecute(profileRequestContext)) {
            final SubjectCanonicalizationContext sc = scContext = scCtxLookupStrategy.apply(profileRequestContext);
            if (sc == null) {
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
                return false;
            }
            
            return doPreExecute(profileRequestContext, sc);
        }
        
        return false;
    }

    /**
     * Performs this c14n action's pre-execute step. Default implementation just returns true iff a subject
     * is set.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param c14nContext the current subject canonicalization context
     * 
     * @return true iff execution should continue
     */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
        
        if (c14nContext.getSubject() == null) {
            c14nContext.setException(new SubjectCanonicalizationException("No Subject found in context"));
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected final void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        doExecute(profileRequestContext, getSubjectCanonicalizationContext());
    }

    /**
     * Performs this authentication action. Default implementation throws an exception.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param c14nContext the current subject canonicalization context
     */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {
    }
    
    /**
     * Apply any configured regular expression replacements to an input value and return the result.
     * 
     * @param input the input string
     * 
     * @return  the result of applying the expressions
     */
    @Nonnull @NotEmpty protected String applyTransforms(@Nonnull @NotEmpty final String input) {
        
        String s = input;
        
        if (trim) {
            log.debug("{} trimming whitespace of input string '{}'", getLogPrefix(), s);
            s = s.trim();
        }
        
        if (lowercase) {
            log.debug("{} converting input string '{}' to lowercase", getLogPrefix(), s);
            s = s.toLowerCase();
        } else if (uppercase) {
            log.debug("{} converting input string '{}' to uppercase", getLogPrefix(), s);
            s = s.toUpperCase();
        }

        for (final Pair<Pattern,String> p : transforms) {
            final Pattern pattern = p.getFirst();
            if (pattern != null) {
                final Matcher m = pattern.matcher(s);
                log.debug("{} applying replacement expression '{}' against input '{}'", getLogPrefix(),
                        pattern.pattern(), s);
                s = m.replaceAll(p.getSecond());
                log.debug("{} result of replacement is '{}'", getLogPrefix(), s);
            }
        }
        
        assert s != null;
        return s;
    }
    
}