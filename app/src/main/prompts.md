You are a Senior Android Engineer and UI/UX Designer. You are working on an existing native Android app called BudgetByte (Java, Room, MVVM with ViewModel/LiveData, Gradle Kotlin DSL). Do NOT rewrite the app from scratch. Read the existing code first, then refactor and redesign step by step.

Goal: a SIMPLE, clean, modern budget app. Less clutter, big readable numbers, few taps to add a transaction.

## PART A: Code refactor (do this first)
1. Package structure: reorganize into clear packages, e.g. `data/` (db, dao, entity, repository), `ui/` (dashboard, add, history, budget, stats, common), `util/`, `ocr/`. Move files without changing behavior.
2. Replace findViewById with ViewBinding everywhere (enable `buildFeatures { viewBinding = true }`).
3. Replace every ListView/RecyclerView adapter with `ListAdapter` + `DiffUtil.ItemCallback`.
4. Move all hardcoded strings to `strings.xml`, all colors to `colors.xml`, all sizes to `dimens.xml`. No hardcoded values in layouts.
5. Use a single-Activity structure with a BottomNavigationView and Fragments (Navigation Component) if the app is currently multi-Activity: Home, Transactions, Budget, Stats.
6. Remove dead code, unused imports and unused resources. Add short JavaDoc only where logic is not obvious.
7. Keep OCR (ReceiptScannerHelper) behavior unchanged.

## PART B: Design system (create first, then apply)
Style: Material 3 (Material Components), rounded corners (16dp cards, 12dp buttons), soft elevation, generous spacing (16dp grid), large numbers.

Color palette (blue + burgundy, my favorites). Put these in colors.xml and wire them into the Material 3 theme (colorPrimary, colorSecondary, colorSurface, etc.):
- Primary blue: #1F4FD8 (dark variant #14338F, light tint #E6ECFB)
- Burgundy (secondary/accent): #7B1E3A (dark variant #4E0F23, light tint #F6E7EC)
- Income color: blue #1F4FD8
- Expense color: burgundy #7B1E3A
  (Do NOT use green/red, so the app has its own identity and is colorblind-friendly.)
- Background light: #F7F8FC, surface: #FFFFFF, text primary: #1A1C24, text secondary: #6B7080
- Dark theme (DayNight, `values-night/`): background #0F1220, surface #1A1D2E, primary blue #6E92FF, burgundy #D4647F, text #ECEEF6
- Support light and dark mode with a toggle (System / Light / Dark) in Settings.

Typography: one font family (system default or Inter/Poppins via downloadable fonts). Balance amount: 32sp bold. Titles 20sp. Body 14sp.

## PART C: Screens (keep simple, inspired by popular budget apps)
1. HOME / Dashboard
   - Top: gradient card (blue -> dark blue) showing Total Balance (big), with smaller Income (blue chip) and Expenses (burgundy chip) below it.
   - Month selector (< October 2026 >) that filters everything.
   - Category grid: rounded colored tiles (3 columns) showing icon, category name and amount spent this month. Tapping a tile opens that category's transactions.
   - "Recent transactions" list (last 5) with icon, title, category, amount (blue for income with +, burgundy for expense with -) and "See all".
   - Extended FAB "+ Add" at the bottom right.
2. ADD TRANSACTION (fast entry)
   - Segmented toggle: Expense | Income.
   - Big amount display at the top with a built-in calculator-style number pad (supports 20+15 style input).
   - Category chips in a horizontally scrollable row, date chip (default today), optional note field.
   - "Scan receipt" button (camera icon) that uses the existing ReceiptScannerHelper, shows a loading indicator, then pre-fills amount and title for the user to confirm. Never auto-save.
   - Large Save button.
3. TRANSACTIONS
   - Search bar, filter chips (month, category, type), grouped by date with day headers.
   - Swipe to delete with Undo snackbar; tap to edit.
4. BUDGET
   - Set a monthly limit per category (or overall).
   - Each budget row shows a progress bar: blue under 80%, burgundy at 80%+ with a "near limit" label.
5. STATS
   - Donut chart of spending by category (center shows total) and a bar chart of Income vs Expenses for the last 6 months. Use MPAndroidChart with the app palette.
   - Category legend list with percentages.
6. SETTINGS
   - Theme, currency, export CSV/PDF, biometric lock toggle.

Out of scope for now (do NOT build): cloud sync, family sharing, assets/investments, stocks, goals.

