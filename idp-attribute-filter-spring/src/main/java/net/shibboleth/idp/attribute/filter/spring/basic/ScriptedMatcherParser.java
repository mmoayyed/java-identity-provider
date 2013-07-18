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

package net.shibboleth.idp.attribute.filter.spring.basic;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.script.ScriptException;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.impl.matcher.ScriptedMatcher;
import net.shibboleth.idp.attribute.filter.impl.policyrule.ScriptedPolicyRule;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO testing
/**
 * Bean definition parser for {@link ScriptedPolicyRule} or {@link ScriptedMatcher} objects.
 */
public class ScriptedMatcherParser extends BaseFilterParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeFilterBasicNamespaceHandler.NAMESPACE, "Script");

    /** Script file element name. */
    public static final QName SCRIPT_FILE_ELEMENT_NAME = new QName(AttributeFilterBasicNamespaceHandler.NAMESPACE,
            "ScriptFile");

    /** Inline Script element name. */
    public static final QName SCRIPT_ELEMENT_NAME = new QName(AttributeFilterBasicNamespaceHandler.NAMESPACE, "Script");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterBasicNamespaceHandler.class);

    /** Prefix for log messages. */
    private String logPrefix;

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        if (isPolicyRule(element)) {
            return ScriptedPolicyRule.class;
        }
        return ScriptedMatcher.class;
    }

    /**
     * Query the DOM and get the script from the appropriate subelements.
     * 
     * @param config The DOM we are interested in
     * @return The script as a string or throws an {@link BeanCreationException}
     */
    @Nonnull private String getScript(Element config) {
        String script = null;
        List<Element> scriptElem = ElementSupport.getChildElements(config, SCRIPT_ELEMENT_NAME);
        List<Element> scriptFileElem = ElementSupport.getChildElements(config, SCRIPT_FILE_ELEMENT_NAME);
        if (scriptElem != null && scriptElem.size() > 0) {
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                log.info("{} definition contains both <Script> and <ScriptFile> elements, using the <Script> element",
                        logPrefix);
            }
            script = scriptElem.get(0).getTextContent();
        } else {
            if (scriptFileElem != null && scriptFileElem.size() > 0) {
                String scriptFile = scriptFileElem.get(0).getTextContent();
                try {
                    script = StringSupport.inputStreamToString(new FileInputStream(scriptFile), null);
                } catch (IOException e) {
                    throw new BeanCreationException("{} Unable to read script file {}"
                            + logPrefix, scriptFile, e);
                }
            }
        }

        if (script == null) {
            throw new BeanCreationException("No script specified for this attribute definition");
        }
        return script;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final String myId = builder.getBeanDefinition().getAttribute("qualifiedId").toString();

        builder.addPropertyValue("id", myId);

        logPrefix = new StringBuilder("Scipted Filter '").append(myId).append("' :").toString();

        String scriptLanguage = "javascript";
        if (config.hasAttributeNS(null, "language")) {
            scriptLanguage = config.getAttributeNS(null, "language");
        }
        log.debug("{}: scripting language: {}.", logPrefix, scriptLanguage);

        String script = getScript(config);
        log.debug("{} script: {}.", logPrefix, script);
        try {
            builder.addConstructorArgValue(new EvaluableScript(scriptLanguage, script));
        } catch (ScriptException e) {
            log.error("{} could not create the EvaluableScript : {}.", logPrefix, e);
            throw new BeanCreationException("Scripted filter :'" + myId + "' Could not create the EvaluableScript");
        }
    }
}