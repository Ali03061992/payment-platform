package com.paymentplatform.organization.domain.engine;

import java.util.*;

/**
 * Demand forecasting engine with multiple methods.
 * Selects the best method based on data characteristics.
 */
public class DemandForecaster {

    public record ForecastResult(
        String method,
        double[] forecasts,
        double accuracy,
        double mape,
        double rmse,
        Map<String, Double> parameters,
        int trainingPeriods,
        String explanation
    ) {}

    private static final double ALPHA_DEFAULT = 0.3;
    private static final double BETA_DEFAULT = 0.1;
    private static final double GAMMA_DEFAULT = 0.3;

    /**
     * Analyze demand and select the best forecasting method.
     */
    public ForecastResult forecast(double[] historicalDemand, int horizon) {
        if (historicalDemand == null || historicalDemand.length < 3) {
            return noDataResult(historicalDemand != null ? historicalDemand.length : 0);
        }

        double cv = coefficientOfVariation(historicalDemand);
        boolean intermittent = isIntermittent(historicalDemand);
        boolean hasTrend = detectTrend(historicalDemand);
        boolean hasSeasonality = detectSeasonality(historicalDemand);

        List<ForecastCandidate> candidates = new ArrayList<>();

        // Always try SMA
        candidates.add(smaForecast(historicalDemand, horizon, 3));
        candidates.add(smaForecast(historicalDemand, horizon, 5));
        candidates.add(smaForecast(historicalDemand, horizon, 7));

        // Weighted MA
        candidates.add(wmaForecast(historicalDemand, horizon, 3));
        candidates.add(wmaForecast(historicalDemand, horizon, 5));

        // Exponential smoothing
        candidates.add(sesForecast(historicalDemand, horizon, 0.2));
        candidates.add(sesForecast(historicalDemand, horizon, 0.3));
        candidates.add(sesForecast(historicalDemand, horizon, 0.5));

        if (hasTrend) {
            candidates.add(holtForecast(historicalDemand, horizon, ALPHA_DEFAULT, BETA_DEFAULT));
            candidates.add(holtForecast(historicalDemand, horizon, 0.4, 0.1));
        }

        if (hasSeasonality && historicalDemand.length >= 14) {
            int period = detectSeasonalPeriod(historicalDemand);
            if (period > 0 && historicalDemand.length >= period * 2) {
                candidates.add(holtWintersForecast(historicalDemand, horizon, period, "additive"));
                candidates.add(holtWintersForecast(historicalDemand, horizon, period, "multiplicative"));
            }
        }

        if (intermittent) {
            candidates.add(crostonForecast(historicalDemand, horizon, 0.15));
            candidates.add(crostonForecast(historicalDemand, horizon, 0.3));
        }

        // Select best by MAPE (lowest)
        ForecastCandidate best = candidates.stream()
            .min(Comparator.comparingDouble(c -> c.mape))
            .orElse(candidates.get(0));

        return new ForecastResult(
            best.method,
            best.forecasts,
            Math.max(0, 100 - best.mape),
            best.mape,
            best.rmse,
            best.parameters,
            historicalDemand.length,
            buildExplanation(best, cv, intermittent, hasTrend, hasSeasonality)
        );
    }

    // === Simple Moving Average ===
    private ForecastCandidate smaForecast(double[] data, int horizon, int window) {
        double[] forecasts = new double[horizon];
        double[] errors = new double[data.length];

        for (int i = window; i < data.length; i++) {
            double sum = 0;
            for (int j = i - window; j < i; j++) sum += data[j];
            double forecast = sum / window;
            errors[i] = Math.abs(data[i] - forecast);
        }

        double lastSum = 0;
        for (int i = data.length - window; i < data.length; i++) lastSum += data[i];
        double lastAvg = lastSum / window;
        Arrays.fill(forecasts, lastAvg);

        double mape = computeMAPE(data, errors, window);
        double rmse = computeRMSE(data, window, lastAvg);

        Map<String, Double> params = new HashMap<>();
        params.put("window", (double) window);
        return new ForecastCandidate("SMA(" + window + ")", forecasts, mape, rmse, params);
    }

