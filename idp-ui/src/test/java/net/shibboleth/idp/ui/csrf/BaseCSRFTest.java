/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.ui.csrf;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.springframework.core.style.ToStringCreator;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.ViewState;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.View;

/**
 * Basis for CSRF tests.
 */
public class BaseCSRFTest {
    
    /** The name of the field that holds the set of included viewstates.*/
    protected static final String INCLUDED_VIEWSTATES_FIELDNAME = "includedViewStateIds";
    
    /** The name of the field that holds the set of excluded viewstates.*/
    protected static final String EXCLUDED_VIEWSTATES_FIELDNAME = "excludedViewStateIds";
    
    /** The name of the field that says whether all view states are included.*/
    protected static final String INCLUDE_ALL_VIEWSTATES_FIELDNAME = "includeAllViewStates";
    
    /**
     * Simple mock view-state with a no-op viewFactory.
     */
    protected class MockViewState extends ViewState {

        public MockViewState(@Nonnull
        final String flowID, @Nonnull
        final String viewID) {
            super(new Flow(flowID), viewID, viewFactory -> {
                throw new UnsupportedOperationException();
            });
        }
    }
    
    /**
     * MockView, for use when you do not actually need a rendered response.
     */
    protected static class MockView implements View {

        /**
         * The id of the view that would have been rendered.
         */
        private String viewId;

        private RequestContext context;

        public MockView(String id, RequestContext ctx) {
            viewId = id;
            context = ctx;
        }

        /**
         * Returns the id of the view that would have been rendered.
         * 
         * @return the view id
         */
        public String getViewId() {
            return viewId;
        }

        public void render() throws IOException {
            context.getExternalContext().getResponseWriter().write(viewId);
        }

        public boolean userEventQueued() {
            return hasFlowEvent();
        }

        public void processUserEvent() {
            
        }

        public Serializable getUserEventState() {
            return null;
        }

        public boolean hasFlowEvent() {
            return context.getRequestParameters().contains("_eventId");
        }

        public Event getFlowEvent() {
            return new Event(this, context.getRequestParameters().get("_eventId"));
        }

        public void saveState() {

        }

        public String toString() {
            return new ToStringCreator(this).append("viewId", viewId).toString();
        }
    }

}
