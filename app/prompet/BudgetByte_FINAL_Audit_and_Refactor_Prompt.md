# BudgetByte Pro — Logic, UI, State (SSOT/UDF) Audit and Refactor

You are a Senior Android Engineer (Java, MVVM, Room, LiveData, ViewBinding, Material 3, Navigation Component, UDF).

This is a TWO-STAGE task:
- **STAGE 0:** Read-only, evidence-based audit and plan. Then STOP.
- **STAGE 1:** Implementation, only after I send the exact word: `continue`.

Do NOT implement, patch, or edit anything in Stage 0.

---

## PROJECT CONTEXT

Inspect the project from its current root. Do not assume any absolute path.

- Application ID: `com.example.budgetbyte.v2`
- Native Android, **Java ONLY** (no Kotlin, coroutines, Hilt, Flow)
- XML layouts, ViewBinding, Material Components / existing Material 3 resources, AndroidX
- Navigation Component, MVVM, ViewModel + LiveData, Room
- Existing: `ExpenseRepository`, `ExpenseDao`, `Expense`, `MoneyUtils` (long minor units), `SelectedMonthViewModel`, OCR scanner and its navigation contract, `ReceiptScannerHelper`

The current source is the only source of truth. The findings in this prompt are leads, not proof. Verify each one. If a lead is already fixed or wrong, say so.

## ABSOLUTE RULES (both stages)

- Do NOT change: applicationId, Room schema/migrations, `SecurityHelper`, `BiometricHelper`, `PdfExporter` behavior (except the confirmed total bug, in Stage 1 only), navigation structure/contract (argument names and types), or the existing OCR contract.
- Do NOT add dependencies. Do NOT introduce a new architecture, new repository, or new Expense model.
- OCR NEVER writes to Room. It only produces an unsaved draft; only the Save button in the Add form inserts.
- No static/global state. Money only through `MoneyUtils` (long minor units, BigDecimal HALF_UP, never Double/float, never `%.2f`).
- Dates: canonical storage `yyyy-MM-dd`, one localized display formatter.
- All user-facing strings in `values/` AND `values-ar/`. No hardcoded strings or hex colors in layouts or Java.
- Use the project's existing resource names (for example `bb_secondary`, `bb_on_secondary`, `bb_primary_container`, `bb_surface_lowest`, `bb_outline_variant`). Do not create duplicate colors or resources.
- Do NOT rename, normalize, or migrate stored category values without my explicit approval.
- Preserve all existing working behavior and historical user data. Prefer small, verifiable changes over rewrites.
- Do not reformat or "clean up" unrelated code.

---

# STAGE 0 — READ-ONLY AUDIT

## Stage 0 rules

- Do not modify, create, delete, rename, or reformat any file (sources, layouts, resources, Gradle, manifest, navigation).
- Do not clear app data, insert/delete records, or change preferences.
- Run `git status --short` first. Report and preserve any pre-existing changes.
- Use static inspection and read-only searches. You may read existing test reports. Run no command that modifies project files.
- Check `adb devices -l`. Claim runtime behavior only if a real device or emulator was actually used. Otherwise label it RUNTIME-UNVERIFIED.
- Do not claim a file was read line by line if it was only searched or sampled. List exactly what was and was not fully read.
- Do not treat a class or resource as active just because it exists. Trace real references and navigation paths.
- Do not call code broken just because it looks unusual, and do not call it working just because it compiles.

## A. Establish the real architecture

Identify: application ID and packages, launcher Activity, registered nav destinations and the actual Fragment behind each, the real layout each controller inflates, ViewBinding usage, ViewModel/Repository/DAO relationships, Room entity and queries, shared state owners, budget persistence, settings persistence, OCR draft transport, localization/RTL resources, existing tests.

Also confirm that the installed/launched app opens the same `AddExpenseFragment` and layouts that the source tree shows (no duplicate or stale fragment being opened instead).

## B. Audit checklist (verify every item against current code)

### 1. Shared month state (CRITICAL)
Suspected: `setCurrentMonth()` is called in `onViewCreated` of `HomeFragment` (~line 52), `BudgetFragment` (~46), `StatsFragment` (~62), resetting the selected month to the current month on every tab switch.
Trace: `Month selector → SelectedMonthViewModel → screen ViewModel → Repository/DAO → LiveData → UI`.
Check: one shared selected-month state; any Fragment overwriting it; label vs. query agreement; same month boundaries in Home/Budget/Stats; month lost when switching tabs; observers or queries recreated needlessly.

