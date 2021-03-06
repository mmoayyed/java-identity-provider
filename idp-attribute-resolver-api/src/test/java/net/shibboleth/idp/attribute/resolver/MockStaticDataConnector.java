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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

/** An attribute definition that simply returns a static value.   Used for testing only.  This is 
 * a cut and paste job from StaticDataConnector in idp-attribute-resolver-impl */
@ThreadSafe
public class MockStaticDataConnector extends AbstractDataConnector {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(MockStaticDataConnector.class);

    /** Static collection of values returned by this connector. */
    private Map<String, IdPAttribute> attributes;

    /**
     * Get the static values returned by this connector.
     * 
     * @return static values returned by this connector
     */
    @Nonnull public Map<String, IdPAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Set static values returned by this connector.
     * 
     * @param newValues static values returned by this connector
     */
    public void setValues(@Nullable Collection<IdPAttribute> newValues) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (null == newValues) {
            attributes = null;
            return;
        } 
        
        Map<String, IdPAttribute> map = new HashMap<>(newValues.size());
        for (IdPAttribute attr:newValues) {
            if (null == attr) {
                continue;
            }
            map.put(attr.getId(), attr);
        }
        
        attributes = Map.copyOf(map);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        log.debug("Data connector '{}': Resolving static attribute {}", getId(), attributes);
        return attributes;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == attributes) {
            throw new ComponentInitializationException("Static Data connector " + getId()
                    + " does not have values set up.");
        }
    }
    
}