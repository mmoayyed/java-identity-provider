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

package net.shibboleth.idp.attribute.mapper.impl;

import net.shibboleth.idp.attribute.mapper.AbstractSAMLAttributesMapper;
import net.shibboleth.idp.attribute.mapper.RequestedAttribute;

/**
 * This class conceptually represents the content of a attribute-map file, hence it describes (and then does) the
 * mappings from a {@link java.util.List} of SAML2 {@link org.opensaml.saml.saml2.metadata.RequestedAttribute} into a
 * {@link com.google.common.collect.Multimap} going from (SAML2) attributeId to idp {@link RequestedAttribute}s (or null
 * if the mapping failed for type reasons).
 * 
 */
public class RequestedAttributesMapper extends
        AbstractSAMLAttributesMapper<org.opensaml.saml.saml2.metadata.RequestedAttribute, RequestedAttribute> {

}