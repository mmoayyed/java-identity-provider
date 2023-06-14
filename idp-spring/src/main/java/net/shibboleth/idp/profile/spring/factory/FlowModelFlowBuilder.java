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

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.webflow.engine.model.BeanImportModel;
import org.springframework.webflow.engine.model.registry.FlowModelHolder;
import org.springframework.webflow.scope.ConversationScope;
import org.springframework.webflow.scope.FlashScope;
import org.springframework.webflow.scope.FlowScope;
import org.springframework.webflow.scope.ViewScope;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.shared.spring.context.FilesystemGenericWebApplicationContext;
import net.shibboleth.shared.spring.custom.SchemaTypeAwareXMLBeanDefinitionReader;

/**
 * This is subclassed in order to customize the Spring {@link ApplicationContext} used for flow configuration.
 */
public class FlowModelFlowBuilder extends org.springframework.webflow.engine.builder.model.FlowModelFlowBuilder {

    /**
     * Constructor.
     *
     * @param flowModelHolder the flow model holder
     */
    public FlowModelFlowBuilder(final FlowModelHolder flowModelHolder) {
        super(flowModelHolder);
    }

    /** {@inheritDoc} */
    @Override
    protected GenericApplicationContext createFlowApplicationContext() {
        final String[] contextResources = parseContextResources(getFlowModel().getBeanImports());
        return createFlowApplicationContext(contextResources);
    }

    /**
     * Pull out the imported resources as Strings so we can resolve them later.
     * 
     * @param beanImports the imports
     * 
     * @return the imported resource paths
     */
    @Nonnull private String[] parseContextResources(@Nullable final List<BeanImportModel> beanImports) {
        if (beanImports != null && !beanImports.isEmpty()) {
            final String[] resources = new String[beanImports.size()];
            final @Nonnull List<String> resultAsList = beanImports.stream()
                    .map(BeanImportModel::getResource)
                    .collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableList()))
                    .get();
            final String result[] = resultAsList.toArray(resources);
            assert result != null;
            return result;
        }
        return new String[0];
    }

    /**
     * Create our own {@link GenericApplicationContext} using our own builder logic. 
     * 
     * @param resources the imports
     * 
     * @return the context
     */
    private GenericApplicationContext createFlowApplicationContext(@Nonnull final String[] resources) {
        final ApplicationContext parent = getContext().getApplicationContext();
        final GenericApplicationContext flowContext;
        if (parent instanceof WebApplicationContext) {
            // Shibboleth change - Use our override of GenericWebApplicationContext.
            final GenericWebApplicationContext webContext = new FilesystemGenericWebApplicationContext();
            webContext.setServletContext(((WebApplicationContext) parent).getServletContext());
            flowContext = webContext;
        } else {
            // Shibboleth change - Use our override of GenericApplicationContext.
            flowContext = new FilesystemGenericApplicationContext();
        }
        flowContext.setDisplayName("Flow ApplicationContext [" + getContext().getFlowId() + "]");
        flowContext.setParent(parent);
        flowContext.getBeanFactory().registerScope("request", new RequestScope());
        flowContext.getBeanFactory().registerScope("flash", new FlashScope());
        flowContext.getBeanFactory().registerScope("view", new ViewScope());
        flowContext.getBeanFactory().registerScope("flow", new FlowScope());
        flowContext.getBeanFactory().registerScope("conversation", new ConversationScope());
        
        // Ensure the current ClassLoader is used, or otherwise setting the ResourceLoader would suppress it
        final ClassLoader classLoaderToUse = flowContext.getClassLoader();
        flowContext.setClassLoader(classLoaderToUse);

        final Resource flowResource = getFlowModelHolder().getFlowModelResource();
        flowContext.setResourceLoader(new FlowRelativeResourceLoader(flowResource));

        // Shibboleth change - override the property placeholder syntax.
        flowContext.getEnvironment().setPlaceholderPrefix("%{");
        flowContext.getEnvironment().setPlaceholderSuffix("}");

        AnnotationConfigUtils.registerAnnotationConfigProcessors(flowContext);

        // Shibboleth change - auto-inject the property replacement bean.
        final PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertyConfigurer.setPlaceholderPrefix("%{");
        propertyConfigurer.setPlaceholderSuffix("}");
        propertyConfigurer.setEnvironment(flowContext.getEnvironment());
        flowContext.addBeanFactoryPostProcessor(propertyConfigurer);
        
        // Shibboleth change - Use our document reader instead, which fixes import resolution.
        new SchemaTypeAwareXMLBeanDefinitionReader(flowContext).loadBeanDefinitions(resources);
        
        registerFlowBeans(flowContext.getBeanFactory());

        flowContext.refresh();

        return flowContext;
    }

}