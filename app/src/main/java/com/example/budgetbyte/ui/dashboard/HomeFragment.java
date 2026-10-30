package com.example.budgetbyte.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgetbyte.R;
import com.example.budgetbyte.databinding.FragmentHomeBinding;
import com.example.budgetbyte.ui.common.ExpenseAdapter;
import com.example.budgetbyte.ui.viewmodel.HomeViewModel;
import com.example.budgetbyte.ui.viewmodel.SelectedMonthViewModel;
import com.example.budgetbyte.util.MoneyUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private SelectedMonthViewModel selectedMonthViewModel;
    private ExpenseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        selectedMonthViewModel = new ViewModelProvider(requireActivity()).get(SelectedMonthViewModel.class);
        adapter = new ExpenseAdapter();

        binding.rvRecentExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentExpenses.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            root.setPadding(statusBars.left + 16, statusBars.top + 16, statusBars.right + 16,
                    navBars.bottom + 16);
            return insets;
        });

        viewModel.recentExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses == null) {
                expenses = Collections.emptyList();
            }
            adapter.submitList(expenses);
            boolean empty = expenses.isEmpty();
            binding.rvRecentExpenses.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.balance().observe(getViewLifecycleOwner(), value -> {
            long amount = value == null ? 0L : value;
            binding.tvTotalBalance.setText(MoneyUtils.formatAmount(amount, "EGP"));
        });

        viewModel.monthlyIncome().observe(getViewLifecycleOwner(), value -> {
            long amount = value == null ? 0L : value;
            binding.chipIncome.setText(getString(R.string.income) + "\n" + MoneyUtils.formatAmount(amount, "EGP"));
        });

        viewModel.monthlyExpenses().observe(getViewLifecycleOwner(), value -> {
            long amount = value == null ? 0L : value;
            binding.chipExpense.setText(getString(R.string.expense) + "\n" + MoneyUtils.formatAmount(amount, "EGP"));
        });

        selectedMonthViewModel.selectedMonth().observe(getViewLifecycleOwner(), month -> {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.clear();
            calendar.set(month.year, month.month - 1, 1);
            binding.tvSelectedMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
            viewModel.loadForMonth(month.toYearMonthString());
        });

        binding.btnPrevMonth.setOnClickListener(v -> selectedMonthViewModel.shiftMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> selectedMonthViewModel.shiftMonth(1));

        binding.chipIncome.setOnClickListener(v -> openAddExpenseDialog("income"));
        binding.chipExpense.setOnClickListener(v -> openAddExpenseDialog("expense"));

        binding.fabAddExpense.setOnClickListener(v -> openAddExpenseDialog("expense"));

        binding.btnScanReceipt.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_ocrScannerFragment));

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    viewModel.delete(adapter.getExpenseAt(position));
                }
            }
        }).attachToRecyclerView(binding.rvRecentExpenses);

        binding.tvSelectedMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date()));
    }

    private void openAddExpenseDialog(String transactionType) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        String type = (transactionType == null || transactionType.trim().isEmpty()) ? "expense" : transactionType;
        args.putString("transactionType", type);
        args.putString("type", type);
        args.putString("title", "Receipt");
        args.putLong("amount", 0L);
        args.putString("category", "Uncategorized");
        args.putString("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        navController.navigate(R.id.action_global_addExpenseFragment, args);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
