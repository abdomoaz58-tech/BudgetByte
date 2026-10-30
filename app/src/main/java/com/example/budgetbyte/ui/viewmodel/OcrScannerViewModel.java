package com.example.budgetbyte.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.budgetbyte.R;
import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.ocr.ReceiptScannerHelper;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Screen-owned OCR state. It owns no Activity, View, or database reference. */
public final class OcrScannerViewModel extends AndroidViewModel {
    public enum Mode { RECEIPT, TAX_QR, MULTI_PAGE }
    public static final int MAX_MULTI_PAGE_COUNT = 5;

    public static final class UiState {
        public static final int READY = 0, LOADING = 1, SUCCESS = 2, ERROR = 3;
        public final int status;
        public final Expense draft;
        public final int messageRes;
        public final String recognizedText;

        private UiState(int status, Expense draft, int messageRes, String recognizedText) {
            this.status = status;
            this.draft = draft;
            this.messageRes = messageRes;
            this.recognizedText = recognizedText;
        }

        public static UiState ready() { return new UiState(READY, null, 0, null); }
        private static UiState loading() { return new UiState(LOADING, null, 0, null); }
        private static UiState draft(Expense expense, String raw) {
            return new UiState(SUCCESS, expense, 0, raw);
        }
        private static UiState qr(String raw) { return new UiState(SUCCESS, null, 0, raw); }
        private static UiState error(int resource) { return new UiState(ERROR, null, resource, null); }
    }

    private final SavedStateHandle state;
    private final MutableLiveData<UiState> ui = new MutableLiveData<>(UiState.ready());
    private final MutableLiveData<Expense> preview = new MutableLiveData<>();
    private final MutableLiveData<Integer> pageCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> capturingPage = new MutableLiveData<>(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ReceiptScannerHelper helper = new ReceiptScannerHelper();
    private final BarcodeScanner barcodeScanner = BarcodeScanning.getClient(
            new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());
    private final AtomicBoolean operationInFlight = new AtomicBoolean(false);
    private final AtomicBoolean captureInFlight = new AtomicBoolean(false);
    private final Application application;
    private volatile int generation;

    public OcrScannerViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        this.application = application;
        this.state = state;
        pageCount.setValue(pageUris().size());
    }

    public LiveData<UiState> ui() { return ui; }
    public LiveData<Expense> preview() { return preview; }
    public LiveData<Integer> pageCount() { return pageCount; }
    public LiveData<Boolean> capturingPage() { return capturingPage; }

    public Mode mode() {
        String value = state.get("mode");
        try {
            return value == null ? Mode.RECEIPT : Mode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return Mode.RECEIPT;
        }
    }

    public void setMode(Mode mode) {
        state.set("mode", mode.name());
        if (mode != Mode.MULTI_PAGE) {
            clearPages();
        }
    }

    public boolean torchEnabled() { Boolean value = state.get("torchEnabled"); return Boolean.TRUE.equals(value); }
    public void setTorchEnabled(boolean value) { state.set("torchEnabled", value); }

    public boolean autoCapture() { return false; }
    public void setAutoCapture(boolean ignored) { }

    public boolean beginPageCapture() {
        if (isBusy() || !captureInFlight.compareAndSet(false, true)) {
            return false;
        }
        capturingPage.setValue(true);
        return true;
    }

    public void finishPageCapture() {
        captureInFlight.set(false);
        capturingPage.postValue(false);
    }

    public boolean addPage(@NonNull Uri uri) {
        ArrayList<String> pages = pageUris();
        if (pages.size() >= MAX_MULTI_PAGE_COUNT) {
            return false;
        }
        pages.add(uri.toString());
        state.set("multiPageUris", pages);
        pageCount.setValue(pages.size());
        return true;
    }

    public int pageCountValue() { return pageUris().size(); }

    public void scan(InputImage image, Runnable closeImage) {
        Runnable closeOnce = closeOnce(closeImage);
        if (!beginOperation(closeOnce)) {
            return;
        }
        final int request = generation;
        try {
            executor.execute(() -> helper.scan(image, new ReceiptScannerHelper.Callback() {
                @Override public void onDraftReady(Expense draft, String raw) {
                    if (isCurrent(request)) {
                        preview.postValue(draft);
                        ui.postValue(UiState.draft(draft, raw));
                    }
                    finishOperation(closeOnce, request);
                }

                @Override public void onFailure(Exception exception) {
                    if (isCurrent(request)) {
                        ui.postValue(UiState.error(R.string.scanner_error));
                    }
                    finishOperation(closeOnce, request);
                }
            }));
        } catch (RuntimeException exception) {
            finishOperation(closeOnce, request);
            ui.postValue(UiState.error(R.string.scanner_error));
        }
    }

