package com.example.budgetbyte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.ocr.ReceiptScannerHelper;

import org.junit.Test;

import java.time.LocalDate;

public final class OcrParserTest {
	@Test
	public void parsesWesternDigitsAndDotDecimalsExactly() {
		Expense draft = ReceiptScannerHelper.parseDraft("TOTAL: EGP 125.50");
		assertEquals(12550L, draft.amount);
	}

	@Test
	public void parsesCommaDecimalAmountsExactly() {
		Expense draft = ReceiptScannerHelper.parseDraft("Total: 125,50 EGP");
		assertEquals(12550L, draft.amount);
	}

	@Test
	public void parsesGroupedAmountAndCurrencySymbol() {
		Expense draft = ReceiptScannerHelper.parseDraft("Grand Total: ج.م 1,234.56");
		assertEquals(123456L, draft.amount);
	}

	@Test
	public void normalizesArabicIndicDigitsAndDecimalSeparator() {
		Expense draft = ReceiptScannerHelper.parseDraft("الإجمالي: ١٢٣٫٤٥ ج.م");
		assertEquals(12345L, draft.amount);
	}

	@Test
	public void normalizesEasternArabicIndicDigits() {
		Expense draft = ReceiptScannerHelper.parseDraft("Total: ۱۲۳٫۴۵");
		assertEquals(12345L, draft.amount);
	}

	@Test
	public void validDateIsStoredAsCanonicalIsoDate() {
		Expense draft = ReceiptScannerHelper.parseDraft("Date: 31/12/2025\nTotal: 10.50");
		assertEquals("2025-12-31", draft.date);
		assertEquals(1050L, draft.amount);
	}

	@Test
	public void missingAmountIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> ReceiptScannerHelper.parseDraft("Receipt\nDate: 31/12/2025"));
	}

	@Test
	public void invalidDateFallsBackToToday() {
		Expense draft = ReceiptScannerHelper.parseDraft("Date: 31/02/2025\nTotal: 10.00");
		assertEquals(LocalDate.now().toString(), draft.date);
	}

	@Test
	public void labelledTotalWinsOverSubtotalAndLargerItemValues() {
		Expense draft = ReceiptScannerHelper.parseDraft(
				"Item 199.00\nSubtotal 199.00\nTax 20.00\nGrand Total 12.50\nChange 200.00");
		assertEquals(1250L, draft.amount);
	}

	@Test
	public void dateDigitsAreNotMistakenForAnAmount() {
		assertThrows(IllegalArgumentException.class,
				() -> ReceiptScannerHelper.parseDraft("Date: 31/12/2025"));
	}

	@Test
	public void multipleUnlabelledAmountsChooseLargestCandidate() {
		Expense draft = ReceiptScannerHelper.parseDraft("Item 3.00\nItem 8.25");
		assertEquals(825L, draft.amount);
	}

	@Test
	public void categoryIsOnlyInferredFromClearExistingKeyword() {
		Expense recognized = ReceiptScannerHelper.parseDraft("Pharmacy\nTotal 20.00");
		Expense unknown = ReceiptScannerHelper.parseDraft("Merchant\nTotal 20.00");
		assertEquals("Health", recognized.category);
		assertEquals("Uncategorized", unknown.category);
	}

	@Test
	public void storeNameFallsBackInsteadOfBeingInvented() {
		Expense draft = ReceiptScannerHelper.parseDraft("Total 20.00");
		assertEquals("Receipt", draft.title);
	}

	@Test
	public void extractsConservativeLatinStoreNameWhenPresent() {
		Expense draft = ReceiptScannerHelper.parseDraft("Cafe Nile\nTotal 20.00");
		assertEquals("Cafe Nile", draft.title);
	}
}
