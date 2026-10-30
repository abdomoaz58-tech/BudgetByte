package com.example.budgetbyte.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.example.budgetbyte.data.entity.Expense;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts receipt OCR output into an unsaved draft that must be reviewed before persistence. */
public final class ReceiptScannerHelper implements AutoCloseable {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?<![0-9])([0-9]{1,3}(?:[,.][0-9]{3})+(?:[,.][0-9]{1,2})?|[0-9]+(?:[,.][0-9]{1,2})?)(?![0-9])");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?<![0-9])([0-9]{1,2})[/-]([0-9]{1,2})[/-]([0-9]{2,4})(?![0-9])");
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public void scan(Bitmap bitmap, int rotationDegrees, Callback callback) {
        scan(InputImage.fromBitmap(bitmap, rotationDegrees), callback);
    }

    public void scan(Context context, Uri imageUri, Callback callback) {
        try {
            scan(InputImage.fromFilePath(context, imageUri), callback);
        } catch (IOException exception) {
            callback.onFailure(exception);
        }
    }

    public void scan(InputImage image, Callback callback) {
        Task<com.google.mlkit.vision.text.Text> task = recognizer.process(image);
        task.addOnSuccessListener(text -> {
            try {
                callback.onDraftReady(parseDraft(text.getText()), text.getText());
            } catch (IllegalArgumentException exception) {
                callback.onFailure(exception);
            }
        }).addOnFailureListener(callback::onFailure);
    }

    /** Parses candidate values only; this method never inserts into Room. */
    public static Expense parseDraft(String recognizedText) {
        String normalized = normalizeDigits(recognizedText);
        long amount = findCandidateAmount(normalized);
        if (amount <= 0L) {
            throw new IllegalArgumentException("No receipt total could be identified");
        }
        return new Expense(findStoreName(normalized), amount, findCategory(normalized),
            "expense", findDate(normalized));
    }

    static String normalizeDigits(String text) {
        return com.example.budgetbyte.util.MoneyUtils.normalizeDigits(text);
    }

    private static long findCandidateAmount(String text) {
        long bestLabelledAmount = 0L;
        long bestUnlabelledAmount = 0L;
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String lowerCaseLine = line.toLowerCase(Locale.ROOT);
            boolean excluded = lowerCaseLine.contains("subtotal") || lowerCaseLine.contains("sub total")
                    || lowerCaseLine.contains("tax") || lowerCaseLine.contains("change")
                    || lowerCaseLine.contains("card") || line.contains("الضريبة")
                    || line.contains("الباقي");
            boolean labelled = !excluded && (lowerCaseLine.contains("total") || lowerCaseLine.contains("amount")
                    || lowerCaseLine.contains("grand") || line.contains("الإجمالي")
                    || line.contains("الاجمالي") // without hamza — common OCR variant
                    || line.contains("المجموع") || line.contains("المبلغ"));
                Matcher dateMatcher = DATE_PATTERN.matcher(line);
            Matcher matcher = AMOUNT_PATTERN.matcher(line);
            while (matcher.find()) {
                long candidate = parseMinorUnits(matcher.group(1));
                if (candidate <= 0L || matcher.group(1).replaceAll("[^0-9]", "").length() > 9
                    || overlapsDate(matcher.start(), matcher.end(), dateMatcher)) {
                    continue;
                }
                if (labelled) {
                    bestLabelledAmount = Math.max(bestLabelledAmount, candidate);
                } else if (!excluded) {
                    bestUnlabelledAmount = Math.max(bestUnlabelledAmount, candidate);
                }
            }
        }
        return bestLabelledAmount > 0L ? bestLabelledAmount : bestUnlabelledAmount;
    }

    private static boolean overlapsDate(int start, int end, Matcher dates) {
        dates.reset();
        while (dates.find()) {
            if (start < dates.end() && end > dates.start()) {
                return true;
            }
        }
        return false;
    }

    private static long parseMinorUnits(String token) {
        try {
            return com.example.budgetbyte.util.MoneyUtils.parseMinorUnits(token);
        } catch (IllegalArgumentException exception) {
            return 0L;
        }
    }

    private static String findDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year = Integer.parseInt(matcher.group(3));
            if (year < 100) {
                year += 2000;
            }
            if (year < 2000 || year > 2100) {
                return today();
            }
            try {
                return LocalDate.of(year, month, day).toString();
            } catch (DateTimeException ignored) {
                // Fall through to today's date.
            }
        }
        return today();
    }

    private static String findCategory(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (containsAny(value, "pharmacy", "medicine", "clinic", "صيدلية", "دواء", "مستشفى")) {
            return "Health";
        }
        if (containsAny(value, "grocery", "groceries", "supermarket", "market", "بقالة", "سوبر ماركت")) {
            return "Groceries";
        }
        if (containsAny(value, "restaurant", "cafe", "coffee", "food", "مطعم", "طعام", "مقهى")) {
            return "Food";
        }
        if (containsAny(value, "transport", "taxi", "bus", "fuel", "مواصلات", "تاكسي", "وقود")) {
            return "Transportation";
        }
        if (containsAny(value, "electricity", "water bill", "utility", "invoice", "فاتورة", "كهرباء")) {
            return "Bills";
        }
        if (containsAny(value, "shopping", "clothing", "mall", "تسوق", "ملابس")) {
            return "Shopping";
        }
        if (containsAny(value, "cinema", "movie", "game", "entertainment", "سينما", "ترفيه")) {
            return "Entertainment";
        }
        if (containsAny(value, "school", "bookstore", "education", "مدرسة", "تعليم", "كتب")) {
            return "Education";
        }
        return "Uncategorized";
    }

    private static String findStoreName(String text) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.length() < 2 || candidate.length() > 48
                    || !candidate.matches("[\\p{L}][\\p{L}0-9 &'.-]*")) {
                continue;
            }
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "receipt", "invoice", "total", "subtotal", "tax", "change",
                    "amount", "date", "cash", "card", "thank you", "فاتورة", "إيصال", "ايصال",
                    "المجموع", "الإجمالي", "الاجمالي", "الضريبة", "الباقي", "شكرا", "شكراً")) {
                continue;
            }
            return candidate;
        }
        return "Receipt";
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String today() { return LocalDate.now().toString(); }

    @Override
    public void close() {
        recognizer.close();
    }

    public interface Callback {
        void onDraftReady(Expense draft, String recognizedText);
        void onFailure(Exception exception);
    }
}