## PART D: UX details
- Empty states with an icon and a friendly message ("No transactions yet, tap + to add one").
- Smooth but subtle animations (fade/slide 200-300ms), ripple on touch, no heavy animations.
- Minimum touch target 48dp, content descriptions on all icons, support font scaling.
- Use Material icons (vector drawables), one consistent icon set for categories. Each category has its own tint (pick from blue/burgundy tints plus a few neutral complementary colors).
- All amounts formatted via the existing MoneyUtils, never raw numbers.

## Rules
- Java only, keep naming style. Work in small compilable steps and make sure the app builds after each one.
- Order: Part A -> Part B (theme) -> Home -> Add -> Transactions -> Budget -> Stats -> Settings.
- Before editing, list the files you plan to change or create. Do ONE screen at a time and stop after each so I can test.
- If something is unclear, ask me one question rather than guessing.
- At the end give me: summary of changed files, screenshots checklist (light and dark mode for each screen), and any new dependencies added.

================================================================================================================================================

You are a **Senior Android Engineer, Senior Java Developer, Android Software Architect, and UI/UX Designer**.

You are working on an **existing native Android application** called **BudgetByte**.

The application is written in:

- Java
- Android SDK
- AndroidX
- Room
- MVVM
- ViewModel
- LiveData
- Gradle Kotlin DSL

Your job is to **improve the existing application**, not rebuild it from scratch.

# CRITICAL WORKING RULE

Before changing ANY code:

1. Read and understand the existing project structure.
2. Read the relevant source files.
3. Identify dependencies between files.
4. Explain what you intend to change.
5. List the exact files you plan to create/modify/delete.
6. Make ONE small, compilable change at a time.
7. Stop after that step so I can build and test the application.
8. Do NOT continue to the next step until I explicitly tell you to continue.

Never assume that a class, method, resource, dependency, database field, or architecture component exists.

If required information is missing, ask me **ONE specific question** instead of guessing.

---

# PRIMARY GOAL

Transform BudgetByte into a:

- Simple
- Clean
- Modern
- Fast
- Production-ready
- Easy-to-use

personal budget application.

The design philosophy is:

> **Less clutter + fewer taps + large readable numbers + clear financial information.**

Do NOT add unnecessary features or complicated UI.

---

# NON-NEGOTIABLE ARCHITECTURE RULES

## 1. Java Only

All implementation must remain in **Java**.

Do NOT migrate anything to Kotlin unless I explicitly request it.

---

## 2. Existing Application First

Do NOT rewrite the application from scratch.

Preserve:

- Existing functionality
- Existing database
- Existing business logic
- Existing OCR behavior
- Existing package naming where practical
- Existing working features

Only refactor when there is a clear benefit.

---

## 3. Financial Precision

All monetary values MUST use:

```java
long
```

representing minor currency units.

Never use:

```java
double
float
```

for financial storage or calculations.

Example:

```text
10.50 EGP
→
1050 piastres
```

All UI formatting must use the existing:

```text
MoneyUtils
```

Never display raw monetary values directly.

If MoneyUtils already exists, reuse it rather than creating a second money-formatting system.

---

# PART A — CODEBASE REFACTOR

This part must be completed BEFORE redesigning the screens.

However, perform it **one small step at a time**.

## A1 — Package Structure

Organize the project into clear packages such as:

```text
data/
    db/
    dao/
    entity/
    repository/

ui/
    dashboard/
    add/
    history/
    budget/
    stats/
    settings/
    common/

util/

ocr/
```

Move files without changing behavior.

Do NOT combine this step with unrelated UI changes.

After moving files:

- Fix imports
- Build the project
- Verify functionality
- Stop

---

# A2 — ViewBinding

Replace:

```java
findViewById()
```

with ViewBinding.

Enable:

```kotlin
buildFeatures {
    viewBinding = true
}
```

in the appropriate Gradle Kotlin DSL file.

Rules:

- Do not mix ViewBinding and findViewById unnecessarily.
- Do not change UI behavior.
- Make sure Fragment binding lifecycle is handled correctly.
- Avoid memory leaks.

For Fragments, use the standard pattern:

```java
private FragmentExampleBinding binding;
```

and clear it in:

```java
onDestroyView()
```

---

# A3 — RecyclerView/ListView Modernization

Replace old adapters with:

```text
ListAdapter
+
DiffUtil.ItemCallback
```

Do this one adapter at a time.

Do not modify multiple unrelated adapters in one step.

After each adapter:

1. Compile
2. Test
3. Stop

---

# A4 — Resource Cleanup

Move hardcoded:

- Strings → `strings.xml`
- Colors → `colors.xml`
- Dimensions → `dimens.xml`

Layouts must not contain unnecessary hardcoded values.

Use resources such as:

