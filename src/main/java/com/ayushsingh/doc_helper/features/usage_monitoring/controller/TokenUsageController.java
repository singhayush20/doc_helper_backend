package com.ayushsingh.doc_helper.features.usage_monitoring.controller;

import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.DailyUsageSummaryResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.QuotaInfoResponse;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.UpdateTierRequest;
import com.ayushsingh.doc_helper.features.usage_monitoring.dto.UsageBreakdown;
import com.ayushsingh.doc_helper.features.usage_monitoring.entity.UserTokenUsage;
import com.ayushsingh.doc_helper.features.usage_monitoring.service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    /**
     * Get current user's quota information
     */
    @GetMapping("/quota")
    public ResponseEntity<QuotaInfoResponse> getQuotaInfo() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        QuotaInfoResponse quota = tokenUsageService.getUserQuotaInfo(userId);
        return ResponseEntity.ok(quota);
    }

    /**
     * Get usage history with pagination
     */
    @GetMapping("/history")
    public ResponseEntity<Page<UserTokenUsage>> getUsageHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserTokenUsage> history = tokenUsageService.getUserUsageHistory(
                pageRequest);

        return ResponseEntity.ok(history);
    }

    /**
     * Get usage for a specific document
     */
    @GetMapping("/document")
    public ResponseEntity<Page<UserTokenUsage>> getDocumentUsage(
            @RequestParam Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<UserTokenUsage> usage = tokenUsageService.getDocumentUsageHistory(
                documentId, pageRequest);

        return ResponseEntity.ok(usage);
    }

    /**
     * Get current month's token usage
     */
    @GetMapping("/current-month")
    public ResponseEntity<Long> getCurrentMonthUsage() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        Long usage = tokenUsageService.getCurrentMonthUsage(userId);
        return ResponseEntity.ok(usage);
    }

    /**
     * Get daily usage summary for last N days
     */

    @GetMapping("/daily-summary")
    public ResponseEntity<DailyUsageSummaryResponse> getDailySummary(
            @RequestParam(defaultValue = "30") int days) {

        DailyUsageSummaryResponse summary = tokenUsageService.getDailyUsageSummaryForDays(
                days);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get total cost for current month
     */
    @GetMapping("/cost")
    public ResponseEntity<BigDecimal> getCurrentMonthCost() {
        Long userId = UserContext.getCurrentUser().getUser().getId();

        Instant startOfMonth = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                .withDayOfMonth(1)
                .toInstant();

        BigDecimal cost = tokenUsageService.getTotalCost(userId, startOfMonth);
        return ResponseEntity.ok(cost);
    }

    /**
     * Get usage breakdown by operation type (chat vs embedding)
     */
    @GetMapping("/breakdown")
    public ResponseEntity<UsageBreakdown> getUsageBreakdown() {
        Long userId = UserContext.getCurrentUser().getUser().getId();
        UsageBreakdown breakdown = tokenUsageService.getUsageBreakdown(userId);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Get usage breakdown for a specific date range
     */
    @GetMapping("/breakdown/range")
    public ResponseEntity<UsageBreakdown> getUsageBreakdownByRange(
            @RequestParam(required = false) Long startTimestamp,
            @RequestParam(required = false) Long endTimestamp) {
        Long userId = UserContext.getCurrentUser().getUser().getId();

        Instant startDate =
                startTimestamp != null ? Instant.ofEpochMilli(startTimestamp) :
                        Instant.now().minus(30, ChronoUnit.DAYS);

        Instant endDate =
                endTimestamp != null ? Instant.ofEpochMilli(endTimestamp) :
                        Instant.now();

        UsageBreakdown breakdown = tokenUsageService.getUsageBreakdownByDateRange(
                userId, startDate, endDate);

        return ResponseEntity.ok(breakdown);
    }

    /**
     * Update user tier (admin endpoint)
     */
    @PatchMapping("/tier")
    public ResponseEntity<Void> updateTier(
            @RequestBody UpdateTierRequest request) {
        // Add admin authorization check here
        tokenUsageService.updateUserTier(request.getUserId(), request.getTier(),
                request.getMonthlyLimit());
        return ResponseEntity.ok().build();
    }
}
