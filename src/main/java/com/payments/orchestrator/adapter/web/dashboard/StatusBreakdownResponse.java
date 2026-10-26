package com.payments.orchestrator.adapter.web.dashboard;

import lombok.Builder;
import lombok.Getter;

/**
 * Transaction count broken down by status.
 */
@Getter
@Builder
public class StatusBreakdownResponse {

    private final long initiated;
    private final long routing;
    private final long authorized;
    private final long captured;
    private final long settled;
    private final long failed;
    private final long retrying;
    private final long failedFinal;
    private final long total;
}