    public void scanQr(InputImage image, Runnable closeImage) {
        Runnable closeOnce = closeOnce(closeImage);
        if (!beginOperation(closeOnce)) {
            return;
        }
        final int request = generation;
        try {
            executor.execute(() -> barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!isCurrent(request)) {
                            finishOperation(closeOnce, request);
                            return;
                        }
                        String raw = null;
                        for (Barcode barcode : barcodes) {
                            if (barcode.getFormat() == Barcode.FORMAT_QR_CODE
                                    && barcode.getRawValue() != null) {
                                raw = barcode.getRawValue();
                                break;
                            }
                        }
                        if (raw == null) {
                            ui.postValue(UiState.error(R.string.scanner_qr_not_found));
                        } else {
                            ui.postValue(UiState.qr(raw));
                        }
                        finishOperation(closeOnce, request);
                    })
                    .addOnFailureListener(exception -> {
                        if (isCurrent(request)) {
                            ui.postValue(UiState.error(R.string.scanner_error));
                        }
                        finishOperation(closeOnce, request);
                    }));
        } catch (RuntimeException exception) {
            finishOperation(closeOnce, request);
            ui.postValue(UiState.error(R.string.scanner_error));
        }
    }

    public void scanCollectedPages() {
        ArrayList<String> pages = pageUris();
        if (pages.isEmpty() || !operationInFlight.compareAndSet(false, true)) {
            return;
        }
        final int request = ++generation;
        ui.postValue(UiState.loading());
        scanPageAt(pages, 0, new ArrayList<>(), request);
    }

    public void consumeResult() {
        UiState current = ui.getValue();
        if (current != null && (current.status == UiState.SUCCESS || current.status == UiState.ERROR)) {
            ui.setValue(UiState.ready());
        }
    }

    private void scanPageAt(ArrayList<String> pages, int index, ArrayList<String> recognized,
                            int request) {
        if (!isCurrent(request)) {
            return;
        }
        if (index >= pages.size()) {
            String combined = joinPages(recognized);
            try {
                Expense draft = ReceiptScannerHelper.parseDraft(combined);
                preview.postValue(draft);
                ui.postValue(UiState.draft(draft, combined));
            } catch (IllegalArgumentException exception) {
                ui.postValue(UiState.error(R.string.scanner_error));
            }
            cleanupPageFiles(pages);
            clearPages();
            operationInFlight.set(false);
            return;
        }

        Uri uri = Uri.parse(pages.get(index));
        try {
            executor.execute(() -> {
                try {
                    helper.scan(application, uri, new ReceiptScannerHelper.Callback() {
                        @Override public void onDraftReady(Expense ignoredDraft, String raw) {
                            recognized.add(raw == null ? "" : raw);
                            scanPageAt(pages, index + 1, recognized, request);
                        }

                        @Override public void onFailure(Exception exception) {
                            if (isCurrent(request)) {
                                ui.postValue(UiState.error(R.string.scanner_error));
                                cleanupPageFiles(pages);
                                clearPages();
                                operationInFlight.set(false);
                            }
                        }
                    });
                } catch (RuntimeException exception) {
                    if (isCurrent(request)) {
                        ui.postValue(UiState.error(R.string.scanner_error));
                        cleanupPageFiles(pages);
                        clearPages();
                        operationInFlight.set(false);
                    }
                }
            });
        } catch (RuntimeException exception) {
            ui.postValue(UiState.error(R.string.scanner_error));
            cleanupPageFiles(pages);
            clearPages();
            operationInFlight.set(false);
        }
    }

    private String joinPages(List<String> values) {
        StringBuilder combined = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (combined.length() > 0) {
                combined.append('\n');
            }
            combined.append(value.trim());
        }
        return combined.toString();
    }

    private boolean beginOperation(Runnable closeImage) {
        if (!operationInFlight.compareAndSet(false, true)) {
            closeImage.run();
            return false;
        }
        generation++;
        ui.postValue(UiState.loading());
        return true;
    }

    private Runnable closeOnce(Runnable closeAction) {
        AtomicBoolean closed = new AtomicBoolean(false);
        return () -> {
            if (closed.compareAndSet(false, true)) {
                closeAction.run();
            }
        };
    }

    private void finishOperation(Runnable closeImage, int request) {
        closeImage.run();
        if (isCurrent(request)) {
            operationInFlight.set(false);
        }
    }

    private boolean isCurrent(int request) {
        return request == generation;
    }

    private boolean isBusy() {
        return operationInFlight.get() || captureInFlight.get();
    }

    private ArrayList<String> pageUris() {
        ArrayList<String> pages = state.get("multiPageUris");
        return pages == null ? new ArrayList<>() : new ArrayList<>(pages);
    }

    public void clearPages() {
        cleanupPageFiles(pageUris());
        state.set("multiPageUris", new ArrayList<String>());
        pageCount.setValue(0);
    }

    private void cleanupPageFiles(List<String> uris) {
        for (String value : uris) {
            Uri uri = Uri.parse(value);
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                if (file.getParentFile() != null
                        && file.getParentFile().getName().equals("ocr-multipage")) {
                    // Only scanner-owned temporary page files are deleted here.
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }

    public boolean isProcessing() {
        return isBusy();
    }

    @Override
    protected void onCleared() {
        generation++;
        clearPages();
        helper.close();
        barcodeScanner.close();
        executor.shutdownNow();
    }
}
