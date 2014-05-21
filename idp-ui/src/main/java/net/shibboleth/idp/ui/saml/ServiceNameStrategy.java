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

package net.shibboleth.idp.ui.saml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.collection.LazyMap;

import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.ServiceName;

/**
 * Get the map for the service name.<br/>
 * We populate from the MDUI (if it exists) first, then the ACS, but we do not replace anything we set from MDUI.
 * Finally we populate the default entityId from the entity.
 * 
 */
public class ServiceNameStrategy extends AbstractEntityStrategy {

    /**
     * If the entityId can look like a host return that, otherwise the entityId in full.
     * @param entity the entity,
     * @return either the host or the entityId.
     */
    @Nullable private String getNameFromEntityId(@Nonnull EntityDescriptor entity) {

        try {
            final URI entityId = new URI(entity.getEntityID());
            final String scheme = entityId.getScheme();

            if ("http".equals(scheme) || "https".equals(scheme)) {
                return entityId.getHost();
            }
        } catch (URISyntaxException e) {
            //
            // It wasn't an URI. return full entityId.
            //
            return entity.getEntityID();
        }
        //
        // not a URL return full entityID
        //
        return entity.getEntityID();
    }

    
    /** {@inheritDoc} */
    @Override @Nonnull public Map<String, String> apply(@Nullable EntityDescriptor input) {

        if (input == null) {
            return Collections.EMPTY_MAP;
        }

        final Map<String, String> result = new LazyMap<>();
        
        // UI DisplayNames
        final UIInfo uiInfo = getSPUIInfo(input);
        if (uiInfo != null && null != uiInfo.getDisplayNames()) {
            for (final DisplayName name : uiInfo.getDisplayNames()) {
                result.put(name.getXMLLang(), name.getValue());
            }
        }
        
        // ACS
        AttributeConsumingService acs = null;
        final List<RoleDescriptor> roles = input.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        if (!roles.isEmpty()) {
            SPSSODescriptor spssod = (SPSSODescriptor) roles.get(0);
            acs = spssod.getDefaultAttributeConsumingService();
        }
        if (acs != null) {
            for (ServiceName name : acs.getNames()) {
                if (result.containsKey(name.getXMLLang())) {
                    result.put(name.getXMLLang(), name.getValue());
                }
            }
        }
        // Default from name
        result.put(null, getNameFromEntityId(input));
        
        return result;
 
    }

}
