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

package net.shibboleth.idp.ui.context;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * The data we use for rendering RelyingParty information.<br/>
 * This <em>prototype bean</em> will either be configured into (or, more likely be part of) Action
 * which sets up the context.
 * <br/>
 * At initialization time we set up all the {@link RelyingPartyUIData} fields which
 * were not configured.
 * 
 * @param <T> the type which this stores data for.
 */
@NotThreadSafe
public class RelyingPartyUIInformation<T> extends AbstractInitializableComponent {

    /** The ServiceDescription information. */
    @NonnullAfterInit private RelyingPartyUIData<T> serviceDescription;

    /** The Service Name information. */
    @NonnullAfterInit private RelyingPartyUIData<T> serviceName;

    
    /** The list of supported browser languages. */
    @Nonnull private List<String> browserLanguages = Collections.EMPTY_LIST;

    /**
     * Get the service description object.
     * 
     * @return the description
     */
    @NonnullAfterInit public RelyingPartyUIData<T> getServiceDescription() {
        return serviceDescription;
    }

    /**
     * Set the service description object.
     * 
     * @param description what to set
     */
    @NonnullAfterInit public void setServiceDescription(final RelyingPartyUIData<T> description) {
        serviceDescription = description;
    }

    /**
     * Get the service name object.
     * 
     * @return the object
     */
    @NonnullAfterInit public RelyingPartyUIData<T> getServiceName() {
        return serviceName;
    }

    /**
     * Set the service description object.
     * 
     * @param object what to set
     */
    @NonnullAfterInit public void setServiceName(final RelyingPartyUIData<T> object) {
        serviceName = object;
    }
 
    /**
     * Sets the list of languages supported by the browser.
     * 
     * @param langs the languages.
     */
    public void setBrowserLanguages(final List<String> langs) {
        browserLanguages = Constraint.isNotNull(langs, "List of languages cannot be null");
    }

    
    /** Set the relying party.  This calls into all the subsidiary {@link RelyingPartyUIData}
     * fields to set up the values from the relying party.
     * @param relyingParty what to set
     */
    public void setRelyingParty(T relyingParty) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        serviceDescription.setRelyingParty(relyingParty);
        serviceName.setRelyingParty(relyingParty);
    }

    /**
     * Lookup the provided map by language iterating over all languages, then the non name language (if present), then
     * the default.
     * 
     * @param lookup the map to consult
     * @return the value.
     */
    @Nullable protected String lookupMap(@Nonnull final Map<String, String> lookup) {
        for (String browserLanguage : browserLanguages) {
            // Lookup by language
            final String val = lookup.get(browserLanguage);
            if (null != val) {
                return val;
            }
        }
        // null language is the default.
        return  lookup.get(null);
    }

    /** {@inheritDoc} 
     * @throws ComponentInitializationException */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == serviceDescription) {
            serviceDescription = new RelyingPartyUIData<>();
        }
        if (null == serviceName) {
            serviceName = new RelyingPartyUIData<>();
        }
    }
    
    /** Populate the {@link RelyingPartyUIContext} with the values to display.
     * we consult the subsidiary {@link RelyingPartyUIData} elements and our
     * language. 
     * @param context what to populate
     */
    public void populateContext(@Nonnull final RelyingPartyUIContext context) {
        context.setServiceDescription(lookupMap(getServiceDescription().getDataMap()));
        context.setServiceName(lookupMap(getServiceName().getDataMap()));
    }

}
