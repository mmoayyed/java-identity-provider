/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.saml2.profile.config.impl;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.logic.NoConfidentialityMessageChannelPredicate;
import org.opensaml.profile.logic.NoIntegrityMessageChannelPredicate;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;

/** Configuration support for IdP SAML 2.0 attribute query profile. */
public class AttributeQueryProfileConfiguration extends AbstractSAML2AssertionProducingProfileConfiguration
        implements net.shibboleth.idp.saml.saml2.profile.config.AttributeQueryProfileConfiguration {
    
    /** Name of profile counter. */
    @Nonnull @NotEmpty public static final String PROFILE_COUNTER = "net.shibboleth.idp.profiles.sam2.query.attribute";

    /** Whether the FriendlyName attribute should be randomized when encoding Attributes. */
    @Nonnull private Predicate<ProfileRequestContext> randomizeFriendlyNamePredicate;

    /** Constructor. */
    public AttributeQueryProfileConfiguration() {
        this(PROFILE_ID);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected AttributeQueryProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        setSignResponsesPredicate(new NoIntegrityMessageChannelPredicate());
        setEncryptAssertionsPredicate(new NoConfidentialityMessageChannelPredicate());
        randomizeFriendlyNamePredicate = PredicateSupport.alwaysFalse();
    }

    /** {@inheritDoc} */
    public boolean isRandomizeFriendlyName(@Nullable final ProfileRequestContext profileRequestContext) {
        return randomizeFriendlyNamePredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether to randomize/perturb the FriendlyName attribute when encoding SAML 2.0 Attributes to
     * enable probing of invalid behavior by relying parties.
     * 
     * @param flag flag to set
     * 
     * @since 5.1.0
     */
    public void setRandomizeFriendlyName(final boolean flag) {
        randomizeFriendlyNamePredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set condition to determine whether to randomize/perturb the FriendlyName attribute when encoding
     * SAML 2.0 Attributes to enable probing of invalid behavior by relying parties.
     * 
     * @param condition condition to set
     * 
     * @since 5.1.0
     */
    public void setRandomizeFriendlyNamePredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        randomizeFriendlyNamePredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }
    
}