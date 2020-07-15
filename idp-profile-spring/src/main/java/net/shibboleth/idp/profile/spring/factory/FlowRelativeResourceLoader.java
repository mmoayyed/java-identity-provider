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

package net.shibboleth.idp.profile.spring.factory;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * This code is copied verbatim from org.springframework.webflow.engine.builder.model.FlowRelativeResourceLoader
 * 
 * A resource loader that loads other resources relative to a Flow definition resource. Allows for easy loading of
 * flow-relative resources using the standard {@link ResourceLoader} interface.
 * 
 * @author Keith Donald
 */
class FlowRelativeResourceLoader implements ResourceLoader {

	private Resource flowResource;

	public FlowRelativeResourceLoader(Resource resource) {
		this.flowResource = resource;
	}

	public ClassLoader getClassLoader() {
		return flowResource.getClass().getClassLoader();
	}

	public Resource getResource(String location) {
		if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		} else {
			return createFlowRelativeResource(location);
		}
	}

	private Resource createFlowRelativeResource(String location) {
		try {
			return flowResource.createRelative(location);
		} catch (IOException e) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"Unable to access a flow relative resource at location '" + location + "'");
			iae.initCause(e);
			throw iae;
		}
	}
}