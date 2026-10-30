package com.example.budgetbyte.ui.history;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgetbyte.R;
import com.example.budgetbyte.databinding.FragmentTransactionsBinding;
import com.example.budgetbyte.ui.common.ExpenseAdapter;
import com.example.budgetbyte.ui.viewmodel.TransactionsViewModel;
import com.google.android.material.button.MaterialButton;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private ExpenseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        adapter = new ExpenseAdapter();

        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransactions.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            root.setPadding(statusBars.left + 12, statusBars.top + 12, statusBars.right + 12,
                    navBars.bottom + 12);
            return insets;
        });

        adapter.setOnItemClickListener(expense -> {
            Bundle args = new Bundle();
            args.putLong("expenseId", expense.getId());
            args.putString("transactionType", expense.getType() == null || expense.getType().trim().isEmpty() ? "expense" : expense.getType());
            args.putString("type", args.getString("transactionType"));
            args.putString("title", expense.getTitle());
            args.putLong("amount", expense.getAmount());
            args.putString("category", expense.getCategory());
            args.putString("date", expense.getDate());
            NavHostFragment.findNavController(this).navigate(R.id.action_global_addExpenseFragment, args);
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setFilter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        binding.btnFilterAll.setOnClickListener(v -> viewModel.setTypeFilter("all"));
        binding.btnFilterIncome.setOnClickListener(v -> viewModel.setTypeFilter("income"));
        binding.btnFilterExpense.setOnClickListener(v -> viewModel.setTypeFilter("expense"));

        viewModel.expenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.submitList(expenses);
            boolean empty = expenses == null || expenses.isEmpty();
            binding.tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.typeFilter().observe(getViewLifecycleOwner(), this::renderTypeButtons);

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
        }).attachToRecyclerView(binding.rvTransactions);
    }

    private void renderTypeButtons(String selectedType) {
        String type = selectedType == null || selectedType.trim().isEmpty() ? "all" : selectedType.trim().toLowerCase();
        setFilterButtonSelected(binding.btnFilterAll, "all".equals(type));
        setFilterButtonSelected(binding.btnFilterIncome, "income".equals(type));
        setFilterButtonSelected(binding.btnFilterExpense, "expense".equals(type));
    }

    private void setFilterButtonSelected(MaterialButton button, boolean selected) {
        button.setSelected(selected);
        button.setAlpha(selected ? 1f : 0.7f);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selected ? 0xFF003FB1 : 0xFFE9ECFF));
        button.setTextColor(selected ? 0xFFFFFFFF : 0xFF434654);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
