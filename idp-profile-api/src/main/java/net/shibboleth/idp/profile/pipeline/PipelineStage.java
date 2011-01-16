/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.profile.pipeline;

import net.shibboleth.idp.profile.ProfileContext;

/** A processing stage in a profile conversation. */
public interface PipelineStage<InboundMessageType, OutboundMessageType> {

    /**
     * Gets the ID of the stage.
     * 
     * @return the ID of the stage, never null
     */
    public String getId();

    /**
     * Processes the incoming request according to the rules of this stage.
     * 
     * @param profileContext current profile context
     */
    public void processRequest(ProfileContext<InboundMessageType, OutboundMessageType> profileContext);
}