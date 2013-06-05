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

package net.shibboleth.idp.attribute.filter.impl.saml;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.testng.annotations.BeforeClass;

/**
 * tests for {@link AttributeRequesterEntityAttributeExactMatcher}.
 */
public class BaseMetadataTests extends XMLObjectBaseTestCase {

    private EntitiesDescriptor metadata;

    static private final String IDP_ENTITY_ID = "https://idp.shibboleth.net/idp/shibboleth";

    static private final String JIRA_ENTITY_ID = "https://issues.shibboleth.net/shibboleth";

    static private final String WIKI_ENTITY_ID = "https://wiki.shibboleth.net/shibboleth";

    static private final String NONE_ENTITY_ID = "https://none.shibboleth.net/shibboleth";

    protected EntityDescriptor idpEntity;

    protected EntityDescriptor jiraEntity;

    protected EntityDescriptor wikiEntity;

    protected EntityDescriptor noneEntity;

    @BeforeClass(dependsOnMethods = "initXMLObjectSupport") public void setUp() {
        metadata = unmarshallElement("/data/net/shibboleth/idp/filter/impl/saml/shibboleth.net-metadata.xml");

        for (EntityDescriptor entity : metadata.getEntityDescriptors()) {
            if (IDP_ENTITY_ID.equals(entity.getEntityID())) {
                idpEntity = entity;
            } else if (JIRA_ENTITY_ID.equals(entity.getEntityID())) {
                jiraEntity = entity;
            } else if (WIKI_ENTITY_ID.equals(entity.getEntityID())) {
                wikiEntity = entity;
            } else if (NONE_ENTITY_ID.equals(entity.getEntityID())) {
                noneEntity = entity;
            }
        }
    }

    static protected AttributeFilterContext
            metadataContext(EntityDescriptor idp, EntityDescriptor sp, String principal) {
        BaseContext parent = new BaseContext() {};

        AttributeRecipientContext recipientContext = new AttributeRecipientContext();
        AttributeResolutionContext resolutionContext = new AttributeResolutionContext();

        recipientContext.setPrincipal(principal);
        if (null != idp) {
            recipientContext.setAttributeIssuerID(idp.getEntityID());
        }
        recipientContext.setAttributeIssuerMetadata(idp);
        if (null != sp) {
            recipientContext.setAttributeRecipientID(sp.getEntityID());
        }
        recipientContext.setAttributeRecipientMetadata(sp);
        resolutionContext.addSubcontext(recipientContext);

        parent.addSubcontext(resolutionContext);
        return parent.getSubcontext(AttributeFilterContext.class, true);
    }
}