### 2. observeForever and lifecycle (CRITICAL)
Search the whole tree for `observeForever`. For each occurrence (especially `BudgetViewModel.loadForMonth`, `StatsViewModel.loadForMonth`): why it exists, whether a new observer is added per month change, whether old ones are removed, whether obsolete month queries still update state, whether destroyed UI can stay referenced, duplicate calculations. Do not call every `observeForever` a leak without proof. Give the smallest safe fix per confirmed defect (for example `switchMap` on the selected month, or `removeObserver` of the previous source).

### 3. Home not linked to selected month
Suspected: `HomeViewModel` computes the month once from `new Date()`, month buttons change only the label, recent list is "last 5 of all time". For each displayed value, state its source: Room query, ViewModel calculation, hard-coded, or stale copy. Establish the intended product behavior for the recent list before calling it a defect. Check income, expense, balance consistency and that swipe-delete refreshes the real list.

### 4. Transactions screen
Suspected: `fragment_transactions.xml` (~834 lines) contains a duplicated "Add Transaction" design (static "250.00", fixed date, hardcoded Arabic, ~68 hex colors) next to the real RecyclerView `rvTransactions`, and has no search/filter.
Verify XML IDs and Java references (including indirect ones) before concluding. Document exactly which structures are dead and which are active. Check: search and type filter controls exist and are wired to `TransactionsViewModel`; they combine correctly; clearing search keeps the filter; DAO supports it; row click opens Edit; delete updates data and list; Undo; empty/loading states; whether `ExpenseAdapter.filter()` / `fullList` are dead or used indirectly. Propose a minimal layout correction (do not rewrite in Stage 0).

### 5. Categories
Suspected: stored canonical values like `Food`, `Transportation` (English) are translated only in AddExpense; Budget/Stats display the raw `item.category`; `"Other"` is hardcoded in Stats.
List every category representation found (English, Arabic, `Other`, `Uncategorized`, OCR-produced values), every category-dependent query/filter, and whether inconsistent historical values already exist or are only suspected.
Evaluate a stable internal key (for example `food`) with localized labels resolved at display time. State clearly: can a **display-time mapping with legacy-value support** fix the issue with NO migration? If a real migration is necessary, list the risk to history, queries, budgets, stats, and mark it APPROVAL REQUIRED. Do not touch stored data.
Also: categories must follow the selected type (Income vs Expense). If income categories do not exist in code, say so and do not invent them without telling me.

### 6. Add/Edit form state
Inspect `AddExpenseFragment`, `AddExpenseViewModel`, `SavedStateHandle`, `nav_graph.xml`, `ExpenseRepository`, `ExpenseDao`, `MoneyUtils`.
Check: Add vs Edit initialization; old values leaking into a new form; text appended/duplicated; `setText()` loops and cursor jumps; LiveData re-render wiping active input; repeated TextWatchers; SavedStateHandle reset; insert vs update routing; duplicate saves (double tap); Save & Add Another (must keep the selected type and clear the form, not flip back to Expense); failure handling keeps user input; success shown only after persistence; Cancel creates nothing.
Navigation defaults: suspected `title="Receipt"` and `category="Uncategorized"` in `nav_graph.xml` (~lines 16 and 18) and passed manually in `HomeFragment` (~134–136). Trace every navigation path and determine whether manual Add starts with a preset title/category.
Type/ID: case variants such as `"INCOME"` silently treated as expense (~`initializeFromArguments:150`); expense ID passed as Integer vs Long causing `ClassCastException`.
Persisted fields: list which fields the `Expense` entity really stores. Unsupported controls (payment method, notes, time) must be hidden or disabled, never faked.
Future dates: report current behavior (allowed or blocked).

### 7. Dates and money
- **MaterialDatePicker:** show the exact conversion path in `AddExpenseFragment` (~line 531). MaterialDatePicker expects UTC midnight millis; a local-time timestamp can show the previous day in Africa/Cairo. Prove the shift path, do not assume. Confirm canonical `yyyy-MM-dd` storage and one display formatter.
- **`MoneyUtils.parseMinorUnits`** (~line 340): inspect the real regex. Check Western `50`, Arabic-Indic `٥٠`, Eastern Arabic-Indic `۵۰`, decimal separators (`.`, `,`, Arabic `٫`), thousands separators (`,`, `٬`), currency symbols, empty/invalid input. Confirm long minor units and exact arithmetic. Find any separate parsing path (Add form, OCR) or any floating-point money math.

