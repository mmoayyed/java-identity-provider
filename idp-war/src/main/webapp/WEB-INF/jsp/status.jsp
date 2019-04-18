<%@ page language="java" contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>
<%@ page import="org.opensaml.saml.metadata.resolver.ChainingMetadataResolver" %>
<%@ page import="org.opensaml.saml.metadata.resolver.MetadataResolver" %>
<%@ page import="org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver" %>
<%@ page import="org.opensaml.saml.metadata.resolver.BatchMetadataResolver" %>
<%@ page import="org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver" %>
<%@ page import="net.shibboleth.idp.Version" %>
<%@ page import="net.shibboleth.idp.saml.metadata.RelyingPartyMetadataProvider" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.AttributeResolver" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.DataConnector" %>
<%@ page import="net.shibboleth.utilities.java.support.component.IdentifiedComponent" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ReloadableService" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ServiceableComponent" %>
<%
final RequestContext requestContext = (RequestContext) request.getAttribute("flowRequestContext");
final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
final Instant now = Instant.now();
final Instant startupTime = Instant.ofEpochMilli(requestContext.getActiveFlow().getApplicationContext().getStartupDate());
%>### Operating Environment Information
operating_system: <%= System.getProperty("os.name") %>
operating_system_version: <%= System.getProperty("os.version") %>
operating_system_architecture: <%= System.getProperty("os.arch") %>
jdk_version: <%= System.getProperty("java.version") %>
available_cores: <%= Runtime.getRuntime().availableProcessors() %>
used_memory: <%= (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 %> MB
maximum_memory: <%= Runtime.getRuntime().maxMemory() / 1048576 %> MB

### Identity Provider Information
idp_version: <%= Version.getVersion() %>
start_time: <%= dateTimeFormatter.format(startupTime) %>
current_time: <%= dateTimeFormatter.format(now) %>
uptime: <%= now.toEpochMilli() - startupTime.toEpochMilli() %> ms

<%
for (final ReloadableService service : (Collection<ReloadableService>) request.getAttribute("services")) {
    final Instant successfulReload = service.getLastSuccessfulReloadInstant();
    final Instant lastReload = service.getLastReloadAttemptInstant();
    final Throwable cause = service.getReloadFailureCause();

    out.println("service: " + ((IdentifiedComponent) service).getId());
    if (successfulReload != null) {
        out.println("last successful reload attempt: " + dateTimeFormatter.format(successfulReload));
    }
    if (lastReload != null) {
        out.println("last reload attempt: " + dateTimeFormatter.format(lastReload));
    }
    if (cause != null) {
        out.println("last failure cause: " + cause.getClass().getName() + ": " + cause.getMessage());
    }
    
    out.println();
    
    if (((IdentifiedComponent) service).getId().contains("Metadata")) {
        final ServiceableComponent<MetadataResolver> component = service.getServiceableComponent();
        if (null != component) {
            try {
                MetadataResolver rootResolver = component.getComponent();
                Collection<RefreshableMetadataResolver> resolvers = Collections.emptyList();
                
                // Step down into wrapping component.
                if (rootResolver instanceof RelyingPartyMetadataProvider) {
                    rootResolver = ((RelyingPartyMetadataProvider) rootResolver).getEmbeddedResolver();
                }
                
                if (rootResolver instanceof ChainingMetadataResolver) {
                    resolvers = new ArrayList<RefreshableMetadataResolver>();
                    for (final MetadataResolver childResolver : ((ChainingMetadataResolver) rootResolver).getResolvers()) {
                        if (childResolver instanceof RefreshableMetadataResolver) {
                            resolvers.add((RefreshableMetadataResolver) childResolver);
                        }
                    }
                } else if (rootResolver instanceof RefreshableMetadataResolver) {
                    resolvers = Collections.<RefreshableMetadataResolver>singletonList((RefreshableMetadataResolver) rootResolver);
                }
                
                for (final RefreshableMetadataResolver resolver : resolvers) {
                    final Instant lastRefresh = resolver.getLastRefresh();
                    final Instant lastUpdate = resolver.getLastUpdate();

                    Instant lastSuccessfulRefresh = null;
                    if (resolver instanceof RefreshableMetadataResolver) {
                        lastSuccessfulRefresh = ((RefreshableMetadataResolver)resolver).getLastSuccessfulRefresh();
                    }
                    Instant rootValidUntil = null;
                    if (resolver instanceof BatchMetadataResolver) {
                        rootValidUntil = ((BatchMetadataResolver)resolver).getRootValidUntil();
                    }
    
                    out.println("\tmetadata source: " + resolver.getId());
                    if (lastRefresh != null) {
                        out.println("\tlast refresh attempt: " + dateTimeFormatter.format(lastRefresh));
                    }
                    if (lastSuccessfulRefresh != null) {
                        out.println("\tlast successful refresh: " + dateTimeFormatter.format(lastSuccessfulRefresh));
                    }
                    if (lastUpdate != null) {
                        out.println("\tlast update: " + dateTimeFormatter.format(lastUpdate));
                    }
                    if (rootValidUntil != null) {
                        out.println("\troot validUntil: " + dateTimeFormatter.format(rootValidUntil));
                    }
                    out.println();
                }
            } finally {
                component.unpinComponent();
            }
        }
    } else if (((IdentifiedComponent) service).getId().contains("AttributeResolver")) {
        final ServiceableComponent<AttributeResolver> component = service.getServiceableComponent();
        if (null != component) {
            try {
                AttributeResolver resolver = component.getComponent();
                for (final DataConnector connector: resolver.getDataConnectors().values()) {
                    final Instant lastFail = connector.getLastFail();
                    if (null != lastFail) {
                        out.println("\tDataConnector " +  connector.getId() + ": last failed at " + dateTimeFormatter.format(lastFail));
                    } else {
                        out.println("\tDataConnector " +  connector.getId() + ": has never failed");
                    }
                    out.println();
                }
            } finally {
                component.unpinComponent();
            }
        }
    
    }    
}
%>
