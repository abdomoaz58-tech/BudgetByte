---
name: BudgetByte Pro System
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434654'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#737686'
  outline-variant: '#c3c5d7'
  surface-tint: '#1353d8'
  primary: '#003fb1'
  on-primary: '#ffffff'
  primary-container: '#1a56db'
  on-primary-container: '#d4dcff'
  inverse-primary: '#b5c4ff'
  secondary: '#a5345d'
  on-secondary: '#ffffff'
  secondary-container: '#ff7ba4'
  on-secondary-container: '#760b3a'
  tertiary: '#005439'
  on-tertiary: '#ffffff'
  tertiary-container: '#006f4d'
  on-tertiary-container: '#7ff2be'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b5c4ff'
  on-primary-fixed: '#00174d'
  on-primary-fixed-variant: '#003dab'
  secondary-fixed: '#ffd9e1'
  secondary-fixed-dim: '#ffb1c5'
  on-secondary-fixed: '#3f001a'
  on-secondary-fixed-variant: '#861a45'
  tertiary-fixed: '#85f8c4'
  tertiary-fixed-dim: '#68dba9'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: IBM Plex Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: IBM Plex Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: IBM Plex Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: IBM Plex Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
  title-lg:
    fontFamily: IBM Plex Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  title-md:
    fontFamily: IBM Plex Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: IBM Plex Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: IBM Plex Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  label-lg:
    fontFamily: IBM Plex Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: IBM Plex Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: IBM Plex Sans
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a premium, authoritative, yet approachable financial management tool crafted specifically for Arabic-first (RTL) mobile interaction. It merges modern Android Material 3 tactile mechanics with contemporary executive fintech aesthetics: clean white and slate surfaces, deeply saturated functional color coding, and crisp numerical clarity.

The target demographic consists of ambitious professionals, entrepreneurs, and discerning individuals seeking effortless clarity over their day-to-day liquidity, expenditure, and savings. The emotional tone is deliberate, disciplined, and frictionless—fostering confidence through high visual hierarchy, clean geometry, and zero cognitive clutter. 

The aesthetic philosophy centers on **Material Minimalist Fintech**: generous whitespace, high-fidelity soft elevation, rounded-2xl structural containers, and tactile tactile pill and button interactions.

## Colors

The color system delivers instantaneous functional legibility:
- **Primary Deep Cobalt (`#1A56DB`):** Applied to active navigation indicators, key actions, primary balances, positive income transactions, and active state chips. Conveys institutional trust and modern software sophistication.
- **Secondary Burgundy / Wine (`#831843`):** Applied exclusively to expenditure metrics, debit debriefs, negative transaction balances, and primary expense entry actions. Replaces aggressive traffic-light reds with an executive, refined tone.
- **Tertiary Emerald (`#059669`):** Reserved for growth confirmations, completed savings goals, and positive net cash flow tags.
- **Neutral Dark Slate (`#0F172A`):** Anchors high-contrast headlines, primary figures, and structural iconography.

Surfaces rely on subtle slate tiers: Canvas `#F8FAFC`, elevated cards `#FFFFFF`, and tonal overlays `#F1F5F9`. Thin structural borders use `#E2E8F0` at 1px to provide container separation against neutral backdrops.

## Typography

The typography utilizes IBM Plex Sans (with full Arabic and Latin complement) to ensure high-legibility geometric shaping across RTL and LTR contexts. Arabic script renders naturally with deliberate vertical proportions, preventing clipping in dense dashboard layouts. 

Financial figures paired with currency notations (e.g., `EGP`) use tabular figures and bold weights (`display-lg` and `headline-lg`) to preserve baseline alignment. Arabic subheaders maintain a steady 1.4-1.6x line-height ratio to preserve diacritics and complex character anatomy.

## Layout & Spacing

