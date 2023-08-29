package org.jboss.pnc.smeg.util;

import com.redhat.resilience.otel.OTelCLIHelper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Based on com.redhat.resilience.otel.OTelCLIHelper
 */
public class OTelCLIHelperW3cPropagation {

    private static Logger log = LoggerFactory.getLogger(OTelCLIHelperW3cPropagation.class);
    private SpanProcessor spanProcessor;

    private Span root = null;

    /**
     * Setup a {@link OtlpGrpcSpanExporter} exporter with the given endpoint.
     *
     * @param endpoint The gRPC endpoint for sending span data
     * @return The {@link OtlpGrpcSpanExporter} instance
     */
    public SpanExporter spanExporter(String endpoint )
    {
        if (endpoint != null && !endpoint.equals("")) {
            return OtlpGrpcSpanExporter.builder().setEndpoint( endpoint ).build();
        } else {
            return NoopSpanExporter.getInstance();
        }
    }

    /**
     * Setup a {@link BatchSpanProcessor} with the supplied {@link SpanExporter}.
     *
     * @param exporter The {@link SpanExporter}, which MAY come from {@link OTelCLIHelper#defaultSpanExporter}
     * @return The {@link BatchSpanProcessor} instance
     */
    public SpanProcessor defaultSpanProcessor( SpanExporter exporter )
    {
        return BatchSpanProcessor.builder( exporter ).build();
    }

    /**
     * Setup {@link GlobalOpenTelemetry} using the provided service name and span processor (which contains an exporter).
     * <p>
     * When the {@link GlobalOpenTelemetry} setup is done, <b>this method will also start a root span</b>, which enables
     * the CLI execution to use {@link Span#current()} to set attributes directly with no further setup required.
     *
     * @param serviceName  This translates into 'service.name' in the span, which is usually required for span validity
     * @param commandName  This is used to name the new span
     * @param grpcEndpoint The gRPC endpoint for sending span data
     * @return
     */
    public Tracer startOTel(String serviceName, String commandName, Map<String, String> contextMap, String grpcEndpoint )
    {
        SpanExporter exporter = spanExporter(grpcEndpoint);
        SpanProcessor processor = defaultSpanProcessor(exporter);
        return startOTel(serviceName, commandName, processor, contextMap );
    }

    /**
     * Setup {@link GlobalOpenTelemetry} using the provided service name and span processor (which contains an exporter).
     * <p>
     * When the {@link GlobalOpenTelemetry} setup is done, <b>this method will also start a root span</b>, which enables
     * the CLI execution to use {@link Span#current()} to set attributes directly with no further setup required.
     *
     * @param serviceName This translates into 'service.name' in the span, which is usually required for span validity
     * @param commandName This is used to name the new span
     * @param processor   This is a span processor that determines how spans are exported
     * @param contextMap  Map with W3C propagation values
     * @return
     */
    public Tracer startOTel(String serviceName, String commandName, SpanProcessor processor, Map<String, String> contextMap)
    {
        if (spanProcessor != null)
        {
            throw new IllegalStateException("startOTel has already been called");
        }
        if (serviceName == null)
        {
            throw new RuntimeException("serviceName must be passed in");
        }
        if (commandName == null)
        {
            commandName = serviceName;
        }

        if (contextMap == null) {
            contextMap = Collections.emptyMap();
        }

        spanProcessor = processor;

        Resource resource = Resource.getDefault()
                                    .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                                                               .addSpanProcessor(processor)
                                                               .setResource(resource)
                                                               .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                                         .setTracerProvider(sdkTracerProvider)
                                                         .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                                                         .buildAndRegisterGlobal();

        Context parentContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), contextMap, textMapGetter());
        Tracer tracer = openTelemetry.getTracer(serviceName);
        root = tracer.spanBuilder(commandName).setParent(parentContext).startSpan();
        root.makeCurrent();
        log.debug("Running with traceId {} spanId {}",
                  Span.current().getSpanContext().getTraceId(), Span.current().getSpanContext().getSpanId());
        return tracer;
    }

    private static TextMapGetter<Map<String, String>> textMapGetter() {
         return new TextMapGetter<Map<String, String>>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }
            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier.get(key);
            }
        };
    }

    public boolean otelEnabled() {
        return spanProcessor != null;
    }

    /**
     * Shutdown the span processor, giving it some time to flush any pending spans out to the exporter.
     */
    public void stopOTel()
    {
        if ( otelEnabled() )
        {
            log.debug("Finishing OpenTelemetry instrumentation for {}", root);
            if (root != null)
            {
                root.end();
            }
            spanProcessor.close();
            spanProcessor = null;
        }
    }

    /**
     * Copied from io.opentelemetry.sdk.trace.export.NoopSpanExporter
     */
    static class NoopSpanExporter implements SpanExporter {

        private static final SpanExporter INSTANCE = new NoopSpanExporter();

        static SpanExporter getInstance() {
            return INSTANCE;
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public String toString() {
            return "NoopSpanExporter{}";
        }
    }
}
