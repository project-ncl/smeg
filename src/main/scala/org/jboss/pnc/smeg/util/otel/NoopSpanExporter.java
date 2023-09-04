package org.jboss.pnc.smeg.util.otel;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;

/**
 * Copied from io.opentelemetry.sdk.trace.export.NoopSpanExporter
 */
public class NoopSpanExporter implements SpanExporter {

    private static final SpanExporter INSTANCE = new NoopSpanExporter();

    public static SpanExporter getInstance() {
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
