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

package net.shibboleth.idp.attribute.filter.spring.policyrule.impl;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.policyrule.filtercontext.impl.ProxiedRequesterRegexpPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;

/**
 * Bean definition parser for {@link ProxiedRequesterRegexpPolicyRule}.
 */
public class ProxiedRequesterRegexRuleParser extends AbstractRegexPolicyRuleParser {

    /** Schema type. */
    @Nonnull public static final QName SCHEMA_TYPE_AFP = new QName(BaseFilterParser.NAMESPACE, "ProxiedRequesterRegex");

    /** {@inheritDoc} */
    @Override protected QName getAFPName() {
        return SCHEMA_TYPE_AFP;
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected Class<ProxiedRequesterRegexpPolicyRule> getNativeBeanClass() {
        return ProxiedRequesterRegexpPolicyRule.class;
    }
    
}