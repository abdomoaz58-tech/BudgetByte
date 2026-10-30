package com.example.budgetbyte.ui.budget;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.budgetbyte.R;
import com.example.budgetbyte.data.entity.CategoryStat;
import com.example.budgetbyte.databinding.FragmentBudgetBinding;
import com.example.budgetbyte.ui.viewmodel.BudgetViewModel;
import com.example.budgetbyte.ui.viewmodel.SelectedMonthViewModel;
import com.example.budgetbyte.util.CategoryUtils;
import com.example.budgetbyte.util.MoneyUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetFragment extends Fragment {
    private FragmentBudgetBinding binding;
    private BudgetViewModel viewModel;
    private SelectedMonthViewModel selectedMonthViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        selectedMonthViewModel = new ViewModelProvider(requireActivity()).get(SelectedMonthViewModel.class);

        binding.btnPrevMonth.setOnClickListener(v -> selectedMonthViewModel.shiftMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> selectedMonthViewModel.shiftMonth(1));

        binding.btnQuickEdit.setOnClickListener(v -> showBudgetEditor());
        binding.btnEditBudgetLimit.setOnClickListener(v -> showBudgetEditor());

        selectedMonthViewModel.selectedMonth().observe(getViewLifecycleOwner(), month -> {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.clear();
            calendar.set(month.year, month.month - 1, 1);
            binding.tvSelectedMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
            String monthKey = month.toYearMonthString();
            viewModel.loadForMonth(monthKey);
            renderBudgetSummary(monthKey, getCurrentMonthSpending());
        });

        viewModel.monthlySpending().observe(getViewLifecycleOwner(), spending -> {
            String monthKey = selectedMonthViewModel.getSelectedMonthValue().toYearMonthString();
            renderBudgetSummary(monthKey, spending == null ? 0L : spending);
        });

        viewModel.categories().observe(getViewLifecycleOwner(), this::renderCategoryBreakdown);
    }

    private void showBudgetEditor() {
        String monthKey = selectedMonthViewModel.getSelectedMonthValue().toYearMonthString();
        long currentBudget = viewModel.getBudgetForMonth(monthKey);
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentBudget));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("تعيين سقف الميزانية")
                .setView(input)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    try {
                        long value = Long.parseLong(input.getText().toString().trim());
                        viewModel.setBudgetForMonth(monthKey, value);
                        renderBudgetSummary(monthKey, getCurrentMonthSpending());
                    } catch (NumberFormatException ignored) {
                        // Ignore invalid budget inputs.
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private long getCurrentMonthSpending() {
        Long spending = viewModel.monthlySpending().getValue();
        return spending == null ? 0L : spending;
    }

    private void renderBudgetSummary(String monthKey, long spending) {
        long budget = viewModel.getBudgetForMonth(monthKey);
        long remaining = Math.max(0L, budget - spending);
        int percent = budget > 0 ? (int) Math.min(100, Math.round((spending * 100d) / budget)) : 0;

        binding.tvBudgetSummary.setText(getString(R.string.budget_monthly_limit, MoneyUtils.formatAmount(budget, "EGP")));
        binding.tvBudgetAmount.setText(MoneyUtils.formatAmount(budget, "EGP"));
        binding.tvBudgetSpent.setText(getString(R.string.budget_spent, MoneyUtils.formatAmount(spending, "EGP")));
        binding.tvBudgetProgressPercent.setText(percent + "%");
        binding.progressMainBudget.setProgress(percent);
        binding.tvBudgetLimit.setText(getString(R.string.budget_limit_total, MoneyUtils.formatAmount(budget, "EGP")));
        binding.tvBudgetRemaining.setText(getString(R.string.budget_remaining, MoneyUtils.formatAmount(remaining, "EGP")));

        // Dynamic Alert Banner
        if (binding.alertBanner != null) {
            if (budget > 0 && spending >= budget) {
                binding.alertBanner.setVisibility(View.VISIBLE);
                if (binding.tvAlertMessage != null) {
                    binding.tvAlertMessage.setText(getString(R.string.budget_alert_exceeded,
                            MoneyUtils.formatAmount(spending - budget, "EGP")));
                }
            } else if (budget > 0 && percent >= 80) {
                binding.alertBanner.setVisibility(View.VISIBLE);
                if (binding.tvAlertMessage != null) {
                    binding.tvAlertMessage.setText(getString(R.string.budget_alert_warning, percent));
                }
            } else {
                binding.alertBanner.setVisibility(View.GONE);
            }
        }

        // Dynamic Daily Allowed Rate
        if (binding.tvDailyAllowedRate != null) {
            Calendar now = Calendar.getInstance();
            int totalDays = now.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            int remainingDays = Math.max(1, totalDays - currentDay + 1);
            long dailyAllowed = remaining / remainingDays;
            binding.tvDailyAllowedRate.setText(MoneyUtils.formatAmount(dailyAllowed, "EGP"));
        }

        // Dynamic Projected Spending
        if (binding.tvProjectedSpending != null) {
            Calendar now = Calendar.getInstance();
            int totalDays = now.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentDay = Math.max(1, now.get(Calendar.DAY_OF_MONTH));
            long projected = Math.round((spending / (double) currentDay) * totalDays);
            binding.tvProjectedSpending.setText(MoneyUtils.formatAmount(projected, "EGP"));
            if (binding.tvProjectedStatus != null) {
                if (budget > 0 && projected > budget) {
                    binding.tvProjectedStatus.setText(getString(R.string.budget_projected_over));
                    binding.tvProjectedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
                } else {
                    binding.tvProjectedStatus.setText(getString(R.string.budget_projected_good));
                    binding.tvProjectedStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tertiary));
                }
            }
        }
    }

    private void renderCategoryBreakdown(List<CategoryStat> items) {
        binding.budgetCategoryList.removeAllViews();

        int count = (items == null) ? 0 : items.size();
        if (binding.tvCategoryCount != null) {
            binding.tvCategoryCount.setText(getString(R.string.budget_category_count, count));
        }

        long total = 0L;
        if (items != null) {
            for (CategoryStat item : items) {
                if (item != null) {
                    total += item.total;
                }
            }
        }

        if (items == null || items.isEmpty()) {
            binding.budgetCategoryEmpty.setVisibility(View.VISIBLE);
            binding.budgetCategoryList.setVisibility(View.GONE);
            return;
        }

        binding.budgetCategoryEmpty.setVisibility(View.GONE);
        binding.budgetCategoryList.setVisibility(View.VISIBLE);

        long grandTotal = total > 0 ? total : 1L;
        for (CategoryStat item : items) {
            if (item == null || item.category == null || item.category.trim().isEmpty()) {
                continue;
            }

            long amount = item.total;
            int percent = (int) Math.round((amount * 100d) / grandTotal);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_budget_category, binding.budgetCategoryList, false);

            TextView title = row.findViewById(R.id.tvBudgetCategoryName);
            TextView value = row.findViewById(R.id.tvBudgetCategoryAmount);
            TextView pct = row.findViewById(R.id.tvBudgetCategoryPercent);
            ProgressBar progressBar = row.findViewById(R.id.progressBudgetCategory);
            View dot = row.findViewById(R.id.viewBudgetCategoryDot);

            title.setText(CategoryUtils.getLocalizedName(requireContext(), item.category));
            value.setText(MoneyUtils.formatAmount(amount, "EGP"));
            pct.setText(String.format(Locale.getDefault(), "%d%%", percent));
            progressBar.setProgress(Math.min(100, percent));
            dot.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));

            binding.budgetCategoryList.addView(row);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
