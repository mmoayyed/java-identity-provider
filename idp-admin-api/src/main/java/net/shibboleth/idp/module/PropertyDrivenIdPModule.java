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

package net.shibboleth.idp.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Implementation of {@link IdPModule} relying on Java {@link Properties}.
 * 
 * @since 4.1.0
 */
public class PropertyDrivenIdPModule extends AbstractIdPModule {

    /** Default name of module properties resource. */
    @Nonnull @NotEmpty public static final String DEFAULT_RESOURCE = "module.properties";

    /** Suffix of property for module name. */
    @Nonnull @NotEmpty public static final String MODULE_NAME_PROPERTY = ".name";

    /** Suffix of property for module description. */
    @Nonnull @NotEmpty public static final String MODULE_DESC_PROPERTY = ".desc";

    /** Suffix of property for module URL. */
    @Nonnull @NotEmpty public static final String MODULE_URL_PROPERTY = ".url";

    /** Suffix of property for module plugin owner. */
    @Nonnull @NotEmpty public static final String MODULE_PLUGIN_PROPERTY = ".plugin";

    /** Suffix of property for resource source. */
    @Nonnull @NotEmpty public static final String MODULE_SRC_PROPERTY = ".src";

    /** Suffix of property for resource destination. */
    @Nonnull @NotEmpty public static final String MODULE_DEST_PROPERTY = ".dest";

    /** Suffix of property for resource replacement. */
    @Nonnull @NotEmpty public static final String MODULE_REPLACE_PROPERTY = ".replace";

    /** Suffix of property for resource optionality. */
    @Nonnull @NotEmpty public static final String MODULE_OPTIONAL_PROPERTY = ".optional";

    /** Suffix of property for resource executability. */
    @Nonnull @NotEmpty public static final String MODULE_EXEC_PROPERTY = ".exec";

    /** Suffix of property for module post-enable message. */
    @Nonnull @NotEmpty public static final String MODULE_POSTENABLE_PROPERTY = ".postenable";

    /** Suffix of property for module post-disable message. */
    @Nonnull @NotEmpty public static final String MODULE_POSTDISABLE_PROPERTY = ".postdisable";

    /** Suffix of property for module languages. */
    @Nonnull @NotEmpty public static final String MODULE_LANGS_PROPERTY = ".langs";

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(PropertyDrivenIdPModule.class);

    /** Properties describing module. */
    @Nonnull private final Properties moduleProperties;

    /** Module ID. */
    @Nonnull @NotEmpty private String moduleId;

    /** Module name. */
    @Nonnull @NotEmpty private String moduleName;

    /** Module URL. */
    @Nullable @NotEmpty private String moduleURL;
    
    /** Plugin ID. */
    @Nullable @NotEmpty private String pluginId;
    
    /** Available message locales. */
    @Nonnull @NonnullElements private List<String> locales;
    
    /** Whether to require an HTTP client. */
    private boolean requireHttpClient;

    /**
     * Constructor.
     *
     * @param claz type of object used to locate default module.properties resource
     * 
     * @throws IOException if unable to read file
     * @throws ModuleException if the module is not in a valid state
     */
    public PropertyDrivenIdPModule(@Nonnull final Class<? extends IdPModule> claz) throws IOException, ModuleException {
        this(claz.getResourceAsStream(DEFAULT_RESOURCE));
    }
    
    /**
     * Constructor.
     *
     * @param inputStream property stream
     * 
     * @throws IOException if unable to read file
     * @throws ModuleException if the module is not in a valid state
     */
    public PropertyDrivenIdPModule(@Nonnull final InputStream inputStream)
            throws IOException, ModuleException {
        locales = CollectionSupport.emptyList();
        moduleProperties = new Properties();
        moduleProperties.load(inputStream);
        moduleId = "";
        moduleName = "";
        load();
    }

