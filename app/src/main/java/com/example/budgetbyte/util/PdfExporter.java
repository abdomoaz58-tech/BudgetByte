package com.example.budgetbyte.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.budgetbyte.data.entity.Expense;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Creates PDF reports only in the FileProvider-scoped cache/reports directory. */
public final class PdfExporter {
    private static final String REPORT_DIRECTORY = "reports";
    private static final String AUTHORITY_SUFFIX = ".fileprovider";

    private PdfExporter() { }

    public static File exportMonthlyReport(Context context, String yearMonth, List<Expense> expenses)
            throws IOException {
        File reportsDirectory = new File(context.getCacheDir(), REPORT_DIRECTORY);
        if (!reportsDirectory.exists() && !reportsDirectory.mkdirs()) {
            throw new IOException("Unable to create report directory");
        }
        String safeMonth = yearMonth == null ? "report" : yearMonth.replaceAll("[^0-9-]", "_");
        File output = new File(reportsDirectory, "budgetbyte-" + safeMonth + ".pdf");
        PdfDocument document = new PdfDocument();
        try {
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setTextSize(20f);
            titlePaint.setFakeBoldText(true);
            Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setTextSize(11f);
            int pageNumber = 1;
            PdfDocument.Page page = startPage(document, pageNumber);
            Canvas canvas = page.getCanvas();
            drawHeader(canvas, titlePaint, bodyPaint, yearMonth);
            float y = 88f;
            long totalIncome = 0L;
            long totalExpense = 0L;

            for (Expense expense : expenses) {
                if (y > 780f) {
                    document.finishPage(page);
                    page = startPage(document, ++pageNumber);
                    canvas = page.getCanvas();
                    drawHeader(canvas, titlePaint, bodyPaint, yearMonth);
                    y = 88f;
                }
                boolean isIncome = expense.isIncome();
                long amount = expense.getAmount();
                if (isIncome) {
                    totalIncome = Math.addExact(totalIncome, amount);
                } else {
                    totalExpense = Math.addExact(totalExpense, amount);
                }

                String sign = isIncome ? "+" : "-";
                String row = safe(expense.date) + "  " + safe(expense.title) + "  ["
                        + CategoryUtils.getLocalizedName(context, expense.category) + "]  "
                        + sign + MoneyUtils.formatAmount(amount, "EGP");
                canvas.drawText(row, 36f, y, bodyPaint);
                y += 22f;
            }

            if (y > 730f) {
                document.finishPage(page);
                page = startPage(document, ++pageNumber);
                canvas = page.getCanvas();
                y = 56f;
            }

            bodyPaint.setFakeBoldText(true);
            y += 10f;
            canvas.drawText("Total Income: +" + MoneyUtils.formatAmount(totalIncome, "EGP"), 36f, y, bodyPaint);
            y += 20f;
            canvas.drawText("Total Expenses: -" + MoneyUtils.formatAmount(totalExpense, "EGP"), 36f, y, bodyPaint);
            y += 20f;
            long net = totalIncome - totalExpense;
            String netSign = net >= 0 ? "+" : "";
            canvas.drawText("Net Balance: " + netSign + MoneyUtils.formatAmount(net, "EGP"), 36f, y, bodyPaint);

            document.finishPage(page);
            try (FileOutputStream outputStream = new FileOutputStream(output)) {
                document.writeTo(outputStream);
            }
            return output;
        } finally {
            document.close();
        }
    }

    public static Uri getShareUri(Context context, File report) {
        return FileProvider.getUriForFile(context, context.getPackageName() + AUTHORITY_SUFFIX, report);
    }

    public static Intent createShareIntent(Context context, File report) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, getShareUri(context, report))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(intent, "Share monthly report");
    }

    private static PdfDocument.Page startPage(PdfDocument document, int pageNumber) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
        return document.startPage(pageInfo);
    }

    private static void drawHeader(Canvas canvas, Paint titlePaint, Paint bodyPaint, String yearMonth) {
        canvas.drawText("BudgetByte Monthly Report", 36f, 42f, titlePaint);
        canvas.drawText("Month: " + safe(yearMonth), 36f, 66f, bodyPaint);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
