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

package edu.internet2.middleware.shibboleth.idp.consent.mock;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;
/**
 * Only a mock for testing
 */
public class IdPContext {
   
	private String entityId = null;
	private Map<String, BaseAttribute<String>> attributes = null;
	
	public IdPContext() {}
	
	public IdPContext(String entityId, Map<String, BaseAttribute<String>> attributes) {
		this.entityId = entityId;
		this.attributes = attributes;
	}
	
	public String getEntityID() {
		return entityId;
	}

	public Map<String, BaseAttribute<String>> getReleasedAttributes() {
		return attributes;
	}
	
	public String getPrincipalName() {
	    return "demouser";
	}
	
	public HttpServletRequest getRequest() {
	    return new MockHttpServletRequest();
	}

}