The layout is built around a mobile-first fluid container with full Right-to-Left (RTL) mirroring:
- **Base Grid:** 4 columns on mobile (<600px), 8 columns on tablet (600–1024px), and 12 columns on desktop/wide screens (>1024px).
- **RTL Fluid Hierarchy:** Start alignment maps to right; end alignment maps to left. Transaction listings display category badges and titles anchored to the right edge, while directional quantitative amounts (`+1000.00 EGP`, `-800.00 EGP`) pin directly against the left edge.
- **Rhythm & Safe Areas:** The bottom navigation bar accounts for device navigation pill cutouts via `2.5rem` bottom clearance, while main content sections utilize `space-md` gaps.

## Elevation & Depth

Visual depth follows a soft-ambient surface layering principle adapted from Material 3:
- **Level 0 (Canvas Base):** Flat `#F8FAFC` background.
- **Level 1 (Card & Containers):** `#FFFFFF` surfaces bounded by a hairline border (`1px solid #E2E8F0`) paired with a diffused ambient shadow: `box-shadow: 0 4px 12px -2px rgba(15, 23, 42, 0.04), 0 2px 4px -2px rgba(15, 23, 42, 0.02)`.
- **Level 2 (Floating Action & Modals):** Elevated buttons and quick-action sheets use `0 8px 24px -4px rgba(26, 86, 219, 0.16)` to cast tinted ambient glows based on button semantics (cobalt for income/actions, burgundy for expense quick-adds).
- **Level 3 (Sticky Navigation):** Bottom bar sits at `0 -4px 16px 0 rgba(15, 23, 42, 0.04)` over a frosted backdrop filter blur (`backdrop-filter: blur(12px)`).

## Shapes

The design system incorporates generous curvature to create a soft, inviting interface:
- **Cards & Data Panels:** Styled with `1rem` to `1.25rem` (rounded-2xl) radius to frame balance metrics and analytics lists.
- **Primary Buttons & Transaction Pills:** Styled with fully rounded pill radii (`9999px`) for touch targets, ensuring unambiguous interactive affordance.
- **Category Chips & Input Boxes:** Styled with `0.75rem` radius for structured compact grouping.

## Components

### Buttons & Interactive CTAs
- **Primary Actions:** Full-pill (`rounded-full`) dual-action switcher: Deep Cobalt (`#1A56DB`) for Income ("الدخل") and Rich Burgundy (`#831843`) for Expenses ("المصروفات") with bold white text.
- **Floating / Primary Add:** Floating pill button with prominent icon and label ("+ إضافة"), applying burgundy fill and gentle drop shadow.
- **Secondary & Ghost:** Outlined with `1px solid #E2E8F0` on white fill, featuring cobalt text for utility actions such as receipt scanning ("مسح الإيصال").

### Transaction Row Item
- Contained within a rounded-2xl white card.
- Right section: Category visual container (vibrant cobalt/burgundy rounded rectangle) holding the Arabic category title, paired with localized micro-dates (`07-10-2026`).
- Left section: Tabular, high-contrast currency display (`+1000.00 EGP` in cobalt or `-800.00 EGP` in dark neutral/burgundy).

### Cards & Summary Modules
- Main Balance display features centered or right-aligned micro-label ("الرصيد") followed by extra-bold `display-lg` numbers (`0.00 EGP`).
- Highlight insights cards utilize soft pastel-tinted surfaces (e.g., `#FDF2F8` for expense alerts) with deep burgundy accent typography.

### Form Inputs & Selectors
- Text input fields feature a 1px border (`#CBD5E1`), subtle slate placeholder, and focus states that transition cleanly to a 2px cobalt ring with zero layout shift.

### Bottom Navigation Bar
- High-clarity Material 3 mobile navigation bar anchored to screen bottom. Contains icons and micro-labels for Home ("الرئيسية"), Transactions ("المعاملات"), Budget ("الميزانية"), Stats ("الإحصائيات"), and Settings ("الإعدادات"). Active tab renders in `#1A56DB` with a subtle soft blue indicator capsule behind the icon.