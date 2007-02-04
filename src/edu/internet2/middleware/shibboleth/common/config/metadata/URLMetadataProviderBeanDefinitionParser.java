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

package edu.internet2.middleware.shibboleth.common.config.metadata;

import javax.xml.namespace.QName;

import org.opensaml.saml2.metadata.provider.URLMetadataProvider;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Spring bean definition parser for Shibboleth file backed url metadata provider definition. 
 */
public class URLMetadataProviderBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:metadata", "URLMetadataProvider");

    /** Metadata URL configuration option attribute name. */
    public static final String METADATA_URL_ATTRIBUTE_NAME = "metadataURL";

    /** Maintain expired metadata configuration option attribute name. */
    public static final String MAINTAIN_EXPIRED_METADATA_ATTRIBUTE_NAME = "maintainExpiredMetadata";

    /** Cache duration configuration option attribute name. */
    public static final String CACHE_DURATION_ATTRIBUTE_NAME = "cacheDuration";

    /** Request timeout configuration option attribute name. */
    public static final String REQUEST_TIMEOUT_ATTRIBUTE_NAME = "requestTimeout";

    /** Basic auth user name configuration option attribute name. */
    public static final String BASIC_AUTH_USER_ATTRIBUTE_NAME = "basicAuthUser";

    /** Basic auth password configuration option attribute name. */
    public static final String BASIC_AUTH_PASSWORD_ATTRIBUTE_NAME = "basicAuthUser";

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return URLMetadataProvider.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        String metadataURL = element.getAttributeNS(null, METADATA_URL_ATTRIBUTE_NAME);
        String basicAuthUser = element.getAttributeNS(null, BASIC_AUTH_USER_ATTRIBUTE_NAME);
        String basicAuthPassword = element.getAttributeNS(null, BASIC_AUTH_PASSWORD_ATTRIBUTE_NAME);

        int requestTimeout = Integer.parseInt(element.getAttributeNS(null, REQUEST_TIMEOUT_ATTRIBUTE_NAME));
        int cacheDuration = Integer.parseInt(element.getAttributeNS(null, CACHE_DURATION_ATTRIBUTE_NAME));

        boolean maintainExpiredMetadata = Boolean.parseBoolean(element.getAttributeNS(null,
                MAINTAIN_EXPIRED_METADATA_ATTRIBUTE_NAME));

        bean.addConstructorArg(metadataURL);
        bean.addConstructorArg(requestTimeout);

        bean.addPropertyValue("maintainExpiredMetadata", maintainExpiredMetadata);
        bean.addPropertyValue("maxCacheDuration", cacheDuration);

        // TODO basic auth credentials
        // bean.addPropertyReference("basicCredentials", beanName)
    }
}