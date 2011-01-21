/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.resolver;

/** Mock object implementation of {@link BaseResolverPlugin}. */
public class MockBaseResolverPlugin extends BaseResolverPlugin<String> {

    /** Static value return by {@link #doResolve(AttributeResolutionContext)}. */
    private String resolverValue;

    /**
     * Constructor.
     * 
     * @param id id of this plugin
     * @param value value returned by {@link #doResolve(AttributeResolutionContext)}
     */
    public MockBaseResolverPlugin(String id, String value) {
        super(id);
        resolverValue = value;
    }

    /** {@inheritDoc} */
    protected String doResolve(AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        return resolverValue;
    }
}