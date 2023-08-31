package org.jboss.pnc.smeg.util;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class OpenTelemetryUtils {

    private static Logger log = LoggerFactory.getLogger(OpenTelemetryUtils.class);

    public static void initGlobal(String serviceName, String exporterEndpoint) {
        Resource resource = Resource
            .getDefault()
            .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider
            .builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter(exporterEndpoint)).build())
            .setResource(resource)
            .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk
            .builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();

        log.debug("Initialized and registered OpenTelemetry.");

    }

    @NotNull
    private static SpanExporter spanExporter(String endpoint) {
        if (endpoint != null && !endpoint.equals("")) {
            return OtlpGrpcSpanExporter.builder().setEndpoint( endpoint ).build();
        } else {
            return NoopSpanExporter.getInstance();
        }
    }

    /**
     * Copied from io.opentelemetry.sdk.trace.export.NoopSpanExporter
     */
    private static class NoopSpanExporter implements SpanExporter {

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
