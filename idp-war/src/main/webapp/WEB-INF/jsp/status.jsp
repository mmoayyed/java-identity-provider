<%@ page language="java" contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormatter" %>
<%@ page import="org.joda.time.format.ISODateTimeFormat" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>
<%@ page import="net.shibboleth.idp.Version" %>
<%@ page import="net.shibboleth.utilities.java.support.component.IdentifiedComponent" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ReloadableService" %>
<%
final RequestContext requestContext = (RequestContext) request.getAttribute("flowRequestContext");
final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();
final DateTime now = DateTime.now();
final DateTime startupTime = new DateTime(requestContext.getActiveFlow().getApplicationContext().getStartupDate());
%>### Operating Environment Information
operating_system: <%= System.getProperty("os.name") %>
operating_system_version: <%= System.getProperty("os.version") %>
operating_system_architecture: <%= System.getProperty("os.arch") %>
jdk_version: <%= System.getProperty("java.version") %>
available_cores: <%= Runtime.getRuntime().availableProcessors() %>
used_memory: <%= Runtime.getRuntime().totalMemory() / 1048576 %> MB
maximum_memory: <%= Runtime.getRuntime().maxMemory() / 1048576 %> MB

### Identity Provider Information
idp_version: <%= Version.getVersion() %>
start_time: <%= startupTime.toString(dateTimeFormatter) %>
current_time: <%= now.toString(dateTimeFormatter) %>
uptime: <%= now.getMillis() - startupTime.getMillis() %> ms

<%
for (final ReloadableService service : (Collection<ReloadableService>) request.getAttribute("services")) {
    final DateTime successfulReload = service.getLastSuccessfulReloadInstant();
    final DateTime lastReload = service.getLastReloadAttemptInstant();
    final Throwable cause = service.getReloadFailureCause();

    out.println("service: " + ((IdentifiedComponent) service).getId());
    if (successfulReload != null) {
        out.println("last successful reload attempt: " + successfulReload.toString(dateTimeFormatter));
    }
    if (lastReload != null) {
        out.println("last reload attempt: " + lastReload.toString(dateTimeFormatter));
    }
    if (cause != null) {
        out.println("last failure cause: " + cause.getClass().getName() + ": " + cause.getMessage());
    }
    
    out.println();
}
%>
