package com.example.budgetbyte.ui.stats;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.budgetbyte.R;
import com.example.budgetbyte.data.entity.CategoryStat;
import com.example.budgetbyte.data.repository.ExpenseRepository;
import com.example.budgetbyte.databinding.FragmentStatsBinding;
import com.example.budgetbyte.ui.viewmodel.SelectedMonthViewModel;
import com.example.budgetbyte.ui.viewmodel.StatsViewModel;
import com.example.budgetbyte.util.CategoryUtils;
import com.example.budgetbyte.util.MoneyUtils;
import com.example.budgetbyte.util.PdfExporter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;
    private StatsViewModel viewModel;
    private SelectedMonthViewModel selectedMonthViewModel;
    private int initialPaddingTop = -1;
    private int initialPaddingBottom = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);
        selectedMonthViewModel = new ViewModelProvider(requireActivity()).get(SelectedMonthViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, insets) -> {
            if (initialPaddingTop == -1) {
                initialPaddingTop = root.getPaddingTop();
                initialPaddingBottom = root.getPaddingBottom();
            }
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int extraDp = dpToPx(16);
            root.setPaddingRelative(
                    extraDp,
                    statusBars.top + initialPaddingTop,
                    extraDp,
                    navBars.bottom + initialPaddingBottom
            );
            return insets;
        });

        binding.btnPrevMonth.setOnClickListener(v -> selectedMonthViewModel.shiftMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> selectedMonthViewModel.shiftMonth(1));

        selectedMonthViewModel.selectedMonth().observe(getViewLifecycleOwner(), month -> {
            binding.tvSelectedMonth.setText(formatMonthLabel(month.year, month.month));
            updatePeriodAndSubtitle(month.year, month.month);
            viewModel.loadForMonth(month.toYearMonthString());
        });

        viewModel.currentMonthSpending().observe(getViewLifecycleOwner(), this::renderCurrentMonthSummary);
        viewModel.categoryBreakdown().observe(getViewLifecycleOwner(), this::renderCategoryBreakdown);
        viewModel.monthlyTotals().observe(getViewLifecycleOwner(), this::renderMonthlyHistory);

        binding.btnDownloadReport.setOnClickListener(v -> {
            SelectedMonthViewModel.YearMonth ym = selectedMonthViewModel.selectedMonth().getValue();
            if (ym == null) {
                return;
            }
            binding.btnDownloadReport.setEnabled(false);
            viewModel.exportReport(requireContext(), ym.toYearMonthString(), new ExpenseRepository.ResultCallback<File>() {
                @Override
                public void onResult(File file) {
                    if (!isAdded()) return;
                    binding.btnDownloadReport.setEnabled(true);
                    if (file == null) {
                        Snackbar.make(binding.getRoot(), R.string.stats_export_empty, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    Snackbar.make(binding.getRoot(), R.string.stats_export_success, Snackbar.LENGTH_SHORT).show();
                    try {
                        Intent shareIntent = PdfExporter.createShareIntent(requireContext(), file);
                        startActivity(shareIntent);
                    } catch (Exception e) {
                        Snackbar.make(binding.getRoot(), R.string.stats_export_error, Snackbar.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception exception) {
                    if (!isAdded()) return;
                    binding.btnDownloadReport.setEnabled(true);
                    Snackbar.make(binding.getRoot(), R.string.stats_export_error, Snackbar.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updatePeriodAndSubtitle(int year, int month) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.clear();
        cal.set(year, month - 1, 1);
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        binding.tvMonthPeriod.setText(getString(R.string.stats_period_format, maxDays, monthName));
        binding.tvExportSubtitle.setText(getString(R.string.stats_export_subtitle_format, monthName));
    }

    private void renderCurrentMonthSummary(Long total) {
        long totalValue = total == null ? 0L : total;
        binding.tvMonthTotal.setText(MoneyUtils.formatNumber(totalValue));
    }

    private void renderCategoryBreakdown(List<CategoryStat> items) {
        binding.categoryLegend.removeAllViews();
        if (items == null || items.isEmpty()) {
            binding.categoryEmpty.setVisibility(View.VISIBLE);
            binding.categoryLegend.setVisibility(View.GONE);
            binding.pieChart.setVisibility(View.GONE);
            binding.topCategoryOverlay.setVisibility(View.GONE);
            binding.tvInsight.setText(getString(R.string.insight_empty));
            binding.layoutInsight2.setVisibility(View.GONE);
            binding.layoutInsight3.setVisibility(View.GONE);
            return;
        }

        long total = StatsAnalysisUtils.totalForSet(items);
        binding.categoryEmpty.setVisibility(View.GONE);
        binding.categoryLegend.setVisibility(View.VISIBLE);
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.layoutInsight2.setVisibility(View.VISIBLE);
        binding.layoutInsight3.setVisibility(View.VISIBLE);

        if (total <= 0L) {
            binding.pieChart.clear();
            binding.topCategoryOverlay.setVisibility(View.GONE);
            binding.tvInsight.setText(getString(R.string.insight_empty));
            return;
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int[] palette = new int[] {
                ContextCompat.getColor(requireContext(), R.color.primary_container),
                ContextCompat.getColor(requireContext(), R.color.secondary),
                ContextCompat.getColor(requireContext(), R.color.tertiary_container),
                ContextCompat.getColor(requireContext(), R.color.on_secondary_fixed),
                ContextCompat.getColor(requireContext(), R.color.outline),
                ContextCompat.getColor(requireContext(), R.color.primary),
                ContextCompat.getColor(requireContext(), R.color.secondary_container),
                ContextCompat.getColor(requireContext(), R.color.tertiary)
        };

        CategoryStat top = null;
        for (CategoryStat item : items) {
            if (item == null) {
                continue;
            }
            if (top == null || item.total > top.total) {
                top = item;
            }
            String localizedCategory = CategoryUtils.getLocalizedName(requireContext(), item.category);
            pieEntries.add(new PieEntry(item.total, localizedCategory));
            colors.add(palette[colors.size() % palette.length]);
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(colors);
        pieDataSet.setDrawValues(false);
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(4f);
        PieData pieData = new PieData(pieDataSet);

        PieChart pieChart = binding.pieChart;
        pieChart.setUsePercentValues(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(60f);
        pieChart.setTransparentCircleRadius(64f);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setDrawCenterText(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.setData(pieData);
        pieChart.invalidate();

        for (int i = 0; i < items.size(); i++) {
            CategoryStat item = items.get(i);
            if (item == null) {
                continue;
            }
            int percent = StatsAnalysisUtils.calculatePercent(item.total, total);
            String localizedCategory = CategoryUtils.getLocalizedName(requireContext(), item.category);

            LinearLayout row = new LinearLayout(requireContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(6), 0, dpToPx(6));

            View dot = new View(requireContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    dpToPx(10), dpToPx(10));
            dotParams.setMargins(0, 0, dpToPx(10), 0);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundColor(palette[i % palette.length]);
            row.addView(dot);

            TextView title = new TextView(requireContext());
            title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            title.setText(localizedCategory);
            title.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            title.setTextSize(14f);
            row.addView(title);

            TextView amount = new TextView(requireContext());
            amount.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            amount.setText(MoneyUtils.formatAmount(item.total, "EGP"));
            amount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            amount.setTextSize(12f);
            row.addView(amount);

            TextView percentView = new TextView(requireContext());
            LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            percentParams.setMargins(dpToPx(12), 0, 0, 0);
            percentView.setLayoutParams(percentParams);
            percentView.setText(String.format(Locale.getDefault(), "%d%%", percent));
            percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            percentView.setTextSize(13f);
            row.addView(percentView);

            binding.categoryLegend.addView(row);
        }

        if (top != null) {
            String topName = CategoryUtils.getLocalizedName(requireContext(), top.category);
            int topPercent = StatsAnalysisUtils.calculatePercent(top.total, total);
            binding.tvTopCategoryName.setText(topName);
            binding.tvTopCategoryPercent.setText(String.format(Locale.getDefault(), "%d%%", topPercent));
            binding.topCategoryOverlay.setVisibility(View.VISIBLE);

            String insight1 = getString(R.string.insight_category_format, topPercent, topName);
            binding.tvInsight.setText(insight1);
        } else {
            binding.topCategoryOverlay.setVisibility(View.GONE);
            binding.tvInsight.setText(getString(R.string.insight_empty));
        }
    }

    private void renderMonthlyHistory(List<CategoryStat> items) {
        if (items == null || items.isEmpty()) {
            binding.barChart.setVisibility(View.GONE);
            binding.barEmpty.setVisibility(View.VISIBLE);
            binding.comparisonBadge.setVisibility(View.GONE);
            return;
        }

        List<CategoryStat> sorted = StatsAnalysisUtils.limitLastSix(items);
        binding.barChart.setVisibility(View.VISIBLE);
        binding.barEmpty.setVisibility(View.GONE);

        if (sorted.isEmpty()) {
            binding.barChart.clear();
            binding.barEmpty.setVisibility(View.VISIBLE);
            binding.comparisonBadge.setVisibility(View.GONE);
            return;
        }

        long avg = StatsAnalysisUtils.calculateAverage(sorted);
        binding.tvAverageSpending.setText(getString(R.string.stats_average_format, MoneyUtils.formatAmount(avg, "EGP")));

        Integer comparison = StatsAnalysisUtils.calculateMonthlyComparison(sorted);
        if (comparison != null) {
            binding.comparisonBadge.setVisibility(View.VISIBLE);
            if (comparison > 0) {
                binding.tvMonthlyComparison.setText(getString(R.string.stats_comparison_increase, comparison));
                binding.ivComparisonTrend.setImageResource(R.drawable.ic_trending_up);
            } else if (comparison < 0) {
                binding.tvMonthlyComparison.setText(getString(R.string.stats_comparison_decrease, Math.abs(comparison)));
                binding.ivComparisonTrend.setImageResource(R.drawable.ic_trending_up);
            } else {
                binding.tvMonthlyComparison.setText(getString(R.string.stats_comparison_same));
                binding.ivComparisonTrend.setImageResource(R.drawable.ic_trending_up);
            }
        } else {
            binding.comparisonBadge.setVisibility(View.GONE);
        }

        List<BarEntry> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            CategoryStat item = sorted.get(i);
            if (item == null) {
                continue;
            }
            values.add(new BarEntry(i, item.total));
            labels.add(formatMonthLabel(item.category));
        }

        BarDataSet dataSet = new BarDataSet(values, "");
        List<Integer> paletteList = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            paletteList.add(i == sorted.size() - 1
                    ? ContextCompat.getColor(requireContext(), R.color.primary)
                    : ContextCompat.getColor(requireContext(), R.color.surface_container_high));
        }
        dataSet.setColors(paletteList);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        dataSet.setDrawValues(false);

        BarChart barChart = binding.barChart;
        barChart.setData(new BarData(dataSet));
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setTouchEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-25f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(Math.min(labels.size(), 6));
        xAxis.setAvoidFirstLastClipping(false);
        barChart.invalidate();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String formatMonthLabel(int year, int month) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.clear();
        calendar.set(year, month - 1, 1);
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    private String formatMonthLabel(String yearMonth) {
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            return "";
        }
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM", Locale.US).parse(yearMonth);
            if (parsed == null) {
                return yearMonth;
            }
            return new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(parsed);
        } catch (ParseException e) {
            return yearMonth;
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
