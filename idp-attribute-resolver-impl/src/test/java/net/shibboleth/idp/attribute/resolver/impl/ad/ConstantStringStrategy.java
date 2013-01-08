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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;

import com.google.common.base.Function;

class ConstantStringStrategy implements Function<AttributeResolutionContext, String> {

    static final String IDP_ENTITY_ID = "https://idp.example.org/idp";
    
    static final String PRINCIPAL_ID = "PETER_THE_PRINCIPAL";

    static final String SP_ENTITY_ID = "https://sp.example.org/idp";
    
    final String returnValue;

    @SuppressWarnings("unused")
    private ConstantStringStrategy() {
        returnValue = "notSet";
    }
    
    public ConstantStringStrategy(String value) {
        returnValue = value;
    } 
    
    /** {@inheritDoc} */
    @Nullable public String  apply(@Nullable AttributeResolutionContext arg0) {
        return returnValue;
    }
}