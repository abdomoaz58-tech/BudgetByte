package com.example.budgetbyte.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgetbyte.R;
import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.databinding.ItemExpenseBinding;
import com.example.budgetbyte.util.CategoryUtils;
import com.example.budgetbyte.util.MoneyUtils;

import java.util.Objects;

public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getAmount() == newItem.getAmount()
                            && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getDate(), newItem.getDate())
                            && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                            && Objects.equals(oldItem.getType(), newItem.getType());
                }
            };

    public ExpenseAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public Expense getExpenseAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExpenseBinding binding = ItemExpenseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ExpenseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final ItemExpenseBinding binding;

        public ExpenseViewHolder(@NonNull ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Expense expense, final OnItemClickListener listener) {
            Context context = binding.getRoot().getContext();
            boolean isIncome = expense.isIncome();

            binding.tvExpenseTitle.setText(expense.getTitle() != null ? expense.getTitle() : "");
            binding.tvExpenseDate.setText(expense.getDate() != null ? expense.getDate() : "");
            if (binding.tvExpenseCategory != null) {
                // Localized display mapping — converts stored English keys to localized string resources
                binding.tvExpenseCategory.setText(CategoryUtils.getLocalizedName(context, expense.getCategory()));
            }

            String amountText = MoneyUtils.formatAmount(expense.getAmount(), "EGP");
            binding.tvExpenseAmount.setText(context.getString(
                    isIncome ? R.string.amount_income_format : R.string.amount_expense_format,
                    amountText));
            binding.tvExpenseAmount.setTextColor(ContextCompat.getColor(context,
                    isIncome ? R.color.income : R.color.expense));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(expense);
                }
            });
        }
    }
}
