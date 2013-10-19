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

package net.shibboleth.idp.saml.nameid;

import java.util.Collections;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSamlNameIdentifierEncoder;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Criteria: Attributes SAML protocol or Encoder Class Supported formats - from metadata Format precedence - from
 * profile config.
 * 
 * Needs helpers getting supported formats
 */
//TODO
public class NameIdentifierAttributeResolver implements
        Resolver<Pair<IdPAttribute, AbstractSamlNameIdentifierEncoder>, CriteriaSet> {

    /** {@inheritDoc} */
    public Iterable<Pair<IdPAttribute, AbstractSamlNameIdentifierEncoder>> resolve(CriteriaSet criteria)
            throws ResolverException {
        return Collections.singleton(resolveSingle(criteria));
    }

    /** {@inheritDoc} */
    public Pair<IdPAttribute, AbstractSamlNameIdentifierEncoder> resolveSingle(CriteriaSet criteria)
            throws ResolverException {
        // TODO Auto-generated method stub
        return null;
    }
}