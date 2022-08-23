/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app.health;

import com.redhat.cloud.policies.app.lightweight.LightweightEngine;
import com.redhat.cloud.policies.app.StuffHolder;
import com.redhat.cloud.policies.app.model.Policy;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.extension.annotations.WithSpan;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * We gather the Status of ourselves and remotes.
 * This is then put into the StuffHolder from
 * where both the Gauge below and the Status rest
 * endpoint can fetch it.
 */
@ApplicationScoped
public class ScheduledStatusProducer {

    public static final String DUMMY = "dummy";

    private static final String DUMMY_CONDITION = "facts.arch = 'x86_64'";

    @Inject
    @RestClient
    LightweightEngine lightweightEngine;

    @Inject
    OpenTelemetry otel;

    //  // Quarkus only activates this after the first REST-call to any method in this class
    @Gauge(name = "status_isDegraded", unit = MetricUnits.NONE, absolute = true,
            description = "Returns 0 if good, value > 0 for number of entries in the status message")
    int isDegraded() {
        return StuffHolder.getInstance().getStatusInfo().size();
    }

    @WithSpan
    @Scheduled(every = "10s")
    void gather() {

        Map<String, String> issues;
        issues = new HashMap<>();

        Tracer tracer = otel.getTracer("scheduled-status");

        // Admin has used the endpoint to signal degraded status
        boolean degraded = StuffHolder.getInstance().isDegraded();
        if (degraded) {
            issues.put("admin-degraded", "true");
        }

        // Now the normal checks
        Span span = tracer.spanBuilder("check-db")
            .startSpan();

        try {
            Policy.findByNameOrgId(DUMMY, "-dummy-");
        } catch (Exception e) {
            issues.put("backend-db", e.getMessage());
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Issue talking to the DB");
        }
        finally {
          span.end();
        }


      span = tracer.spanBuilder("check engine").startSpan();
        try (Scope scope = span.makeCurrent()){
            span.addEvent("Call Engine");
            lightweightEngine.validateCondition(DUMMY_CONDITION);
        } catch (Exception e) {
            issues.put("engine", e.getMessage());
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Can't call engine");
        } finally {
          span.end();
        }

        StuffHolder.getInstance().setStatusInfo(issues);
    }

    public void update() {
        gather();
    }
}
