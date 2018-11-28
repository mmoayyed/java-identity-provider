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

package net.shibboleth.idp.attribute.filter.spring.impl;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;

/**
 * A special case version of {@link BaseFilterParser} which warns if the legacy name is used.
 */
public abstract class AbstractWarningFilterParser extends BaseFilterParser {

    /**
     * Helper function to assist rewrite from old to new QName.
     * 
     * @return the "new" type
     */
    protected abstract QName getAFPName();

}