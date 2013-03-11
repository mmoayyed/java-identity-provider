
package net.shibboleth.idp.attribute.resolver.spring;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.service.AbstractSpringService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

// TODO incomplete
/**
 * The attribute resolver service manages the lifecycle of an attribute resolver, where the lifecycle comprises
 * starting, stopping, and configuration reloading.
 */
public class AttributeResolverService extends AbstractSpringService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolverService.class);

    /** The attribute resolver. */
    private AttributeResolver attributeResolver;

    /**
     * TODO finish
     * 
     * The service proxies the underlying components functionality.
     * 
     * @param resolutionContext
     * @throws ResolutionException
     */
    public void resolveAttributes(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        attributeResolver.resolveAttributes(resolutionContext);
    }

    // TODO probably wrong
    protected void doPreStart(HashMap context) throws ServiceException {

        // TODO do we have to init resources here ?
        log.debug("getServiceConfigurations() '{}'", getServiceConfigurations());
        for (Resource resource : this.getServiceConfigurations()) {
            try {
                log.debug("initializing resource '{}'", resource);
                resource.initialize();
            } catch (ComponentInitializationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new ServiceException(e);
            }
        }

        super.doPreStart(context);

        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);

        Map<String, BaseAttributeDefinition> adMap = appCtx.getBeansOfType(BaseAttributeDefinition.class);
        log.debug("Loading {} attribute definitions", adMap.size());

        // data connectors
        Map<String, BaseDataConnector> dataConnectorMap = appCtx.getBeansOfType(BaseDataConnector.class);
        log.debug("Loading {} data connectors", dataConnectorMap.size());

        attributeResolver = new AttributeResolver("resolverId", adMap.values(), dataConnectorMap.values());

        try {
            attributeResolver.initialize();
        } catch (ComponentInitializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