### 8. OCR and draft navigation
Inspect `OcrScannerFragment`, `OcrScannerViewModel`, `ReceiptScannerHelper`, `AddExpenseFragment`, nav graph. Verify in current code (not older reports): receipt capture, gallery, `scanQr`, `scanCollectedPages`, five-page limit, camera permission and denial (gallery and manual must still work, rationale shown?), flash lifecycle, feedback message dismissal (suspected: `showFeedback` only sets VISIBLE), multi-page cache file cleanup, duplicate processing on rotation.
Navigation: Home → Scanner → draft → Add → Save/Cancel, and Add → Scanner → existing Add entry. Suspected: `openDraft` navigates without `popUpTo`, so after Save `navigateUp()` returns to the scanner. Check duplicate destinations and that Cancel creates nothing. Identify the real persistence boundary and confirm OCR never calls a Repository/DAO write. Draft transport must be primitive args / Safe Args result / SavedStateHandle through the back stack, never static fields. Do not propose Parcelable unless a real need is shown.
Parser (`findCandidateAmount`): check whether a larger unlabeled number beats a smaller labeled total (suspected condition `(labelled || !foundTotal) && candidate >= bestAmount`), whether date-like numbers (for example `2026`) become amounts, whether `الاجمالي` (no hamza) and other Arabic total spellings are supported, whether only `d/M/y` dates are accepted, whether subtotal/tax/change can beat the real total.
Be honest about recognizer scripts: do not claim Arabic-script OCR unless the configured recognizer supports it (ML Kit Latin reads digits and Latin text, not Arabic words).

### 9. Insets and bottom navigation
Inspect `MainActivity`, Home, Transactions, Budget, Stats, AddExpense, OcrScanner.
Check: top inset applied at host (`MainActivity` ~line 33) AND again in screens with extra +16; pixel-vs-dp in Java (for example `HomeFragment` ~61, `AddExpenseFragment` ~556; ignore XML `dp`); accumulating bottom padding from `getPaddingBottom() + navBars.bottom` (~`MainActivity:34-38`) after repeated dispatch, rotation, or keyboard; BottomNavigationView still visible on Add/Edit and OCR (no destination listener?). State who should own each inset. Recommend one consistent ownership approach, not scattered padding patches. Also list `left/right` attributes that should be `start/end`.

### 10. Calculations and reactivity
Trace where each value is computed (Fragment, ViewModel, Repository, util): income, expense, balance, budget and remaining (`Remaining = Monthly Budget − Actual Expenses`, same selected month), category totals and percentages, chart datasets, Smart Insights, recent list. Look for duplicated or disagreeing calculations, divide-by-zero, percentages over 100%, invalid chart values, wrong month boundaries, stale values after CRUD, unnecessary manual refreshes. Identify calculations that should move out of Fragments (for example `BudgetFragment`) into ViewModels or pure Java utilities.

### 11. Repository, DAO, CRUD, Undo
Check: separate `ExpenseRepository` + executor per ViewModel; callbacks arriving on the executor thread instead of main (trace callers before judging); Edit loading the whole table via `getAllExpenses()` and looping (`observeEditedExpense`) vs a `getById` query; month queries using `strftime('%Y-%m', date)` (index implications; any index/column change is APPROVAL REQUIRED); swipe-delete in Home (~120) and Transactions (~79) has no confirmation and no Undo. Propose a safe Undo using Snackbar + existing Repository re-insert (restoring the same data, no duplicate on repeated taps). Do not invent capabilities the persistence layer cannot support.

### 12. PDF export and SecurityHelper
`PdfExporter`: is `total` the sum of income + expense (wrong semantics)? What does each total mean? Hardcoded English strings and currency. `SecurityHelper`: repeated `MasterKey` / `EncryptedSharedPreferences` creation on the main thread, only report with call-path evidence. Do not change security settings or stored prefs.

### 13. Settings and theme
Check hardcoded profile name/email, "Pro" tier, wallet count, percentages; theme/language/currency/logout controls with no real behavior; biometric switch (real, preserve it); forced `MODE_NIGHT_NO` (~`MainActivity:24`) preventing `values-night`. Label each control: functional / static / intentionally unavailable / misleading. Do not invent a backend or accounts.

