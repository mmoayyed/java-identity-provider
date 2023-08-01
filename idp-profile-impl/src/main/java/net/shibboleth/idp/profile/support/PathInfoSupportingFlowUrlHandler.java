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

package net.shibboleth.idp.profile.support;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.springframework.webflow.context.servlet.DefaultFlowUrlHandler;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.StringSupport;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extension of standard SWF URL handler that checks for requests in which a valid flow ID
 * is a prefix of the PATH_INFO value, allowing the flow to run with the rest of the path
 * available to it as input.
 * 
 * <p>A deliberate choice is made to do this only for enumerated flows, and not by checking
 * every flow in the registry. This would probably not be harmful, but it's less risky to
 * exclude the SAML/CAS/etc. flows unless we deliberately want to allow them to get additional
 * path info.</p>
 */
public class PathInfoSupportingFlowUrlHandler extends DefaultFlowUrlHandler {

    /** Flows to support. */
    @Nonnull private Collection<String> supportedFlowIds;
    
    /** Constructor. */
    public PathInfoSupportingFlowUrlHandler() {
        supportedFlowIds = CollectionSupport.emptyList();
    }

    /**
     * Set an ordered collection of flow IDs to detect.
     * 
     * <p>Flow IDs that overlap <strong>must</strong> be ordered with the greater
     * number of path segments first (e.g., flow/thing/one should precede flow/thing).</p>
     * 
     * @param flowIds the flow definition registry
     */
    public void setSupportedFlows(@Nonnull final Collection<String> flowIds) {
        supportedFlowIds = StringSupport.normalizeStringCollection(flowIds);
    }
    
    /** {@inheritDoc} */
    @Override
    public String getFlowId(final HttpServletRequest request) {
        
        final String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            for (final String id : supportedFlowIds) {
                if (pathInfo.startsWith(id, 1)) {
                    if (pathInfo.length() > id.length() + 1) {
                        if (pathInfo.charAt(id.length() + 1) == '/') {
                            return id;
                        }
                    }
                }
            }
        }
        
        return super.getFlowId(request);
    }

}