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

package net.shibboleth.idp.profile;

import org.springframework.webflow.conversation.Conversation;
import org.springframework.webflow.conversation.ConversationId;
import org.springframework.webflow.conversation.ConversationManager;
import org.springframework.webflow.conversation.ConversationParameters;

/**  A {@link ConversationManager} that stores conversation state in the Infinispan instance used by the IdP. */
public class InfinispanBindingConversationManager implements ConversationManager {

    /** {@inheritDoc} */
    public Conversation beginConversation(final ConversationParameters conversationParameters) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Conversation getConversation(final ConversationId id) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public ConversationId parseConversationId(final String encodedId) {
        // TODO Auto-generated method stub
        return null;
    }
}