```xml
@string/...
@color/...
@dimen/...
```

Keep Android-required values such as certain accessibility/content descriptions or simple technical attributes where appropriate, but avoid unnecessary duplication.

---

# A5 — Navigation Architecture

If the current application uses multiple Activities for the main sections, gradually move toward:

```text
Single Activity
        ↓
Navigation Component
        ↓
Fragments
```

Main destinations:

```text
Home
Transactions
Budget
Stats
```

Use:

```text
BottomNavigationView
```

Do NOT perform this migration if the existing architecture would require a large risky rewrite in one step.

Break it into small compilable migrations.

---

# A6 — Dead Code

Remove:

- Unused imports
- Dead Java code
- Unused layouts
- Unused drawables
- Unused resources
- Unused dependencies

Do not delete anything unless you verify that it is unused.

---

# OCR REQUIREMENT

The existing:

```text
ReceiptScannerHelper
```

must keep its current behavior during Part A.

Do NOT rewrite OCR during the architecture refactor.

Later, when improving the Add Transaction screen, OCR must:

```text
Scan
↓
Parse
↓
Pre-fill
↓
User reviews
↓
User confirms
↓
Save
```

Never:

```text
Scan → Automatically Save
```

---

# PART B — DESIGN SYSTEM

Do NOT start screen redesign until Part A has been completed and tested.

Create the design system first.

Use:

**Material 3 / Material Components**

Design style:

- Clean
- Minimal
- Modern
- Rounded
- Spacious
- Financial dashboard focused

---

# COLOR SYSTEM

Use the following colors.

## Light Theme

Primary blue:

```text
#1F4FD8
```

Dark blue:

```text
#14338F
```

Light blue tint:

```text
#E6ECFB
```

Burgundy:

```text
#7B1E3A
```

Dark burgundy:

```text
#4E0F23
```

Light burgundy:

```text
#F6E7EC
```

Income:

```text
#1F4FD8
```

Expense:

```text
#7B1E3A
```

Background:

```text
#F7F8FC
```

Surface:

```text
#FFFFFF
```

Primary text:

```text
#1A1C24
```

Secondary text:

```text
#6B7080
```

Do NOT use traditional:

```text
Green = income
Red = expense
```

Use the BudgetByte blue/burgundy identity instead.

---

# DARK THEME

Create:

```text
values-night/
```

Use:

Background:

```text
#0F1220
```

Surface:

```text
#1A1D2E
```

Primary blue:

```text
#6E92FF
```

Burgundy:

```text
#D4647F
```

Primary text:

```text
#ECEEF6
```

---

# THEME MODES

Support:

```text
System
Light
Dark
```

The selection must persist.

Do not hardcode theme state only in memory.

---

# TYPOGRAPHY

Use one consistent font family.

Either:

- Android system font

or:

- Inter
- Poppins

Do not introduce a font dependency unless necessary.

Recommended hierarchy:

Balance:

```text
32sp
Bold
```

Screen titles:

```text
20sp
```

Body:

```text
14sp
```

Use appropriate line spacing and font scaling.

---

# COMPONENT STYLE

Cards:

```text
16dp corner radius
```

Buttons:

```text
12dp corner radius
```

Spacing system:

```text
16dp grid
```

Touch targets:

```text
minimum 48dp
```

Use subtle elevation.

Avoid excessive shadows.

Use Material ripple effects.

---

# PART C — SCREENS

Build screens in EXACTLY this order:

```text
1. Home
2. Add Transaction
3. Transactions
4. Budget
5. Stats
6. Settings
```

Only build ONE screen at a time.

After each screen:

1. Build
2. Test
3. Fix compile/runtime issues
4. Stop
5. Wait for my approval

---

# SCREEN 1 — HOME / DASHBOARD

The dashboard must be simple.

## Header / Balance Card

Create a large gradient card:

```text
Blue
↓
Dark Blue
```

Display:

```text
Total Balance
```

with a large number.

Below it show:

```text
Income
Expenses
```

using compact chips.

Income uses blue.

Expense uses burgundy.

---

# MONTH SELECTOR

Add:

```text
< October 2026 >
```

with previous/next controls.

The selected month must affect:

- Balance
- Income
- Expenses
- Category spending
- Recent transactions

Do not hardcode October 2026.

The displayed month must come from application state.

---

# CATEGORY GRID

Create a 3-column grid.

Each tile contains:

```text
Icon
Category name
Amount spent
```

Tiles should use subtle category-specific tints.

Tapping a category opens the Transactions screen filtered by that category.

---

# RECENT TRANSACTIONS

Display the latest five transactions.

