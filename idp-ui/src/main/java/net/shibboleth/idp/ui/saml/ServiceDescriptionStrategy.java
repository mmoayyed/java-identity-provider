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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Code to get a {@link Map} from language to Description.
 */
public class ServiceDescriptionStrategy extends AbstractEntityStrategy  {

    /** logger. */
    private final Logger log = LoggerFactory.getLogger(ServiceDescriptionStrategy.class);

    /** {@inheritDoc} */
    @Override @Nonnull public Map<String, String> apply(@Nullable final EntityDescriptor input) {
        final UIInfo uiInfo = getSPUIInfo(input);
        if (uiInfo == null || uiInfo.getDescriptions() == null) {
            log.trace("No UIInfo or no Descriptions therein");
            return Collections.EMPTY_MAP;
        }

        final Map<String, String> result = new HashMap<>(uiInfo.getDescriptions().size());

        for (final Description desc : uiInfo.getDescriptions()) {
            log.trace("Found description {} in UIInfo, language={} ", desc.getValue(), desc.getXMLLang());
            result.put(desc.getXMLLang(), desc.getValue());
        }
        return result;
    }
}
