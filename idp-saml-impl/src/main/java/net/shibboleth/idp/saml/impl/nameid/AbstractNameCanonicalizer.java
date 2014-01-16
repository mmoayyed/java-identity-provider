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

package net.shibboleth.idp.saml.impl.nameid;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Base for all NameCanonicalizers.
 */
public abstract class AbstractNameCanonicalizer extends AbstractIdentifiableInitializableComponent {

    /**
     * The service providers we support. Null or an empty set means allow all.
     */
    @Nonnull private Collection<String> serviceProviders = Collections.EMPTY_SET;

    /**
     * The formats we support. Null or an empty set means allow all.
     */
    @Nonnull private Collection<String> formats = Collections.EMPTY_SET;

    /** The log prefix. */
    private String logPrefix;

    /**
     * Get the set of SPs we support.
     * 
     * @return Returns the serviceProviders.
     */
    @Nonnull public Collection<String> getServiceProviders() {
        return serviceProviders;
    }

    /**
     * Set the set of SPs we support.
     * 
     * @param providers The serviceProviders to set.
     */
    public void setServiceProviders(@Nonnull Collection<String> providers) {
        serviceProviders = Constraint.isNotNull(providers, "service providers should not be empty");
    }

    /**
     * Get the set of formats we support.
     * 
     * @return Returns the formats.
     */
    public Collection<String> getFormats() {
        return formats;
    }

    /**
     * Set the set of formats we support.
     * 
     * @param theFormats The formats to set.
     */
    public void setFormats(Collection<String> theFormats) {
        formats = Constraint.isNotNull(theFormats, "formats should not be empty");
    }

    /** {@inheritDoc} */
    @Override public void setId(String id) {
        super.setId(id);
    }

    /**
     * returns whether the given SP is applicable for this canonicalizer.
     * 
     * @param sp the sp
     * @return whether it is applicable
     */
    protected boolean isSpApplicable(@Nullable String sp) {
        Collection<String> sps = getServiceProviders();
        if (sps.isEmpty()) {
            return true;
        }
        if (null == sp) {
            return true;
        }
        return sps.contains(sp);
    }

    /**
     * returns whether the given format is applicable for this canonicalizer.
     * 
     * @param format the format to check.
     * @return whether it is applicable
     */
    protected boolean isFormatApplicable(String format) {
        Collection<String> fmts = getFormats();
        if (fmts.isEmpty()) {
            return true;
        }
        if (null == format) {
            return true;
        }
        return fmts.contains(format);
    }

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Name Canonicalizer '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Name Canonicalizer '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }
}