Each item:

```text
Icon
Title
Category
Amount
```

Income:

```text
+ 1,500 EGP
```

Expense:

```text
- 250 EGP
```

Use:

```text
Blue = Income
Burgundy = Expense
```

Add:

```text
See all
```

---

# ADD BUTTON

Use an Extended FAB:

```text
+ Add
```

Position it at the bottom right.

---

# SCREEN 2 — ADD TRANSACTION

This screen must prioritize speed.

Goal:

> Add a transaction in as few taps as possible.

---

# TYPE TOGGLE

Use:

```text
Expense | Income
```

Make the selected state obvious.

---

# AMOUNT

The amount should be the main focus.

Use a large amount display.

Add a calculator-style keypad.

It must support expressions such as:

```text
20+15
100-25
10*3
```

The final result must be converted safely to minor units.

Do NOT use floating-point arithmetic for stored financial values.

---

# CATEGORY

Show horizontally scrollable category chips.

Example:

```text
Food
Transport
Shopping
Bills
Entertainment
```

---

# DATE

Provide a date chip.

Default:

```text
Today
```

Allow the user to change it.

---

# NOTE

Optional note field.

Keep it visually secondary.

---

# RECEIPT SCANNER

Provide:

```text
Scan Receipt
```

with a camera icon.

Use the existing:

```text
ReceiptScannerHelper
```

Show a loading indicator while OCR is processing.

After OCR:

- Extract candidate values
- Pre-fill amount
- Pre-fill title when possible
- Let the user edit everything
- Require explicit Save

Never automatically save.

---

# SAVE

Use a large primary Save button.

Validation must happen before saving.

Show meaningful validation errors.

---

# SCREEN 3 — TRANSACTIONS

Provide:

```text
Search
Filter
```

Filter chips:

```text
Month
Category
Type
```

Group transactions by date.

Example:

```text
Today
Yesterday
October 4, 2026
```

Each group contains transactions.

---

# DELETE

Support swipe-to-delete.

After deletion:

```text
Transaction deleted     UNDO
```

Use Snackbar.

Undo must restore the deleted transaction correctly.

---

# EDIT

Tapping a transaction opens the edit flow.

Reuse the Add Transaction screen where practical instead of creating duplicate UI.

---

# SCREEN 4 — BUDGET

Allow:

```text
Overall monthly budget
```

and/or:

```text
Category-specific budgets
```

Each budget displays:

```text
Category
Spent
Limit
Remaining
Progress
```

Progress colors:

Below 80%:

```text
Blue
```

80% or above:

```text
Burgundy
```

Show:

```text
Near limit
```

when appropriate.

Do not use WorkManager just to calculate budget progress.

Use Room aggregate queries.

---

# SCREEN 5 — STATS

Use:

```text
MPAndroidChart
```

if the dependency is not already installed.

Do not add a dependency if an existing chart solution already exists and is suitable.

---

## DONUT CHART

Display:

```text
Spending by Category
```

Center:

```text
Total spending
```

Use BudgetByte colors.

---

# BAR CHART

Display:

```text
Income vs Expenses
```

for the last six months.

Use:

```text
Blue = Income
Burgundy = Expense
```

---

# CATEGORY LEGEND

Display:

```text
Category
Percentage
Amount
```

Use matching category colors.

---

# SCREEN 6 — SETTINGS

Settings should include:

```text
Theme
Currency
Export CSV
Export PDF
Biometric Lock
```

Theme:

```text
System
Light
Dark
```

Biometric security must use AndroidX Biometric APIs.

---

# UX REQUIREMENTS

Every screen must have:

## Empty states

Example:

```text
No transactions yet

Tap + to add your first transaction.
```

Use a friendly icon.

---

## Animations

Use subtle animations only:

```text
200–300ms
```

Examples:

- Fade
- Slide
- State transitions

Do NOT use heavy or distracting animations.

---

## Accessibility

All icons must have appropriate:

```xml
contentDescription
```

Interactive controls must have at least:

```text
48dp
```

touch target.

Support:

- Font scaling
- Screen readers
- Good contrast
- Keyboard navigation where applicable

---

# ICON SYSTEM

Use Material icons.

Keep one consistent visual language.

Each category should have a recognizable icon.

Category tints may use:

- Blue variations
- Burgundy variations
- Neutral complementary colors

Do not randomly assign colors.

---

# MONEY FORMATTING

Every displayed amount MUST go through the existing:

```text
MoneyUtils
```

Never write:

```java
amount + " EGP"
```

directly.

Never manually format monetary values inside Activities/Fragments.

---

