package org.jboss.pnc.smeg.util;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Based on com.redhat.resilience.otel.OTelCLIHelper
 */
public class W3cParentContextExtractor {

    private static Logger log = LoggerFactory.getLogger(W3cParentContextExtractor.class);

    public Context startOTel(Map<String, String> contextMap) {
        if (contextMap == null) {
            contextMap = Collections.emptyMap();
        }
        Context parentContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), contextMap, textMapGetter());
        return parentContext;
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
}
