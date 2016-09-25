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

package net.shibboleth.idp.profile.support;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.webflow.core.FlowException;
import org.springframework.webflow.mvc.servlet.FlowHandlerAdapter;

/**
 * Extension of SWF's built-in {@link FlowHandlerAdapter} implementation that overrides its
 * poor assumption that a missing flow exception should result in the flow being restarted.
 */
public class RethrowingFlowHandlerAdapter extends FlowHandlerAdapter {

    /** {@inheritDoc} */
    @Override
    protected void defaultHandleException(final String flowId, final FlowException e, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        
        throw e;
    }

}