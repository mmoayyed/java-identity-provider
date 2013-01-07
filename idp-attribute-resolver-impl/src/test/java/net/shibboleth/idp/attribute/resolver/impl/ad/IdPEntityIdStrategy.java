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

import org.opensaml.messaging.context.BasicMessageMetadataContext;

import com.google.common.base.Function;

class IdPEntityIdStrategy implements Function<AttributeResolutionContext, String> {

    private BasicMessageMetadataContext context;
    static final String IDP_ENTITY_ID = "https://idp.example.org/idp";
    
    public IdPEntityIdStrategy() {
        context = new BasicMessageMetadataContext();
        context.setMessageId(IdPEntityIdStrategy.IDP_ENTITY_ID);
    }
    
    /** {@inheritDoc} */
    @Nullable public String  apply(@Nullable AttributeResolutionContext arg0) {
        return IdPEntityIdStrategy.IDP_ENTITY_ID;
    }
}