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

package edu.internet2.middleware.shibboleth.common.config.security;

import javax.xml.namespace.QName;

import org.opensaml.ws.security.provider.CertificateNameOptions;
import org.opensaml.xml.security.x509.X500DNHandler;
import org.opensaml.xml.security.x509.X509Util;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.binding.security.ShibbolethClientCertAuthRule;

/** Spring bean definition parser for {urn:mace:shibboleth:2.0:security}ClientCertificate elements. */
public class ClientCertAuthRuleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "ClientCertAuth");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ShibbolethClientCertAuthRule.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        builder.addConstructorArgReference(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "trustEngineRef")));
        
        CertificateNameOptions nameOptions = new CertificateNameOptions();
        nameOptions.setX500SubjectDNFormat(X500DNHandler.FORMAT_RFC2253);
        nameOptions.setEvaluateSubjectDN(false);
        nameOptions.setEvaluateSubjectCommonName(true);
        nameOptions.getSubjectAltNames().add(X509Util.DNS_ALT_NAME);
        nameOptions.getSubjectAltNames().add(X509Util.URI_ALT_NAME);
        
        builder.addConstructorArgValue(nameOptions);
    }
    
    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}