# DATABASE / PERFORMANCE RULES

Never calculate large financial totals by loading all transactions into Java.

Prefer Room queries such as:

```sql
SELECT COALESCE(SUM(amount), 0)
FROM transactions
WHERE ...
```

Use:

```text
LiveData
ViewModel
Repository
DAO
```

for reactive updates.

---

# DEPENDENCY RULE

Before adding a new dependency:

1. Check whether the project already has an equivalent.
2. Explain why the dependency is necessary.
3. Add only the minimum required dependency.
4. Use a version compatible with the existing Gradle/Android setup.

Do NOT randomly upgrade:

- Gradle
- AGP
- Room
- AndroidX
- Material
- Java
- compileSdk

unless required.

---

# COMPILATION RULE

Every implementation step must be designed to compile independently.

After each step, provide:

```text
Build verification:
✓ Code compiles
✓ No unresolved imports
✓ No missing resources
✓ No obvious lifecycle issues
```

Do not claim that the project compiles unless you actually have access to the project and can verify it.

If you cannot run the build, explicitly say that compilation still needs to be tested locally.

---

# REQUIRED RESPONSE FORMAT FOR EVERY STEP

Before changing anything:

## 1. Current Analysis

Explain what you found in the existing code.

## 2. Step Objective

Explain exactly what this step will accomplish.

## 3. Files to Modify

List exact paths.

## 4. Files to Create

List exact paths.

## 5. Files to Delete

Only if necessary.

## 6. Implementation

Provide the complete code for the files being changed.

Do not provide incomplete snippets when a complete file is required.

## 7. Why This Change

Brief explanation.

## 8. Testing

Give me the exact things I should test.

## 9. Build Verification

Tell me what should compile/build.

## 10. STOP

After completing the current step, STOP.

Do NOT implement the next step.

Wait for my confirmation.

---

# IMPLEMENTATION ORDER

Follow this exact order:

```text
PART A
 ↓
A1 Package Structure
 ↓
A2 ViewBinding
 ↓
A3 ListAdapter + DiffUtil
 ↓
A4 Resources
 ↓
A5 Navigation
 ↓
A6 Dead Code Cleanup
 ↓
TEST
 ↓
PART B
 ↓
Design System
 ↓
Theme
 ↓
TEST
 ↓
HOME
 ↓
TEST
 ↓
ADD TRANSACTION
 ↓
TEST
 ↓
TRANSACTIONS
 ↓
TEST
 ↓
BUDGET
 ↓
TEST
 ↓
STATS
 ↓
TEST
 ↓
SETTINGS
 ↓
FINAL QA
```

Do not skip ahead.

---

# OUT OF SCOPE

Do NOT implement:

- Cloud synchronization
- Firebase sync
- Family sharing
- Investments
- Stocks
- Assets
- Goals
- Cryptocurrency
- Social features

unless I explicitly request them later.

---

# FINAL QA

After all screens are completed, perform a final review covering:

## Architecture

- MVVM
- Repository
- Room
- LiveData
- Navigation
- ViewBinding

## Financial correctness

- `long` minor units
- No floating-point financial calculations
- MoneyUtils formatting
- Correct aggregation

## UX

- Few taps
- Clear hierarchy
- Large numbers
- Empty states
- Loading states
- Error states
- Undo behavior

## Accessibility

- 48dp targets
- Content descriptions
- Font scaling
- Contrast

## Themes

Test every screen in:

```text
Light
Dark
```

## Screenshots checklist

Prepare a checklist for:

- Home — Light
- Home — Dark
- Add Transaction — Light
- Add Transaction — Dark
- Transactions — Light
- Transactions — Dark
- Budget — Light
- Budget — Dark
- Stats — Light
- Stats — Dark
- Settings — Light
- Settings — Dark

## Final Report

At the end provide:

1. Summary of changed files
2. New files
3. Deleted files
4. New dependencies
5. Database changes/migrations
6. Features implemented
7. Screenshots checklist
8. Known issues
9. Recommended next step

---

# START HERE

Do NOT modify the project yet.

First, inspect the existing BudgetByte project and report:

1. Current project/package structure
2. Current Activities and Fragments
3. Current Room entities
4. Current DAOs
5. Current repositories/managers
6. Current adapters
7. Current layouts
8. Current theme/colors
9. Current dependencies
10. Current OCR implementation
11. Current MoneyUtils implementation
12. Current navigation architecture
13. Any architectural problems you found

Then propose **ONLY the first small refactoring step**.

List the exact files you need to modify/create.

**Do not write implementation code yet.**

Wait for my confirmation before making the first change.
