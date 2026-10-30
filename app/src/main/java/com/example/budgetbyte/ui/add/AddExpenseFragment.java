package com.example.budgetbyte.ui.add;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.budgetbyte.R;
import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.data.repository.ExpenseRepository;
import com.example.budgetbyte.databinding.FragmentAddExpenseBinding;
import com.example.budgetbyte.ui.viewmodel.AddExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import com.example.budgetbyte.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class AddExpenseFragment extends Fragment {
    private static final String ARG_TYPE = "transactionType";
    private static final String ARG_ALT_TYPE = "type";
    private static final String ARG_ID = "expenseId";
    private static final String ARG_TITLE = "title";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_DATE = "date";
    private static final String OCR_READY = "addExpense.ocrDraftReady";
    private static final String OCR_TITLE = "addExpense.ocrTitle";
    private static final String OCR_AMOUNT = "addExpense.ocrAmount";
    private static final String OCR_CATEGORY = "addExpense.ocrCategory";
    private static final String OCR_DATE = "addExpense.ocrDate";
    private static final String OCR_TYPE = "addExpense.ocrType";
    private static final long MAX_AMOUNT = 999999999900L;

    private static final List<CategoryOption> EXPENSE_CATEGORIES = Arrays.asList(
            new CategoryOption(R.id.categoryFood, "Food", R.string.category_food, R.drawable.ic_restaurant),
            new CategoryOption(R.id.categoryTransport, "Transportation", R.string.category_transportation, R.drawable.ic_directions_car),
            new CategoryOption(R.id.categoryGroceries, "Groceries", R.string.category_groceries, R.drawable.ic_shopping_basket),
            new CategoryOption(R.id.categoryBills, "Bills", R.string.category_bills, R.drawable.ic_receipt),
            new CategoryOption(R.id.categoryHealth, "Health", R.string.category_health, R.drawable.ic_health_and_safety),
            new CategoryOption(R.id.categoryShopping, "Shopping", R.string.category_shopping, R.drawable.ic_shopping_cart, "تسوق"),
            new CategoryOption(R.id.categoryEntertainment, "Entertainment", R.string.category_entertainment, R.drawable.ic_auto_awesome),
            new CategoryOption(R.id.categoryEducation, "Education", R.string.category_education, R.drawable.ic_school),
            new CategoryOption(R.id.categoryOther, "Other", R.string.category_other, R.drawable.ic_tune));

    private static final List<CategoryOption> INCOME_CATEGORIES = Arrays.asList(
            new CategoryOption(R.id.categoryFood, "Salary", R.string.category_salary, R.drawable.ic_payments),
            new CategoryOption(R.id.categoryTransport, "Freelance", R.string.category_freelance, R.drawable.ic_person, "عمل حر"),
            new CategoryOption(R.id.categoryGroceries, "Gift", R.string.category_gift, R.drawable.ic_auto_awesome),
            new CategoryOption(R.id.categoryOther, "Other", R.string.category_other, R.drawable.ic_tune));

    private FragmentAddExpenseBinding binding;
    private AddExpenseViewModel model;
    private ExpenseRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddExpenseBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(this,
                new SavedStateViewModelFactory(requireActivity().getApplication(), this))
                .get(AddExpenseViewModel.class);
        repository = new ExpenseRepository(requireContext());

        if (savedInstanceState == null) {
            initializeFromArguments(getArguments() == null ? new Bundle() : getArguments());
        }

        binding.paymentContainer.setVisibility(View.GONE);
        binding.tvPaymentLabel.setVisibility(View.GONE);
        binding.notesCard.setVisibility(View.GONE);

        binding.btnBack.setOnClickListener(ignored -> navigateUp());
        binding.btnClose.setOnClickListener(ignored -> navigateUp());
        binding.btnExpense.setOnClickListener(ignored -> setType(AddExpenseViewModel.TYPE_EXPENSE));
        binding.btnIncome.setOnClickListener(ignored -> setType(AddExpenseViewModel.TYPE_INCOME));
        binding.dateCard.setOnClickListener(ignored -> showDatePicker());
        binding.tvChangeDate.setOnClickListener(ignored -> showDatePicker());
        binding.btnPlus50.setOnClickListener(ignored -> addQuickAmount(5000L));
        binding.btnPlus100.setOnClickListener(ignored -> addQuickAmount(10000L));
        binding.btnPlus200.setOnClickListener(ignored -> addQuickAmount(20000L));
        binding.btnPlus500.setOnClickListener(ignored -> addQuickAmount(50000L));
        binding.btnSave.setOnClickListener(ignored -> save(false));
        binding.btnSaveAndAddAnother.setOnClickListener(ignored -> save(true));
        binding.btnScanReceipt.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.ocrScannerFragment));

        binding.etTitle.addTextChangedListener(watch(text -> {
            model.setTitle(text);
            binding.titleInputLayout.setError(null);
        }));
        binding.etAmount.addTextChangedListener(watch(text -> {
            model.setAmount(text);
            binding.etAmount.setError(null);
        }));
        for (int cardId : allCardIds()) {
            binding.getRoot().findViewById(cardId).setOnClickListener(clicked -> selectCategory(clicked.getId()));
        }

        model.saving().observe(getViewLifecycleOwner(), inFlight -> {
            boolean enabled = !Boolean.TRUE.equals(inFlight);
            binding.btnSave.setEnabled(enabled);
            binding.btnSaveAndAddAnother.setEnabled(enabled);
        });
        model.saveResult().observe(getViewLifecycleOwner(), this::onSaveResult);
        observeOcrDraft();

        render();
        applyInsets();
        observeEditedExpense();
    }

    private void initializeFromArguments(Bundle args) {
        String type = args.getString(ARG_TYPE, args.getString(ARG_ALT_TYPE, model.getTransactionType()));
        model.setTransactionType(AddExpenseViewModel.TYPE_INCOME.equals(type)
                ? AddExpenseViewModel.TYPE_INCOME : AddExpenseViewModel.TYPE_EXPENSE);
        model.setExpenseId(args.getLong(ARG_ID, model.getExpenseId()));
        if (args.containsKey(ARG_TITLE)) {
            model.setTitle(args.getString(ARG_TITLE, ""));
        }
        if (args.containsKey(ARG_AMOUNT)) {
            model.setAmount(formatMinorUnits(args.getLong(ARG_AMOUNT)));
        }
        if (args.containsKey(ARG_CATEGORY)) {
            model.setCategory(args.getString(ARG_CATEGORY, ""));
        }
        String date = args.getString(ARG_DATE, "");
        if (!TextUtils.isEmpty(date)) {
            model.setSelectedDate(date);
        }
    }

    private void observeEditedExpense() {
        long expenseId = model.getExpenseId();
        if (expenseId <= 0L || model.isEditLoaded()) {
            return;
        }
        repository.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses == null || model.isEditLoaded()) {
                return;
            }
            for (Expense expense : expenses) {
                if (expense.id == model.getExpenseId()) {
                    model.setTransactionType(AddExpenseViewModel.TYPE_INCOME.equals(expense.type)
                            ? AddExpenseViewModel.TYPE_INCOME : AddExpenseViewModel.TYPE_EXPENSE);
                    model.setTitle(expense.title);
                    model.setAmount(formatMinorUnits(expense.amount));
                    model.setCategory(expense.category);
                    model.setSelectedDate(expense.date);
                    model.setEditLoaded(true);
                    render();
                    break;
                }
            }
        });
    }

    private int[] allCardIds() {
        return new int[]{R.id.categoryFood, R.id.categoryTransport, R.id.categoryGroceries,
                R.id.categoryBills, R.id.categoryHealth, R.id.categoryShopping,
                R.id.categoryEntertainment, R.id.categoryEducation, R.id.categoryOther};
    }

    private List<CategoryOption> categories() {
        return AddExpenseViewModel.TYPE_INCOME.equals(model.getTransactionType())
                ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
    }

    private void setType(String type) {
        if (type.equals(model.getTransactionType())) {
            return;
        }
        model.setTransactionType(type);
        if (findCategory(model.getCategory()) == null) {
            model.setCategory("");
        }
        render();
    }

    private void selectCategory(int cardId) {
        CategoryOption option = findCategoryByCard(cardId);
        if (option == null) {
            return;
        }
        CategoryOption currentlySelected = findCategory(model.getCategory());
        if (currentlySelected != option) {
            model.setCategory(option.canonicalValue);
        }
        renderCategories();
    }

    @Nullable
    private CategoryOption findCategory(String storedValue) {
        if (TextUtils.isEmpty(storedValue)) {
            return null;
        }
        for (CategoryOption option : categories()) {
            if (option.matches(storedValue)) {
                return option;
            }
        }
        return null;
    }

    @Nullable
    private CategoryOption findCategoryByCard(int cardId) {
        for (CategoryOption option : categories()) {
            if (option.cardId == cardId) {
                return option;
            }
        }
        return null;
    }

    private void render() {
        setTextIfDifferent(binding.etTitle, model.getTitle());
        setTextIfDifferent(binding.etAmount, model.getAmount());
        binding.tvSelectedDate.setText(formatDisplayDate(model.getSelectedDate()));
        renderType();
        renderCategories();
    }

    private void renderType() {
        boolean income = AddExpenseViewModel.TYPE_INCOME.equals(model.getTransactionType());
        int activeColor = income ? R.color.primary_container : R.color.secondary;
        int activeTextColor = income ? R.color.on_primary : R.color.on_secondary;
        setTypeButton(binding.btnExpense, !income, activeColor, activeTextColor);
        setTypeButton(binding.btnIncome, income, activeColor, activeTextColor);
        binding.tvCurrencyCode.setTextColor(ContextCompat.getColor(requireContext(),
                income ? R.color.primary : R.color.secondary));
        long id = model.getExpenseId();
        binding.btnSave.setText(id > 0L ? R.string.save_changes
                : (income ? R.string.save_income : R.string.save_expense));
        binding.btnSaveAndAddAnother.setText(R.string.save_and_add_another);
    }

    private void setTypeButton(MaterialButton button, boolean selected, int selectedColor,
                               int selectedTextColor) {
        button.setSelected(selected);
        button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                selected ? selectedColor : R.color.surface_container));
        button.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? selectedTextColor : R.color.on_surface_variant));
        button.setIconTint(ContextCompat.getColorStateList(requireContext(),
                selected ? selectedTextColor : R.color.on_surface_variant));
    }

    private void renderCategories() {
        boolean income = AddExpenseViewModel.TYPE_INCOME.equals(model.getTransactionType());
        int selectedBackground = income ? R.color.primary_container : R.color.secondary;
        int selectedContent = income ? R.color.on_primary : R.color.on_secondary;
        for (int cardId : allCardIds()) {
            MaterialCardView card = binding.getRoot().findViewById(cardId);
            CategoryOption option = findCategoryByCard(cardId);
            card.setVisibility(option == null ? View.GONE : View.VISIBLE);
            if (option == null) {
                continue;
            }
            LinearLayout content = (LinearLayout) card.getChildAt(0);
            ImageView icon = (ImageView) content.getChildAt(0);
            TextView label = (TextView) content.getChildAt(1);
            boolean selected = option.matches(model.getCategory());
            int cardColor = selected ? selectedBackground : R.color.surface_container_lowest;
            card.setCheckable(true);
            card.setChecked(selected);
            card.setStrokeWidth(selected ? 3 : 1);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), cardColor));
            card.setStrokeColor(ContextCompat.getColor(requireContext(),
                    selected ? selectedBackground : R.color.outline_variant));
            icon.setImageResource(option.iconResource);
            icon.setImageTintList(ContextCompat.getColorStateList(requireContext(),
                    selected ? selectedContent : R.color.on_surface_variant));
            label.setText(option.labelResource);
            label.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? selectedContent : R.color.on_surface));
        }
        CategoryOption selected = findCategory(model.getCategory());
        if (selected != null) {
            binding.tvSelectedCategory.setText(selected.labelResource);
        } else if (TextUtils.isEmpty(model.getCategory())) {
            binding.tvSelectedCategory.setText(R.string.select_category);
        } else {
            binding.tvSelectedCategory.setText(model.getCategory());
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date)
                .setSelection(epochMillis(model.getSelectedDate()))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            model.setSelectedDate(utcFormat.format(new Date(selection)));
            binding.tvSelectedDate.setText(formatDisplayDate(model.getSelectedDate()));
        });
        picker.show(getParentFragmentManager(), "add_expense_date_picker");
    }

    private String formatMinorUnits(long amount) {
        return BigDecimal.valueOf(amount, 2).setScale(2).toPlainString();
    }

    private void addQuickAmount(long increment) {
        long current;
        try {
            String text = binding.etAmount.getText() == null ? "" : binding.etAmount.getText().toString().trim();
            current = text.isEmpty() ? 0L : MoneyUtils.parseMinorUnits(text);
        } catch (IllegalArgumentException exception) {
            binding.etAmount.setError(getString(R.string.valid_amount));
            return;
        }
        try {
            long updated = Math.addExact(current, increment);
            if (updated > MAX_AMOUNT) {
                throw new ArithmeticException("Amount exceeds the supported range");
            }
            binding.etAmount.setText(formatMinorUnits(updated));
            binding.etAmount.setSelection(binding.etAmount.length());
        } catch (ArithmeticException exception) {
            binding.etAmount.setError(getString(R.string.valid_amount));
        }
    }

    private void save(boolean addAnother) {
        String title = model.getTitle().trim();
        long amount;
        try {
            amount = MoneyUtils.parseMinorUnits(model.getAmount());
        } catch (IllegalArgumentException exception) {
            binding.etAmount.setError(getString(R.string.valid_amount));
            return;
        }
        if (TextUtils.isEmpty(title)) {
            binding.titleInputLayout.setError(getString(R.string.title_required));
            return;
        }
        if (amount <= 0L || amount > MAX_AMOUNT) {
            binding.etAmount.setError(getString(R.string.valid_amount));
            return;
        }
        if (findCategory(model.getCategory()) == null) {
            Snackbar.make(binding.getRoot(), R.string.category_required, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (!model.beginSave()) {
            return;
        }

        long expenseId = model.getExpenseId();
        boolean editing = expenseId > 0L;
        Expense expense = new Expense(title, amount, model.getCategory(),
                model.getTransactionType(), model.getSelectedDate());
        ExpenseRepository.WriteCallback callback = new ExpenseRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                model.completeSave(editing, addAnother);
            }

            @Override
            public void onError(Exception exception) {
                model.failSave();
            }
        };
        if (editing) {
            expense.id = expenseId;
            repository.update(expense, callback);
        } else {
            repository.insert(expense, callback);
        }
    }

    private void onSaveResult(@Nullable AddExpenseViewModel.SaveResult result) {
        if (result == null || binding == null) {
            return;
        }
        model.clearSaveResult();
        if (!result.success) {
            Snackbar.make(binding.getRoot(), R.string.transaction_save_failed, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (result.editing) {
            Snackbar.make(binding.getRoot(), R.string.transaction_updated, Snackbar.LENGTH_SHORT).show();
            navigateUp();
        } else if (result.addAnother) {
            model.resetValuesKeepingType();
            render();
            binding.etAmount.requestFocus();
            Snackbar.make(binding.getRoot(), R.string.transaction_saved, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(binding.getRoot(), R.string.transaction_saved, Snackbar.LENGTH_SHORT).show();
            navigateUp();
        }
    }

    private void observeOcrDraft() {
        NavController controller = NavHostFragment.findNavController(this);
        if (controller.getCurrentBackStackEntry() == null) {
            return;
        }
        SavedStateHandle result = controller.getCurrentBackStackEntry().getSavedStateHandle();
        result.getLiveData(OCR_READY, false).observe(getViewLifecycleOwner(), ready -> {
            if (!Boolean.TRUE.equals(ready)) {
                return;
            }
            String title = result.get(OCR_TITLE);
            Long amount = result.get(OCR_AMOUNT);
            String category = result.get(OCR_CATEGORY);
            String date = result.get(OCR_DATE);
            String type = result.get(OCR_TYPE);
            result.set(OCR_READY, false);
            result.remove(OCR_TITLE);
            result.remove(OCR_AMOUNT);
            result.remove(OCR_CATEGORY);
            result.remove(OCR_DATE);
            result.remove(OCR_TYPE);

            if (AddExpenseViewModel.TYPE_EXPENSE.equals(type)
                    || AddExpenseViewModel.TYPE_INCOME.equals(type)) {
                model.setTransactionType(type);
            }
            model.setTitle(title == null ? "" : title);
            model.setAmount(formatMinorUnits(amount == null ? 0L : amount));
            model.setCategory(findCategoryForType(category, model.getTransactionType()) == null
                    ? "" : category);
            if (isValidStoredDate(date)) {
                model.setSelectedDate(date);
            }
            render();
        });
    }

    @Nullable
    private CategoryOption findCategoryForType(String value, String type) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        List<CategoryOption> options = AddExpenseViewModel.TYPE_INCOME.equals(type)
                ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        for (CategoryOption option : options) {
            if (option.matches(value)) {
                return option;
            }
        }
        return null;
    }

    private boolean isValidStoredDate(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            Date parsed = format.parse(value);
            return parsed != null && value.equals(format.format(parsed));
        } catch (ParseException exception) {
            return false;
        }
    }

    private TextWatcher watch(java.util.function.Consumer<String> onTextChanged) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                onTextChanged.accept(text == null ? "" : text.toString());
            }
            @Override public void afterTextChanged(Editable editable) { }
        };
    }

    private void setTextIfDifferent(TextView view, String text) {
        String current = view.getText() == null ? "" : view.getText().toString();
        String next = text == null ? "" : text;
        if (!current.equals(next)) {
            view.setText(next);
        }
    }

    private long epochMillis(String date) {
        try {
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = utcFormat.parse(date);
            return parsed == null ? MaterialDatePicker.todayInUtcMilliseconds() : parsed.getTime();
        } catch (ParseException exception) {
            return MaterialDatePicker.todayInUtcMilliseconds();
        }
    }

    private String formatDisplayDate(String date) {
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            return parsed == null ? date : DateFormat.getDateInstance(DateFormat.MEDIUM,
                    Locale.getDefault()).format(parsed);
        } catch (ParseException exception) {
            return date;
        }
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()
                    | WindowInsetsCompat.Type.ime());
            binding.addExpenseScroll.setPadding(binding.addExpenseScroll.getPaddingLeft(),
                    binding.addExpenseScroll.getPaddingTop(), binding.addExpenseScroll.getPaddingRight(),
                    insets.bottom + 24);
            return windowInsets;
        });
    }

    private void navigateUp() {
        if (isAdded()) {
            NavHostFragment.findNavController(this).navigateUp();
        }
    }

    @Override
    public void onDestroyView() {
        if (repository != null) {
            repository.close();
            repository = null;
        }
        binding = null;
        super.onDestroyView();
    }

    private static final class CategoryOption {
        private final int cardId;
        private final String canonicalValue;
        private final int labelResource;
        private final int iconResource;
        private final List<String> historicalAliases;

        private CategoryOption(int cardId, String canonicalValue, int labelResource, int iconResource,
                               String... historicalAliases) {
            this.cardId = cardId;
            this.canonicalValue = canonicalValue;
            this.labelResource = labelResource;
            this.iconResource = iconResource;
            this.historicalAliases = Arrays.asList(historicalAliases);
        }

        private boolean matches(String value) {
            return canonicalValue.equals(value) || historicalAliases.contains(value);
        }
    }
}
