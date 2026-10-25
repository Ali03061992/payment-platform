package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.SupplierFinancialReportResponse;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.infrastructure.http.PaymentSummaryClient;
import com.paymentplatform.shared.domain.exception.DomainException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GetSupplierFinancialReportUseCase {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Africa/Tunis");
    private static final long MAX_RANGE_DAYS = 3660;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");
    private static final Set<String> EXCLUDED_FROM_REVENUE = Set.of("CANCELLED", "REJECTED");
    private static final Set<String> OPEN_STATUSES = Set.of(
            "DRAFT", "CONFIRMED", "PREPARING", "READY_FOR_DELIVERY",
            "DELIVERY_ACCEPTED", "IN_DELIVERY");
    private static final Set<String> DELIVERED_STATUSES = Set.of("DELIVERED", "ACCEPTED");

    private final OrderRepository orderRepository;
    private final PaymentSummaryClient paymentSummaryClient;

    public GetSupplierFinancialReportUseCase(OrderRepository orderRepository,
                                              PaymentSummaryClient paymentSummaryClient) {
        this.orderRepository = orderRepository;
        this.paymentSummaryClient = paymentSummaryClient;
    }

    public SupplierFinancialReportResponse execute(UUID supplierId, LocalDate from, LocalDate to,
                                                   String granularity) {
        ReportGranularity resolvedGranularity = ReportGranularity.parse(granularity);
        LocalDate today = LocalDate.now(REPORT_ZONE);
        LocalDate resolvedTo = to != null ? to : today;
        LocalDate resolvedFrom = from != null ? from : defaultFrom(resolvedTo, resolvedGranularity);

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new DomainException("La date de début doit précéder la date de fin");
        }
        if (ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) > MAX_RANGE_DAYS) {
            throw new DomainException("La période analysée ne peut pas dépasser 10 ans");
        }

        long periodDays = ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1;
        LocalDate previousTo = resolvedFrom.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(periodDays - 1);

        List<Object[]> currentRows = orderRepository.findOrderTotalsBySupplierAndPeriod(
                supplierId, startOfDay(resolvedFrom), startOfDay(resolvedTo.plusDays(1)));
        List<Object[]> previousRows = orderRepository.findOrderTotalsBySupplierAndPeriod(
                supplierId, startOfDay(previousFrom), startOfDay(previousTo.plusDays(1)));

        Aggregation current = aggregate(currentRows, resolvedFrom, resolvedTo,
                resolvedGranularity, true);
        Aggregation previous = aggregate(previousRows, previousFrom, previousTo,
                resolvedGranularity, true);

        Map<String, Long> orderCountByStatus = new LinkedHashMap<>();
        current.statusCounts.forEach(orderCountByStatus::put);

        List<SupplierFinancialReportResponse.TopProduct> topProducts =
                buildTopProducts(orderRepository.topSoldProductsForSupplierBetween(
                        supplierId, startOfDay(resolvedFrom), startOfDay(resolvedTo.plusDays(1)), 10));

        SupplierFinancialReportResponse.PaymentSummary paymentSummary =
                paymentSummaryClient.getSupplierPaymentSummary(supplierId);

        return new SupplierFinancialReportResponse(
                resolvedFrom.format(DAY_FMT),
                resolvedTo.format(DAY_FMT),
                previousFrom.format(DAY_FMT),
                previousTo.format(DAY_FMT),
                resolvedGranularity.name(),
                current.summary,
                previous.summary,
                current.series,
                previous.series,
                orderCountByStatus,
                topProducts,
                paymentSummary);
    }

    private LocalDate defaultFrom(LocalDate to, ReportGranularity granularity) {
        return switch (granularity) {
            case DAY -> to.minusDays(29);
            case WEEK -> to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11);
            case MONTH -> to.minusMonths(11).withDayOfMonth(1);
            case YEAR -> to.minusYears(4).withDayOfYear(1);
        };
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(REPORT_ZONE).toInstant();
    }

    private Aggregation aggregate(List<Object[]> rows, LocalDate from, LocalDate to,
                                  ReportGranularity granularity, boolean withSeries) {
        Map<LocalDate, MutablePoint> buckets = withSeries
                ? emptyBuckets(from, to, granularity)
                : Map.of();
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        BigDecimal revenue = BigDecimal.ZERO;
        long orderCount = 0;
        long openOrders = 0;
        long deliveredOrders = 0;

        for (Object[] row : rows) {
            String status = row[2] != null ? row[2].toString() : "UNKNOWN";
            statusCounts.merge(status, 1L, Long::sum);
            if (EXCLUDED_FROM_REVENUE.contains(status)) {
                continue;
            }

            BigDecimal total = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            revenue = revenue.add(total);
            orderCount++;
            if (OPEN_STATUSES.contains(status)) {
                openOrders++;
            }
            if (DELIVERED_STATUSES.contains(status)) {
                deliveredOrders++;
            }

            if (withSeries) {
                LocalDate date = toLocalDate(row[0]);
                if (date != null) {
                    MutablePoint point = buckets.get(bucketStart(date, granularity));
                    if (point != null) {
                        point.revenue = point.revenue.add(total);
                        point.orderCount++;
                    }
                }
            }
        }

        BigDecimal averageBasket = orderCount > 0
                ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SupplierFinancialReportResponse.SeriesPoint> series = withSeries
                ? buckets.entrySet().stream()
                .map(e -> new SupplierFinancialReportResponse.SeriesPoint(
                        bucketKey(e.getKey(), granularity),
                        e.getValue().orderCount,
                        e.getValue().revenue))
                .toList()
                : List.of();

        return new Aggregation(
                new SupplierFinancialReportResponse.Summary(
                        revenue, orderCount, averageBasket, openOrders, deliveredOrders),
                series,
                statusCounts);
    }

    private Map<LocalDate, MutablePoint> emptyBuckets(LocalDate from, LocalDate to,
                                                      ReportGranularity granularity) {
        Map<LocalDate, MutablePoint> buckets = new LinkedHashMap<>();
        LocalDate cursor = bucketStart(from, granularity);
        LocalDate last = bucketStart(to, granularity);
        while (!cursor.isAfter(last)) {
            buckets.put(cursor, new MutablePoint());
            cursor = nextBucket(cursor, granularity);
        }
        return buckets;
    }

    private LocalDate bucketStart(LocalDate date, ReportGranularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private LocalDate nextBucket(LocalDate bucket, ReportGranularity granularity) {
        return switch (granularity) {
            case DAY -> bucket.plusDays(1);
            case WEEK -> bucket.plusWeeks(1);
            case MONTH -> bucket.plusMonths(1);
            case YEAR -> bucket.plusYears(1);
        };
    }

    private String bucketKey(LocalDate bucket, ReportGranularity granularity) {
        return switch (granularity) {
            case DAY, WEEK -> bucket.format(DAY_FMT);
            case MONTH -> bucket.format(MONTH_FMT);
            case YEAR -> bucket.format(YEAR_FMT);
        };
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant.atZone(REPORT_ZONE).toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(REPORT_ZONE).toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return null;
    }

    private List<SupplierFinancialReportResponse.TopProduct> buildTopProducts(List<Object[]> rows) {
        List<SupplierFinancialReportResponse.TopProduct> products = new ArrayList<>();
        for (Object[] row : rows) {
            String name = row[0] != null ? row[0].toString() : "";
            long qty = row[1] != null ? ((Number) row[1]).longValue() : 0;
            BigDecimal revenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            products.add(new SupplierFinancialReportResponse.TopProduct(name, qty, revenue));
        }
        return products;
    }

    private record Aggregation(
            SupplierFinancialReportResponse.Summary summary,
            List<SupplierFinancialReportResponse.SeriesPoint> series,
            Map<String, Long> statusCounts
    ) {}

    private static final class MutablePoint {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orderCount;
    }

    enum ReportGranularity {
        DAY, WEEK, MONTH, YEAR;

        static ReportGranularity parse(String value) {
            if (value == null || value.isBlank()) {
                return MONTH;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new DomainException("Granularité invalide : " + value);
            }
        }
    }
}