    // === Weighted Moving Average ===
    private ForecastCandidate wmaForecast(double[] data, int horizon, int window) {
        double[] forecasts = new double[horizon];
        double[] errors = new double[data.length];

        double[] weights = new double[window];
        double weightSum = 0;
        for (int i = 0; i < window; i++) {
            weights[i] = (i + 1);
            weightSum += weights[i];
        }

        for (int i = window; i < data.length; i++) {
            double sum = 0;
            for (int j = 0; j < window; j++) {
                sum += data[i - window + j] * weights[j];
            }
            double forecast = sum / weightSum;
            errors[i] = Math.abs(data[i] - forecast);
        }

        double lastSum = 0;
        for (int i = 0; i < window; i++) {
            lastSum += data[data.length - window + i] * weights[i];
        }
        double lastForecast = lastSum / weightSum;
        Arrays.fill(forecasts, lastForecast);

        double mape = computeMAPE(data, errors, window);
        double rmse = computeRMSE(data, window, lastForecast);

        Map<String, Double> params = new HashMap<>();
        params.put("window", (double) window);
        return new ForecastCandidate("WMA(" + window + ")", forecasts, mape, rmse, params);
    }

    // === Simple Exponential Smoothing ===
    private ForecastCandidate sesForecast(double[] data, int horizon, double alpha) {
        double[] forecasts = new double[horizon];
        double[] errors = new double[data.length];

        double level = data[0];
        errors[0] = 0;
        for (int i = 1; i < data.length; i++) {
            double prevLevel = level;
            level = alpha * data[i] + (1 - alpha) * prevLevel;
            errors[i] = Math.abs(data[i] - prevLevel);
        }

        Arrays.fill(forecasts, level);

        double mape = computeMAPE(data, errors, 1);
        double rmse = computeRMSE(data, 1, level);

        Map<String, Double> params = new HashMap<>();
        params.put("alpha", alpha);
        return new ForecastCandidate("SES(alpha=" + alpha + ")", forecasts, mape, rmse, params);
    }

    // === Holt's Linear Trend ===
    private ForecastCandidate holtForecast(double[] data, int horizon, double alpha, double beta) {
        double[] forecasts = new double[horizon];
        double[] errors = new double[data.length];

        double level = data[0];
        double trend = data.length > 1 ? data[1] - data[0] : 0;

        errors[0] = 0;
        for (int i = 1; i < data.length; i++) {
            double prevLevel = level;
            double prevTrend = trend;
            level = alpha * data[i] + (1 - alpha) * (prevLevel + prevTrend);
            trend = beta * (level - prevLevel) + (1 - beta) * prevTrend;
            double f = prevLevel + prevTrend;
            errors[i] = Math.abs(data[i] - f);
        }

        for (int h = 0; h < horizon; h++) {
            forecasts[h] = level + (h + 1) * trend;
        }

        double mape = computeMAPE(data, errors, 1);
        double rmse = computeRMSE(data, 1, level + trend);

        Map<String, Double> params = new HashMap<>();
        params.put("alpha", alpha);
        params.put("beta", beta);
        return new ForecastCandidate("Holt", forecasts, mape, rmse, params);
    }

