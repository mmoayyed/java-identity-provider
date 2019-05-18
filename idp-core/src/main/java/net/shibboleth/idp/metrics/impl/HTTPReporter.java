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

package net.shibboleth.idp.metrics.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.SortedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A metrics reporter that runs at scheduled times and posts a JSON feed of metrics to a designated endpoint.
 */
public class HTTPReporter extends ScheduledReporter implements InitializableComponent {

    /** Default date/time format string. */
    @Nonnull @NotEmpty public static final String DEFAULT_DT_FORMAT = "YYYY-MM-dd'T'HH:mm:ss.SSSZZ";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTTPReporter.class);

    /** Registry of metrics. */
    @Nonnull private final MetricRegistry metricRegistry;
    
    /** Filter to apply. */
    @Nonnull private final MetricFilter metricFilter;

    /** Rate unit. */
    @Nonnull private final TimeUnit rateUnit;

    /** Duration unit. */
    @Nonnull private final TimeUnit durationUnit;

    /** HTTP Client used to post the data. */
    @NonnullAfterInit private HttpClient httpClient;
    
    /** URL to the collection point. */
    @NonnullAfterInit @NotEmpty private String collectorURL;

    /** HTTP client security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;
    
    /** JSON object mapper to produce output. */
    @NonnullAfterInit private ObjectMapper jsonMapper;
    
    /** Formatting string for {@link DateFormat} fields. */
    @Nullable private String dateTimeFormat;

    /** Whether this component has been initialized. */
    private boolean isInitialized;

    /**
     * Constructor.
     *
     * @param registry the registry of metrics to report
     * @param name the reporter name
     * @param filter filter to apply
     */
    public HTTPReporter(@Nonnull @ParameterName(name="registry") final MetricRegistry registry,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name,
            @Nullable @ParameterName(name="filter") final MetricFilter filter) {
        super(registry, name, filter, TimeUnit.SECONDS, TimeUnit.SECONDS);
        metricRegistry = registry;
        metricFilter = filter != null ? filter : MetricFilter.ALL;
        rateUnit = TimeUnit.SECONDS;
        durationUnit = TimeUnit.SECONDS;
    }

    /**
     * Constructor.
     *
     * @param registry the registry of metrics to report
     * @param name the reporter name
     * @param filter filter to apply
     * @param rUnit unit to apply to rate information
     * @param dUnit unit to apply to duration information
     */
    public HTTPReporter(@Nonnull @ParameterName(name="registry") final MetricRegistry registry,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name,
            @Nullable @ParameterName(name="filter") final MetricFilter filter,
            @Nonnull @ParameterName(name="rUnit") final TimeUnit rUnit,
            @Nonnull @ParameterName(name="dUnit") final TimeUnit dUnit) {
        super(registry, name, filter, rUnit, dUnit);
        metricRegistry = registry;
        metricFilter = filter != null ? filter : MetricFilter.ALL;
        rateUnit = rUnit;
        durationUnit = dUnit;
    }

// Checkstyle: ParameterNumber OFF
    /**
     * Constructor.
     *
     * @param registry the registry of metrics to report
     * @param name the reporter name
     * @param filter filter to apply
     * @param rUnit unit to apply to rate information
     * @param dUnit unit to apply to duration information
     * @param executor task scheduler
     */
    public HTTPReporter(@Nonnull @ParameterName(name="registry") final MetricRegistry registry,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name,
            @Nullable @ParameterName(name="filter") final MetricFilter filter,
            @Nonnull @ParameterName(name="rUnit") final TimeUnit rUnit,
            @Nonnull @ParameterName(name="dUnit") final TimeUnit dUnit,
            @Nonnull @ParameterName(name="executor") final ScheduledExecutorService executor) {
        super(registry, name, filter, rUnit, dUnit, executor);
        metricRegistry = registry;
        metricFilter = filter != null ? filter : MetricFilter.ALL;
        rateUnit = rUnit;
        durationUnit = dUnit;
    }
