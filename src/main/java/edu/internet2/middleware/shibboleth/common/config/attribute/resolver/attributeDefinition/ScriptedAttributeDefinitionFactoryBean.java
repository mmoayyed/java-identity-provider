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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import java.io.FileInputStream;
import java.io.IOException;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ScriptedAttributeDefinition;

/**
 * Scripted attribute factory.
 */
public class ScriptedAttributeDefinitionFactoryBean extends BaseAttributeDefinitionFactoryBean {

    /** The scripting language used. */
    private String scriptLanguage;

    /** The file to read the script from. */
    private String scriptFile;

    /** The script. */
    private String script;

    /**
     * Gets the scripting language being used.
     * 
     * @return scripting language being used
     */
    public String getLanguage() {
        return scriptLanguage;
    }

    /**
     * Sets the scripting language being used.
     * 
     * @param language scripting language being used
     */
    public void setLanguage(String language) {
        scriptLanguage = DatatypeHelper.safeTrimOrNullString(language);
    }

    /**
     * Gets the script.
     * 
     * @return the script
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the script.
     * 
     * @param newScript the script
     */
    public void setScript(String newScript) {
        script = DatatypeHelper.safeTrimOrNullString(newScript);
    }

    /**
     * Gets the file to read the script from.
     * 
     * @return file to read the script from
     */
    public String getScriptFile() {
        return scriptFile;
    }

    /**
     * Sets the file to read the script from.
     * 
     * @param file file to read the script from
     */
    public void setScriptFile(String file) {
        scriptFile = DatatypeHelper.safeTrimOrNullString(file);
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ScriptedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        ScriptedAttributeDefinition definition = new ScriptedAttributeDefinition(scriptLanguage);
        populateAttributeDefinition(definition);

        try {
            if (getScript() == null) {
                FileInputStream ins = new FileInputStream(scriptFile);
                byte[] scriptBytes = new byte[ins.available()];
                ins.read(scriptBytes);
                script = new String(script);
            }
        } catch (IOException e) {
            throw e;
        }
        definition.setScript(script);

        definition.initialize();

        return definition;
    }
}