    // === Holt-Winters ===
    private ForecastCandidate holtWintersForecast(double[] data, int horizon, int period, String type) {
        int n = data.length;
        if (n < period * 2) return sesForecast(data, horizon, 0.3);

        double[] forecasts = new double[horizon];
        double[] errors = new double[n];

        double alpha = 0.3, beta = 0.1, gamma = 0.3;
        double level = average(data, 0, period);
        double trend = (average(data, period, period * 2) - average(data, 0, period)) / period;
        double[] seasonal = new double[period];

        if ("multiplicative".equals(type)) {
            for (int i = 0; i < period; i++) {
                seasonal[i] = data[i] / (level + trend * (i + 1));
            }
        } else {
            for (int i = 0; i < period; i++) {
                seasonal[i] = data[i] - (level + trend * (i + 1));
            }
        }

        for (int i = period; i < n; i++) {
            double prevLevel = level;
            double prevTrend = trend;
            int sIdx = i % period;

            double seasonalComponent;
            if ("multiplicative".equals(type)) {
                seasonalComponent = data[i] / (prevLevel + prevTrend + seasonal[sIdx]);
                level = alpha * (data[i] / seasonal[sIdx]) + (1 - alpha) * (prevLevel + prevTrend);
                trend = beta * (level - prevLevel) + (1 - beta) * prevTrend;
                seasonal[sIdx] = gamma * (data[i] / level) + (1 - gamma) * seasonal[sIdx];
            } else {
                seasonalComponent = data[i] - (prevLevel + prevTrend + seasonal[sIdx]);
                level = alpha * (data[i] - seasonal[sIdx]) + (1 - alpha) * (prevLevel + prevTrend);
                trend = beta * (level - prevLevel) + (1 - beta) * prevTrend;
                seasonal[sIdx] = gamma * (data[i] - level) + (1 - gamma) * seasonal[sIdx];
            }

            double f = prevLevel + prevTrend + seasonal[sIdx];
            errors[i] = Math.abs(data[i] - f);
        }

        for (int h = 0; h < horizon; h++) {
            int sIdx = (n + h) % period;
            if ("multiplicative".equals(type)) {
                forecasts[h] = (level + (h + 1) * trend) * seasonal[sIdx];
            } else {
                forecasts[h] = level + (h + 1) * trend + seasonal[sIdx];
            }
            if (forecasts[h] < 0) forecasts[h] = 0;
        }

        double mape = computeMAPE(data, errors, period * 2);
        double rmse = computeRMSE(data, period * 2, level + trend);

        Map<String, Double> params = new HashMap<>();
        params.put("alpha", alpha);
        params.put("beta", beta);
        params.put("gamma", gamma);
        params.put("period", (double) period);
        return new ForecastCandidate("Holt-Winters(" + type + ",p=" + period + ")", forecasts, mape, rmse, params);
    }