### 14. Charts and dead code
`StatsFragment` pie vs legend colors (~113 vs ~160: pie `primary_blue/burgundy` vs legend `primary/secondary`?). Duplicate palette values (only with evidence). `ExpenseAdapter.filter()` / `fullList` dead or indirect. Highly compressed files (for example `SettingsFragment`): is formatting blocking safe maintenance? Do not reformat in Stage 0.

### 15. Localization and resources
Diff `values/strings.xml` vs `values-ar/strings.xml` **by key**, not by count (earlier estimate: 125 vs 87). Produce the exact missing-key list with meaning, including `income`, `expense`, `balance`, `save`, `cancel`, `title`, `insight_empty`, `insight_category_format`, `app_name`. Also report: hardcoded English/Arabic in XML and Java, hardcoded `EGP` while `currency_egp` exists and is unused, direct hex colors (earlier counts: Settings 42, Transactions 68, Stats 4, Home 1, Budget 1), and missing/unused resource references. Required strings for the form: "Save expense" / "Save income" (Arabic: حفظ المصروف / حفظ الدخل) from resources, never hardcoded.

### 16. Stale resources and manifest
Examine `activity_*`, Bento, Neon, Vault, Asset-allocation resources and stale manifest Activity declarations. Before calling anything unused: search Java, XML, nav actions/destination IDs, manifest, styles/themes/drawables. Label unused only with evidence. Produce a cleanup list. Delete nothing.

## C. Evidence format (every significant finding)

- Severity: CRITICAL / HIGH / MEDIUM / LOW
- Classification: CONFIRMED DEFECT / SUSPECTED DEFECT / MISSING FEATURE / IMPROVEMENT
- Confidence: HIGH / MEDIUM / LOW
- Exact file path, class/method/XML ID/resource key, line numbers
- Observed code, why it is a problem, expected behavior
- Minimal proposed fix, files and tests affected
- Runtime verification needed? (yes/no)
- Approval needed? (schema migration, dependency, navigation contract, security)

Avoid vague claims. Show the code path or the missing connection. If a lead cannot be confirmed, say what evidence is missing.

## D. Required report (in this order)

A. Architecture and state ownership (Home, Add/Edit, Transactions, Budget, Stats, OCR, Settings)
B. Critical and high-priority findings
C. UI-to-logic matrix (control | expected action | listener | ViewModel/logic target | data source/effect | status)
D. Screen-by-screen audit (working / partial / broken / static / unverified)
E. State management and UDF issues
F. Categories and historical-data compatibility (state explicitly whether a migration is required, not required, or not established)
G. Layouts, insets, navigation, RTL
H. Money, dates, persistence, CRUD, Undo
I. OCR and PDF export
J. Settings and localization (exact missing Arabic keys)
K. Resource and manifest audit (reference evidence, nothing deleted)
L. Verification and limitations (files fully read vs sampled, tests only read from reports, device availability, runtime evidence yes/no)
M. Prioritized implementation plan: phase name, defects addressed, exact files, dependencies between phases, required tests, migration/approval needs
N. File integrity — expected answer: `NO — AUDIT ONLY`
O. Final status — exactly one of:
   - `AUDIT COMPLETE — WAITING FOR CONTINUE`
   - `AUDIT PARTIAL — MORE SOURCE REVIEW REQUIRED`

## E. Mandatory stop

After delivering the report: **STOP. Do not fix, patch, or continue into implementation.** Wait for the exact word `continue`.

---

# STAGE 1 — LOCKED UNTIL I SEND `continue`

Implement in small phases, following the approved plan from Stage 0. After each phase: build, test, report, and keep going unless a build/test fails or an approval is needed.

## Phase order (adjust only if Stage 0 shows a dependency)