    /**
     * Constructor.
     *
     * @param properties property set
     * 
     * @throws ModuleException if the module is not in a valid state
     */
    public PropertyDrivenIdPModule(@Nonnull final Properties properties) throws ModuleException {
        locales = CollectionSupport.emptyList();
        moduleProperties = Constraint.isNotNull(properties, "Properties cannot be null");
        moduleId = "";
        moduleName = "";
        load();
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Load module information from properties.
     * 
     * @throws ModuleException on errors
     */
    protected void load() throws ModuleException {
        try {
            moduleId = Constraint.isNotNull(
                    StringSupport.trimOrNull(moduleProperties.getProperty(getClass().getName())),
                    "Module ID missing from properties");
            
            moduleName = Constraint.isNotNull(
                    StringSupport.trimOrNull(moduleProperties.getProperty(getId() + MODULE_NAME_PROPERTY)),
                    "Module name missing from properties");
            moduleURL = StringSupport.trimOrNull(moduleProperties.getProperty(getId() + MODULE_URL_PROPERTY));
            pluginId = StringSupport.trimOrNull(moduleProperties.getProperty(getId() + MODULE_PLUGIN_PROPERTY));
            
            locales = StringSupport.stringToList(
                    moduleProperties.getProperty(getId() + MODULE_LANGS_PROPERTY, ""), ", ");
            
            final Collection<BasicModuleResource> resources = new ArrayList<>();
            
            for (Integer rnum = 1; ; ++rnum) {
                
                final String renumstr = "." + rnum.toString();
                
                final String src = moduleProperties.getProperty(getId() + renumstr + MODULE_SRC_PROPERTY);
                final String dest = moduleProperties.getProperty(getId() + renumstr + MODULE_DEST_PROPERTY);
                if (src == null || dest == null) {
                    break;
                }
                
                final Boolean replace = Boolean.valueOf(
                        moduleProperties.getProperty(getId() + renumstr + MODULE_REPLACE_PROPERTY, "false"));
                
                final Boolean optional = Boolean.valueOf(
                        moduleProperties.getProperty(getId() + renumstr + MODULE_OPTIONAL_PROPERTY, "false"));
                
                final Boolean exec = Boolean.valueOf(
                        moduleProperties.getProperty(getId() + renumstr + MODULE_EXEC_PROPERTY, "false"));
                
                final Path destPath = Path.of(dest);
                if (dest.contains("..") || destPath.isAbsolute() || destPath.startsWith("/")) {
                    throw new ModuleException("Module contained a suspect resource destination");
                }
                
                if (!requireHttpClient) {
                    requireHttpClient = src.startsWith("https://") || src.startsWith("http://");
                }
                
                resources.add(new BasicModuleResource(src, destPath, replace, optional, exec));
            }
            
            setResources(resources);
            
            log.debug("Module {} loaded", getId());
            resources.forEach(
                    r -> log.debug("Module {}: Resource {} -> {} ({})",
                            getId(), r.getSource(), r.getDestination(), r.isReplace() ? "replace" : "noreplace"));
        } catch (final ConstraintViolationException e) {
            throw new ModuleException(e);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
        return moduleId;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName(@Nullable final ModuleContext moduleContext) {
        
        if (moduleContext != null) {
            final String best = Locale.lookupTag(moduleContext.getLanguageRanges(), locales);
            if (best != null && !best.equals(locales.get(0))) {
                return moduleProperties.getProperty(getId() + MODULE_NAME_PROPERTY + "." + best, moduleName);
            }
        }
        return moduleName;
    }
    
    /** {@inheritDoc} */
    @Nullable @NotEmpty public String getDescription(@Nullable final ModuleContext moduleContext) {

        if (moduleContext != null) {
            final String best = Locale.lookupTag(moduleContext.getLanguageRanges(), locales);
            if (best != null && !best.equals(locales.get(0))) {
                final String desc = moduleProperties.getProperty(getId() + MODULE_DESC_PROPERTY + "." + best);
                if (!Strings.isNullOrEmpty(desc)) {
                    return desc;
                }
            }
        }
        
        return StringSupport.trimOrNull(moduleProperties.getProperty(getId() + MODULE_DESC_PROPERTY));
    }

    /** {@inheritDoc} */
    @Nullable @NotEmpty public String getURL() {
        return moduleURL;
    }
    
    /**
     * Set the module URL.
     * 
     * @param url URL to set
     */
    public void setURL(@Nullable @NotEmpty final String url) {
        moduleURL = StringSupport.trimOrNull(url);
    }
    
    /** {@inheritDoc} */
    @Nullable @NotEmpty public String getOwnerId() {
        return pluginId;
    }
    
    /** {@inheritDoc} */
    public boolean isHttpClientRequired() {
        return requireHttpClient;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements public Map<ModuleResource,ResourceResult> enable(
            @Nonnull final ModuleContext moduleContext) throws ModuleException {
        final Map<ModuleResource,ResourceResult> results = super.enable(moduleContext);
        
        final PrintStream msgStream = moduleContext.getMessageStream();
        if (msgStream != null) {
            
            String msg = null;
            
            final String best = Locale.lookupTag(moduleContext.getLanguageRanges(), locales);
            if (best != null && !best.equals(locales.get(0))) {
                msg = moduleProperties.getProperty(getId() + MODULE_POSTENABLE_PROPERTY + "." + best);
            }
            
            if (msg == null) {
                msg = moduleProperties.getProperty(getId() + MODULE_POSTENABLE_PROPERTY);
            }
            
            if (msg != null) {
                msgStream.println(msg);
            }
        }
        
        return results;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements public Map<ModuleResource,ResourceResult> disable(
            @Nonnull final ModuleContext moduleContext, final boolean clean) throws ModuleException {
        final Map<ModuleResource,ResourceResult> results = super.disable(moduleContext, clean);

        final PrintStream msgStream = moduleContext.getMessageStream();
        if (msgStream != null) {
            
            String msg = null;
            
            final String best = Locale.lookupTag(moduleContext.getLanguageRanges(), locales);
            if (best != null && !best.equals(locales.get(0))) {
                msg = moduleProperties.getProperty(getId() + MODULE_POSTDISABLE_PROPERTY + "." + best);
            }
            
            if (msg == null) {
                msg = moduleProperties.getProperty(getId() + MODULE_POSTDISABLE_PROPERTY);
            }

            if (msg != null) {
                msgStream.println(msg);
            }
        }
        
        return results;
    }

}