    // === Croston's Method (intermittent demand) ===
    private ForecastCandidate crostonForecast(double[] data, int horizon, double alpha) {
        double[] forecasts = new double[horizon];
        double[] errors = new double[data.length];

        List<Integer> intervals = new ArrayList<>();
        List<Double> demands = new ArrayList<>();

        int lastIdx = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] > 0) {
                intervals.add(lastIdx < 0 ? i + 1 : i - lastIdx);
                demands.add(data[i]);
                lastIdx = i;
            }
        }

        if (demands.isEmpty()) {
            Arrays.fill(forecasts, 0);
            return new ForecastCandidate("Croston", forecasts, 100, 0, Map.of("alpha", alpha));
        }

        double avgInterval = intervals.stream().mapToInt(Integer::intValue).average().orElse(1);
        double avgDemand = demands.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double z = avgInterval;
        double q = avgDemand;

        for (int i = 0; i < intervals.size(); i++) {
            double prevZ = z;
            double prevQ = q;
            z = alpha * intervals.get(i) + (1 - alpha) * prevZ;
            q = alpha * demands.get(i) + (1 - alpha) * prevQ;
        }

        double forecast = q / Math.max(z, 0.01);
        Arrays.fill(forecasts, forecast);

        for (int i = 0; i < data.length; i++) {
            errors[i] = Math.abs(data[i] - forecast);
        }

        double mape = computeMAPE(data, errors, 0);
        double rmse = computeRMSE(data, 0, forecast);

        Map<String, Double> params = new HashMap<>();
        params.put("alpha", alpha);
        params.put("avgInterval", avgInterval);
        params.put("avgDemand", avgDemand);
        return new ForecastCandidate("Croston", forecasts, mape, rmse, params);
    }

    // === Helper methods ===

    public static double coefficientOfVariation(double[] data) {
        if (data == null || data.length < 2) return 0;
        double mean = Arrays.stream(data).average().orElse(0);
        if (mean == 0) return 0;
        double variance = 0;
        for (double v : data) variance += (v - mean) * (v - mean);
        variance /= (data.length - 1);
        return Math.sqrt(variance) / mean;
    }

    public static boolean isIntermittent(double[] data) {
        if (data == null || data.length < 3) return false;
        long zeroCount = Arrays.stream(data).filter(v -> v <= 0).count();
        return (double) zeroCount / data.length > 0.5;
    }

    public static boolean detectTrend(double[] data) {
        if (data == null || data.length < 4) return false;
        int n = data.length;
        int half = n / 2;
        double firstHalf = average(data, 0, half);
        double secondHalf = average(data, half, n);
        double change = Math.abs(secondHalf - firstHalf);
        double avgFirst = firstHalf > 0 ? firstHalf : 1;
        return change / avgFirst > 0.15;
    }

    public static boolean detectSeasonality(double[] data) {
        if (data == null || data.length < 14) return false;
        int period = detectSeasonalPeriod(data);
        if (period <= 0 || period >= data.length / 2) return false;

        double[] seasonalAvg = new double[period];
        int[] counts = new int[period];
        for (int i = 0; i < data.length; i++) {
            seasonalAvg[i % period] += data[i];
            counts[i % period]++;
        }
        for (int i = 0; i < period; i++) seasonalAvg[i] /= counts[i];

        double grandAvg = Arrays.stream(data).average().orElse(0);
        if (grandAvg == 0) return false;

        double seasonalVariance = 0;
        for (double v : seasonalAvg) seasonalVariance += (v - grandAvg) * (v - grandAvg);
        seasonalVariance /= period;

        double totalVariance = 0;
        for (double v : data) totalVariance += (v - grandAvg) * (v - grandAvg);
        totalVariance /= data.length;

        return totalVariance > 0 && seasonalVariance / totalVariance > 0.3;
    }

    public static int detectSeasonalPeriod(double[] data) {
        if (data == null || data.length < 7) return 0;
        int bestPeriod = 0;
        double bestScore = 0;
        for (int p = 2; p <= Math.min(data.length / 2, 12); p++) {
            double[] seasonalAvg = new double[p];
            int[] counts = new int[p];
            for (int i = 0; i < data.length; i++) {
                seasonalAvg[i % p] += data[i];
                counts[i % p]++;
            }
            for (int i = 0; i < p; i++) seasonalAvg[i] /= counts[i];

            double grandAvg = Arrays.stream(data).average().orElse(0);
            if (grandAvg == 0) continue;

            double seasonalVar = 0;
            for (double v : seasonalAvg) seasonalVar += (v - grandAvg) * (v - grandAvg);
            seasonalVar /= p;

            double totalVar = 0;
            for (double v : data) totalVar += (v - grandAvg) * (v - grandAvg);
            totalVar /= data.length;

            double score = totalVar > 0 ? seasonalVar / totalVar : 0;
            if (score > bestScore) {
                bestScore = score;
                bestPeriod = p;
            }
        }
        return bestScore > 0.3 ? bestPeriod : 0;
    }

    private static double average(double[] data, int from, int to) {
        double sum = 0;
        for (int i = from; i < to && i < data.length; i++) sum += data[i];
        return sum / Math.max(1, to - from);
    }

    private double computeMAPE(double[] data, double[] errors, int from) {
        double sum = 0;
        int count = 0;
        for (int i = from; i < data.length; i++) {
            if (data[i] > 0) {
                sum += errors[i] / data[i];
                count++;
            }
        }
        return count > 0 ? (sum / count) * 100 : 100;
    }

    private double computeRMSE(double[] data, int from, double forecast) {
        double sum = 0;
        int count = 0;
        for (int i = from; i < data.length; i++) {
            sum += (data[i] - forecast) * (data[i] - forecast);
            count++;
        }
        return count > 0 ? Math.sqrt(sum / count) : 0;
    }

    private String buildExplanation(ForecastCandidate best, double cv, boolean intermittent, boolean trend, boolean seasonal) {
        StringBuilder sb = new StringBuilder();
        sb.append("Method selected: ").append(best.method);
        sb.append(" | CV=").append(String.format("%.3f", cv));

        if (intermittent) sb.append(" | Demand is intermittent -> Croston method");
        else if (seasonal) sb.append(" | Seasonality detected -> Holt-Winters");
        else if (trend) sb.append(" | Trend detected -> Holt");
        else sb.append(" | Stable demand -> ")

            .append(best.method.startsWith("SES") ? "Exponential smoothing" : "Moving average");

        sb.append(" | MAPE=").append(String.format("%.2f%%", best.mape));
        return sb.toString();
    }

    private ForecastResult noDataResult(int dataLen) {
        return new ForecastResult(
            "N/A", new double[0], 0, 100, 0,
            Map.of(), dataLen,
            dataLen < 3 ? "Insufficient data for forecasting (need >= 3 periods)" : "No data available"
        );
    }

    private record ForecastCandidate(String method, double[] forecasts, double mape, double rmse, Map<String, Double> parameters) {}
}