1. **Shared month + observers:** keep selected month in `SelectedMonthViewModel`, remove the `setCurrentMonth()` resets from `onViewCreated` (set the default only once at creation). Replace per-call `observeForever` with lifecycle-aware `switchMap`/Transformations on the selected month, or deterministic observer removal. Link `HomeViewModel` to the selected month (totals, balance, and the recent list per the intended behavior established in Stage 0).
2. **Add/Edit form:** remove preset defaults (`Receipt`, `Uncategorized`) for manual entry without breaking OCR draft args; correct Add vs Edit initialization; normalize type case-insensitively; read the expense ID safely (Number → long); disable Save and Save & Add Another while saving (no double insert); Save & Add Another keeps the selected type and clears the rest; no success message before persistence; edit by ID without loading the whole table (a `getById` query is allowed only if it needs no schema change, otherwise ask); categories follow the selected type and reset if missing for the new type; hide or disable unsupported controls.
3. **Date and money:** fix the MaterialDatePicker UTC/local conversion so the selected day is correct in Africa/Cairo and storage stays `yyyy-MM-dd`; make `MoneyUtils.parseMinorUnits` accept Arabic-Indic and Eastern Arabic-Indic digits, Arabic decimal/thousands separators, and normal thousands separators, still long minor units with BigDecimal HALF_UP. Quick-amount buttons (+50/+100/+200/+500, if present) add in minor units through `MoneyUtils`, treat an empty field as 0, use `Math.addExact`, and reject overflow. Add JUnit tests: Western, Arabic-Indic, Eastern Arabic-Indic, comma/dot decimals, thousands separators, empty, invalid, overflow.
4. **Categories (compatible):** introduce display-time localization through a mapping that supports legacy stored values (English and any Arabic values found). Do NOT rewrite stored data. If full standardization needs a migration, stop and ask.
5. **Transactions screen:** remove the dead duplicated Add form from `fragment_transactions.xml`, keeping `rvTransactions`, the empty state, and proper toolbar/navigation; use existing color/string resources (no hex). Wire search and type filter through `TransactionsViewModel` and DAO queries (works together; clearing search keeps the filter). Remove dead adapter code only if Stage 0 proved it dead.
6. **Navigation and insets:** fix the Home → Scanner → Add back stack (`popUpTo` as needed, without changing the argument contract); hide BottomNavigationView on Add/Edit and OCR through one destination listener; give each inset a single owner, convert pixel values to dp, store original padding once so bottom padding cannot accumulate, and replace left/right with start/end.
7. **Calculations and CRUD:** move Fragment-owned business calculations into ViewModels or pure Java utilities, with zero-denominator guards and percentages capped for charts. Delete with Snackbar Undo through `ExpenseRepository` (re-insert), safe against repeated taps. Make sure all changes propagate through Room LiveData with no manual refresh.
8. **Localization and resources:** add every missing Arabic key from the Stage 0 list; replace hardcoded strings/`EGP`/hex colors with existing resources (reuse `bb_*` names); "Save expense" / "Save income" from resources.
9. **OCR fixes:** feedback message auto-hides; camera permission rationale plus a denied state that keeps Gallery and Manual working; clean up multi-page temp files; fix `findCandidateAmount` (prefer labeled totals over subtotal/tax/change, ignore date-like and long digit runs, support `الاجمالي`/`الإجمالي`/`المجموع`/`المبلغ`, strict plausible dates, more than `d/M/y`) inside `ReceiptScannerHelper` only, with JUnit tests. OCR still never saves.
10. **Optional / separate approval:** Settings placeholders and theme (remove the forced `MODE_NIGHT_NO` only if approved), PDF total fix, `SecurityHelper` caching, Pie/legend color alignment, stale resource deletion, `SettingsFragment` formatting. Ask before doing any of these.

## Build and test

After each phase run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`. If it fails, stop and report the exact error. Do not disable or weaken tests. Do not assume the shell is outside the project; use the project root.

## Device verification

- If a debug build and a device are available, install and test with `adb`. Verify persistence with `adb shell run-as com.example.budgetbyte.v2` and `sqlite3` on the database file (read-only queries), or with a temporary debug log in the repository, removed afterward.
- If you cannot drive a test through adb (rotation, taps, typing, screenshots), mark it **NOT TESTED** and give me exact manual steps. Never mark PASS without evidence (log lines, sqlite rows, or a described screenshot).

## Final report (Stage 1)

- Files changed (one line each) and why
- Tests added and results
- What was verified on device vs not tested
- Assumptions made and anything left for approval (migration, Settings, PDF, security)
- Manual checklist for me:
  1. Change month on Home, switch tabs: the month stays and the data matches.
  2. Add expense, then add income: types, categories, and button text are right.
  3. Date picker shows the correct day in Cairo time.
  4. `٥٠` and `1,250.50` are accepted as amounts.
  5. Double tap Save: only one record.
  6. Edit an existing transaction: same row updates, no duplicate.
  7. Save & Add Another: stays on the form, same type, cleared fields.
  8. Scan a receipt, then Cancel: nothing appears in Transactions.
  9. Search + filter in Transactions; delete then Undo.
  10. Bottom nav hidden on Add/Edit and Scanner; no padding growth after rotation.
  11. Arabic UI shows no English labels (including Budget/Stats categories).

No UI redesign and no unrelated refactoring. Stop and wait after the final report.