// Checkstyle: ParameterNumber ON

    /**
     * Set the {@link HttpClient} to use.
     * 
     * @param client client to use
     */
    public void setHttpClient(@Nonnull final HttpClient client) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
    }
    
    /**
     * Set the collection point to supply the data to.
     * 
     * @param url URL to post data to
     */
    public void setCollectorURL(@Nonnull @NotEmpty final String url) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        collectorURL = Constraint.isNotNull(StringSupport.trimOrNull(url), "Collector URL cannot be null or empty");
    }

    /**
     * Set the optional client security parameters.
     * 
     * @param params the new client security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        httpClientSecurityParameters = params;
    }
    
    /**
     * Set the {@link DateFormat} formatting string to apply when writing {@link DateFormat}-valued fields.
     * 
     * @param format formatting string
     */
    public void setDateTimeFormat(@Nullable @NotEmpty final String format) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        dateTimeFormat = StringSupport.trimOrNull(format);
    }
    
    /** {@inheritDoc} */
    public boolean isInitialized() {
        return isInitialized;
    }

    /** {@inheritDoc} */
    public void initialize() throws ComponentInitializationException {
        if (!isInitialized) {
            if (httpClient == null || collectorURL == null) {
                throw new ComponentInitializationException("HttpClient and collection URL cannot be null");
            }

            jsonMapper = new ObjectMapper().registerModule(
                    new MetricsModule(rateUnit, durationUnit, true, metricFilter));
            jsonMapper.registerModule(new JavaTimeModule());
            jsonMapper.setDateFormat(new SimpleDateFormat(dateTimeFormat != null ? dateTimeFormat : DEFAULT_DT_FORMAT));
            jsonMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
            
            isInitialized = true;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void stop() {
        super.stop();
        httpClient = null;
        httpClientSecurityParameters = null;
    }
        
    /** {@inheritDoc} */
    @Override
    public void report() {
        synchronized (this) {
            try {
                final HttpPost httpRequest = new HttpPost(collectorURL);
                final HttpClientContext httpContext = buildHttpContext(httpRequest);

                final ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
                final ObjectWriter writer = jsonMapper.writer();
                writer.writeValue(output, metricRegistry);

                // Construct streamed request body.
                final EntityBuilder entityBuilder = EntityBuilder.create();
                entityBuilder.setContentType(ContentType.APPLICATION_JSON);
                entityBuilder.setBinary(output.toByteArray());
                httpRequest.setEntity(entityBuilder.build());

                final HttpResponse response = httpClient.execute(httpRequest, httpContext);
                HttpClientSecuritySupport.checkTLSCredentialEvaluated(httpContext, httpRequest.getURI().getScheme());
                
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    log.debug("Metrics delivered successfully to collector");
                } else {
                    log.error("Collector responded with HTTP status {}", response.getStatusLine().getStatusCode());
                }
            } catch (final IOException e) {
                log.error("Error sending metric registry to collection point {}", collectorURL, e);
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void report(final SortedMap<String, Gauge> gauges, final SortedMap<String, Counter> counters,
            final SortedMap<String, Histogram> histograms, final SortedMap<String, Meter> meters,
            final SortedMap<String, Timer> timers) {
        throw new UnsupportedOperationException("The per-metric report method should never be called.");
    }


    /**
     * Build the {@link HttpClientContext} instance to be used by the HttpClient.
     * 
     * @param request the HTTP client request
     * @return the client context instance
     */
    @Nonnull private HttpClientContext buildHttpContext(@Nonnull final HttpUriRequest request) {
        final HttpClientContext clientContext = HttpClientContext.create();
        HttpClientSecuritySupport.marshalSecurityParameters(clientContext, httpClientSecurityParameters, false);
        HttpClientSecuritySupport.addDefaultTLSTrustEngineCriteria(clientContext, request);
        return clientContext;
    }

}