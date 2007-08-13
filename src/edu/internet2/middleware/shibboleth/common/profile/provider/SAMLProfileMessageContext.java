/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.profile.provider;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;

import edu.internet2.middleware.shibboleth.common.profile.ProfileMessageContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;

/**
 * Marker interface that combines SAML and profile message contexts. *
 * 
 * @param <InboundMessageType> type of inbound SAML message
 * @param <OutboundMessageType> type of outbound SAML message
 * @param <NameIdentifierType> type of name identifier used for subjects
 * @param <ProfileConfigurationType> profile configuration type for current request
 */
public interface SAMLProfileMessageContext<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject, ProfileConfigurationType extends ProfileConfiguration>
        extends SAMLMessageContext<InboundMessageType, OutboundMessageType, NameIdentifierType>,
        ProfileMessageContext<ProfileConfigurationType> {

}
