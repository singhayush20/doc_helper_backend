package com.ayushsingh.doc_helper.features.usage_monitoring.controller;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.config.BillingConfig;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.DailyUsageSummaryResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.QuotaInfoResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.UsageBreakdown;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.QuotaManagementService;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.UsageReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageQueryController {

    private final UsageReportingService usageReportingService;
    private final QuotaManagementService quotaManagementService;
    private final BillingConfig billingConfig;

    /**
     * Get current user's quota information.
     */
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @GetMapping("/quota")
    public ResponseEntity<QuotaInfoResponse> getQuotaInfo() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        QuotaInfoResponse quota = usageReportingService.getUserQuotaInfo(userId);
        return ResponseEntity.ok(quota);
    }

    /**
     * Get usage history with pagination.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/history")
    public ResponseEntity<Page<UserTokenUsage>> getUsageHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Long userId = UserContext.getCurrentUser().getUser().getId();
        Page<UserTokenUsage> history = usageReportingService.getUserUsageHistory(userId, pageRequest);

        return ResponseEntity.ok(history);
    }

    /**
     * Get usage for a specific document.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/document")
    public ResponseEntity<Page<UserTokenUsage>> getDocumentUsage(
            @RequestParam Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Long userId = UserContext.getCurrentUser().getUser().getId();
        Page<UserTokenUsage> usage = usageReportingService.getDocumentUsageHistory(userId, documentId, pageRequest);

        return ResponseEntity.ok(usage);
    }

    /**
     * Get current month's token usage (based on quota table).
     */
    @GetMapping("/current-month")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Long> getCurrentMonthUsage() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        Long usage = quotaManagementService.getCurrentMonthlyUsage(userId);
        return ResponseEntity.ok(usage);
    }

    /**
     * Get daily usage summary for last N days.
     */
    @GetMapping("/daily-summary")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<DailyUsageSummaryResponse> getDailySummary(
            @RequestParam(defaultValue = "30") int days) {

        int normalizedDays = Math.min(Math.max(days, 1), 365);

        Long userId = UserContext.getCurrentUser().getUser().getId();
        DailyUsageSummaryResponse summary = usageReportingService.getDailyUsageSummaryForUser(userId, normalizedDays);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get total cost for current month in billing timezone.
     */
    @GetMapping("/cost")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<BigDecimal> getCurrentMonthCost() {
        Long userId = UserContext.getCurrentUser().getUser().getId();

        ZoneId zoneId = ZoneId.of(billingConfig.getBillingTimezone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        Instant startOfMonth = now
                .withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay(zoneId)
                .toInstant();

        BigDecimal cost = usageReportingService.getTotalCost(userId, startOfMonth);
        return ResponseEntity.ok(cost);
    }

    /**
     * Get usage breakdown by operation type (chat vs embedding).
     */
    @GetMapping("/breakdown")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UsageBreakdown> getUsageBreakdown() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        UsageBreakdown breakdown = usageReportingService.getUsageBreakdown(userId);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Get usage breakdown for a specific date range.
     */
    @GetMapping("/breakdown/range")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UsageBreakdown> getUsageBreakdownByRange(
            @RequestParam(required = false) Long startTimestamp,
            @RequestParam(required = false) Long endTimestamp) {

        Long userId = UserContext.getCurrentUser().getUser().getId();
        Instant now = Instant.now();

        Instant defaultStart = now.minus(30, ChronoUnit.DAYS);

        Instant startDate = (startTimestamp != null) ? Instant.ofEpochMilli(startTimestamp) : defaultStart;

        Instant endDate = (endTimestamp != null) ? Instant.ofEpochMilli(endTimestamp) : now;

        if (startDate.isAfter(endDate)) {
            Instant tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        UsageBreakdown breakdown = usageReportingService.getUsageBreakdownByDateRange(userId, startDate, endDate);

        return ResponseEntity.ok(breakdown);
    }
}
