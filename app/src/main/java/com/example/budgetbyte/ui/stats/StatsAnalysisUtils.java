package com.example.budgetbyte.ui.stats;

import com.example.budgetbyte.data.entity.CategoryStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StatsAnalysisUtils {
    private StatsAnalysisUtils() {
    }

    public static List<CategoryStat> sortMonthlyTotals(List<CategoryStat> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<CategoryStat> copied = new ArrayList<>(input);
        copied.removeIf(item -> item == null || item.category == null || item.category.trim().isEmpty());
        copied.sort(Comparator.comparingInt(item -> parseYearMonth(item.category)));
        return copied;
    }

    public static List<CategoryStat> limitLastSix(List<CategoryStat> input) {
        List<CategoryStat> sorted = sortMonthlyTotals(input);
        if (sorted.size() <= 6) {
            return sorted;
        }
        return sorted.subList(sorted.size() - 6, sorted.size());
    }

    public static int calculatePercent(long value, long total) {
        if (total <= 0L || value <= 0L) {
            return 0;
        }
        return (int) Math.round((value * 100.0d) / total);
    }

    public static String buildCategoryInsight(String category, long categoryTotal, long totalSpending) {
        if (totalSpending <= 0L || categoryTotal <= 0L) {
            return "";
        }
        int percent = calculatePercent(categoryTotal, totalSpending);
        String name = (category == null || category.trim().isEmpty()) ? "Other" : category;
        return String.format(Locale.getDefault(), "%1$d%% of this month's spending was on %2$s",
                percent, name);
    }

    public static long totalForSet(List<CategoryStat> items) {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CategoryStat item : items) {
            if (item != null) {
                total += item.total;
            }
        }
        return total;
    }

    public static long calculateAverage(List<CategoryStat> items) {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        int count = 0;
        for (CategoryStat item : items) {
            if (item != null && item.total > 0L) {
                total += item.total;
                count++;
            }
        }
        return count == 0 ? 0L : total / count;
    }

    public static Integer calculateMonthlyComparison(List<CategoryStat> sortedHistory) {
        if (sortedHistory == null || sortedHistory.size() < 2) {
            return null;
        }
        CategoryStat current = sortedHistory.get(sortedHistory.size() - 1);
        CategoryStat previous = sortedHistory.get(sortedHistory.size() - 2);
        if (current == null || previous == null || previous.total <= 0L) {
            return null;
        }
        long diff = current.total - previous.total;
        return (int) Math.round((diff * 100.0d) / previous.total);
    }

    private static int parseYearMonth(String value) {
        if (value == null || value.length() < 7 || value.charAt(4) != '-') {
            return Integer.MAX_VALUE;
        }
        try {
            String yearPart = value.substring(0, 4);
            String monthPart = value.substring(5, 7);
            int year = Integer.parseInt(yearPart);
            int month = Integer.parseInt(monthPart);
            return year * 12 + month;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
