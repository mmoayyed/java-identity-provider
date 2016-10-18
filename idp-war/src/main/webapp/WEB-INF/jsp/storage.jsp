<%@ page language="java" contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="com.fasterxml.jackson.core.JsonFactory" %>
<%@ page import="com.fasterxml.jackson.core.JsonGenerator" %>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.opensaml.storage.StorageRecord" %>
<%@ page import="org.opensaml.storage.StorageService" %>
<%
final String context = (String) request.getAttribute("context");
final String key = (String) request.getAttribute("key");
final StorageService storageService = (StorageService) request.getAttribute("storageService");

response.setContentType("application/vnd.api+json");

if (storageService == null) {
    response.setStatus(404);
%>
{
  "errors": [
    {
      "status": "404",
      "title":  "Invalid Storage Service",
      "detail": "Invalid storage service identifier in path."
    }
  ]
}
<%
} else if (context == null || key == null) {
    response.setStatus(404);
%>
{
  "errors": [
    {
      "status": "404",
      "title":  "Missing Context or Key",
      "detail": "No context or key specified."
    }
  ]
}
<%
} else if ("GET".equals(request.getMethod())) {
    final StorageRecord record = storageService.read(context, key);
    if (record != null) {
        response.setStatus(200);
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonGenerator g = jsonFactory.createGenerator(response.getWriter()).useDefaultPrettyPrinter();
        g.setCodec((ObjectMapper) request.getAttribute("jsonMapper"));
        g.writeStartObject();
        g.writeObjectFieldStart("data");
        g.writeStringField("type", "records");
        g.writeStringField("id", storageService.getId() + '/' + context +'/' + key);
        g.writeObjectFieldStart("attributes");
        g.writeStringField("value", record.getValue());
        g.writeNumberField("version", record.getVersion());
        if (record.getExpiration() != null) {
            g.writeFieldName("expiration");
            g.writeObject(new DateTime(record.getExpiration()));
        }
        g.close();
    } else {
        response.setStatus(404);
%>
{
  "errors": [
    {
      "status": "404",
      "title":  "Record Not Found",
      "detail": "The specified record was not present or has expired."
    }
  ]
}
<%
    }
} else if ("DELETE".equals(request.getMethod())) {
    if (storageService.delete(context, key)) {
        response.setStatus(204);
    } else {
        response.setStatus(404);
%>
{
  "errors": [
    {
      "status": "404",
      "title":  "Record Not Found",
      "detail": "The specified record was not present or has expired."
    }
  ]
}
<%
    }
} else {
    response.setStatus(405);
%>
{
  "errors": [
    {
      "status": "405",
      "title":  "Unknown Operation",
      "detail": "Only GET and DELETE are supported."
    }
  ]
}
<%
}
%>
