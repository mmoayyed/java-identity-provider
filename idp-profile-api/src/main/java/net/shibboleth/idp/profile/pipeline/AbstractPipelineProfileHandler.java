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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.shibboleth.idp.profile.AbstractConversationalProfileHandler;
import net.shibboleth.idp.profile.ProfileContext;

import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.messaging.context.impl.BaseSubcontext;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;


/**
 * A profile handler that handles a request by means of passing the request through a series of stages that perform a
 * particular function (e.g. decodes the message, validates its trustworthiness, etc.).
 * 
 * Note, as this is a subclass of {@link AbstractConversationalProfileHandler} any stage may pause the conversation. It
 * is the responsibility of the stage to save any necessary state to resume work where it left off. When the profile
 * conversation is resumed the {@link PipelineStage#processRequest(ProfileContext)} method of the stage that paused the
 * conversation will be executed again. After that stage completes the next registered stage will be run and so on until
 * the pipeline completes or another stage pauses the conversation.
 */
public abstract class AbstractPipelineProfileHandler<InboundMessageType, OutboundMessageType> extends
        AbstractConversationalProfileHandler<InboundMessageType, OutboundMessageType> {

    /** Ordered list of stages that each request goes through for processing. */
    private final List<PipelineStage<InboundMessageType, OutboundMessageType>> profileStages;

    /**
     * Constructor.
     * 
     * @param path the path that leads to this profile, never null or empty
     * @param stages the stages for this profile. This list is defensively copied and checked for null values. The list
     *            may not be null or empty after being copied.
     */
    public AbstractPipelineProfileHandler(String path,
            List<PipelineStage<InboundMessageType, OutboundMessageType>> stages) {
        super(path);

        Assert.isNotNull(stages, "Pipeline profile handler stage list may not be null");
        ArrayList<PipelineStage<InboundMessageType, OutboundMessageType>> santaizedStages = new ArrayList<PipelineStage<InboundMessageType, OutboundMessageType>>();
        for (PipelineStage<InboundMessageType, OutboundMessageType> stage : stages) {
            if (stage != null) {
                santaizedStages.add(stage);
            }
        }
        Assert.isNotEmpty(santaizedStages, "Pipeline profile handler stage list must contain at least one stage");
        profileStages = Collections.unmodifiableList(santaizedStages);
    }

    /** {@inheritDoc} */
    protected void initiateConversation(final ProfileContext<InboundMessageType, OutboundMessageType> profileContext) {
        executeProfileStages(profileContext, profileStages);
    }

    /** {@inheritDoc} */
    protected void resumeConversation(final ProfileContext<InboundMessageType, OutboundMessageType> profileContext) {
        PipelineContext pipelineContext = profileContext.getSubcontext(PipelineContext.class);
        // TODO deal with case where there is no pipeline context

        int stageIndex = 0;
        for (; stageIndex < profileStages.size(); stageIndex++) {
            if (profileStages.get(stageIndex).getId().equals(pipelineContext.getCurrentStage())) {
                break;
            }
        }

        // TODO deal with case where stage may no longer exist if stages changed since conversation were paused

        List<PipelineStage<InboundMessageType, OutboundMessageType>> remainingStages = profileStages.subList(
                stageIndex, profileStages.size() - 1);
        executeProfileStages(profileContext, remainingStages);
    }

    /**
     * Executes the given list of profile stages.
     * 
     * @param profileContext current profile context
     * @param stages stages to be executed
     */
    protected void executeProfileStages(final ProfileContext<InboundMessageType, OutboundMessageType> profileContext,
            List<PipelineStage<InboundMessageType, OutboundMessageType>> stages) {
        PipelineContext pipelineContext = profileContext.getSubcontext(PipelineContext.class, true);

        for (PipelineStage<InboundMessageType, OutboundMessageType> stage : profileStages) {
            try {
                pipelineContext.setCurrentStage(stage.getId());
                stage.processRequest(profileContext);
            } catch (Throwable t) {
                // TODO catch and handle any errors
            }
        }
    }

    /** Subcontext containing state about current pipeline process. */
    public final static class PipelineContext extends BaseSubcontext {

        /** The current stage being executed in the profile handler. */
        private String currentStageId;

        /**
         * Constructor.
         * 
         * @param owner the {@link ProfileContext} that owns this subcontext
         */
        public PipelineContext(SubcontextContainer owner) {
            super(owner);
        }

        /**
         * Gets the ID of the current stage being processed.
         * 
         * @return ID of the current stage being processed
         */
        public String getCurrentStage() {
            return currentStageId;
        }

        /**
         * Sets the ID of the current stage being processed.
         * 
         * @param stageId ID of the current stage being processed
         */
        public void setCurrentStage(String stageId) {
            currentStageId = StringSupport.trimOrNull(stageId);
        }
    }
}