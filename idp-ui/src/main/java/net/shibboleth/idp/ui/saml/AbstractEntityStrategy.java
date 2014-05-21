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

import java.util.Map;

import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.common.Extensions;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import com.google.common.base.Function;

/**
 * Base class for functions which navigate the entityDescriptor to look for the MDUI.
 */
public abstract class AbstractEntityStrategy implements Function<EntityDescriptor, Map<String, String>> {

    /**
     * Traverse the SP's EntityDescriptor and pick out the {@link UIInfo}.
     * 
     * @param spEntity the entityDescriptor
     * @return the first UIInfo for the SP.
     */
    @Nullable protected UIInfo getSPUIInfo(@Nullable final EntityDescriptor spEntity) {
        if (null == spEntity) {
            //
            // all done
            //
            return null;
        }

        for (final RoleDescriptor role : spEntity.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME)) {
            final Extensions exts = role.getExtensions();
            if (exts != null) {
                for (XMLObject object : exts.getOrderedChildren()) {
                    if (object instanceof UIInfo) {
                        return (UIInfo) object;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Traverse the SP's EntityDescriptor and pick out the {@link Organization}.
     * 
     * @param spEntity the entityDescriptor
     * @return the {@link Organization} for the SP
     */
    @Nullable protected Organization getSPOrganization(@Nullable final EntityDescriptor spEntity) {
        if (null == spEntity) {
            //
            // all done
            //
            return null;
        }

        for (final RoleDescriptor role : spEntity.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME)) {
            if (role.getOrganization() != null) {
                return role.getOrganization();
            }
        }

        return spEntity.getOrganization();
    }
}
