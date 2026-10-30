package com.example.budgetbyte.ui.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.budgetbyte.R;
import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.databinding.ActivityOcrScannerBinding;
import com.example.budgetbyte.ui.viewmodel.OcrScannerViewModel;
import com.example.budgetbyte.util.MoneyUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** CameraX entry point. Scanner results are drafts/raw QR values; this Fragment never writes Room. */
public final class OcrScannerFragment extends Fragment {
    private static final String OCR_READY = "addExpense.ocrDraftReady";
    private static final String OCR_TITLE = "addExpense.ocrTitle";
    private static final String OCR_AMOUNT = "addExpense.ocrAmount";
    private static final String OCR_CATEGORY = "addExpense.ocrCategory";
    private static final String OCR_DATE = "addExpense.ocrDate";
    private static final String OCR_TYPE = "addExpense.ocrType";

    private ActivityOcrScannerBinding binding;
    private OcrScannerViewModel model;
    private ImageCapture capture;
    private Camera activeCamera;
    private ProcessCameraProvider cameraProvider;
    private boolean cameraStarting;
    private boolean captureWhenCameraReady;

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding == null) return;
                if (granted) {
                    startCamera(true);
                } else {
                    binding.scannerFlashBtn.setEnabled(false);
                    showError(R.string.camera_denied);
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> gallery = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && binding != null) handleGalleryImage(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        binding = ActivityOcrScannerBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(this,
                new SavedStateViewModelFactory(requireActivity().getApplication(), this))
                .get(OcrScannerViewModel.class);

        binding.scannerToolbar.setNavigationOnClickListener(
                ignored -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnGallery.setOnClickListener(ignored -> gallery.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.btnManual.setOnClickListener(ignored -> openDraft(null));
        binding.shutterBtn.setOnClickListener(ignored -> takePicture());
        binding.scannerFlashBtn.setOnClickListener(ignored -> toggleTorch());
        binding.btnFinishMultiPage.setOnClickListener(ignored -> model.scanCollectedPages());
        binding.scannerTabs.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            OcrScannerViewModel.Mode mode = id == R.id.tabTaxQR
                    ? OcrScannerViewModel.Mode.TAX_QR
                    : id == R.id.tabMulti ? OcrScannerViewModel.Mode.MULTI_PAGE
                    : OcrScannerViewModel.Mode.RECEIPT;
            model.setMode(mode);
            renderMode();
        });
        binding.scannerTabs.check(modeButton(model.mode()));
        binding.toggleAuto.setVisibility(View.GONE);

        model.preview().observe(getViewLifecycleOwner(), this::renderPreview);
        model.ui().observe(getViewLifecycleOwner(), this::renderState);
        model.pageCount().observe(getViewLifecycleOwner(), this::renderPageCount);
        model.capturingPage().observe(getViewLifecycleOwner(), ignored -> renderBusyState());

        renderMode();
        renderBusyState();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera(false);
        }
    }

    private int modeButton(OcrScannerViewModel.Mode mode) {
        if (mode == OcrScannerViewModel.Mode.TAX_QR) return R.id.tabTaxQR;
        if (mode == OcrScannerViewModel.Mode.MULTI_PAGE) return R.id.tabMulti;
        return R.id.tabReceipt;
    }

    private void renderMode() {
        boolean multiPage = model.mode() == OcrScannerViewModel.Mode.MULTI_PAGE;
        binding.btnFinishMultiPage.setVisibility(
                multiPage && model.pageCountValue() > 0 ? View.VISIBLE : View.GONE);
        renderPageCount(model.pageCountValue());
    }

    private void renderBusyState() {
        if (binding == null || model == null) return;
        OcrScannerViewModel.UiState state = model.ui().getValue();
        boolean processing = state != null && state.status == OcrScannerViewModel.UiState.LOADING;
        boolean capturing = Boolean.TRUE.equals(model.capturingPage().getValue());
        boolean pageLimitReached = model.mode() == OcrScannerViewModel.Mode.MULTI_PAGE
                && model.pageCountValue() >= OcrScannerViewModel.MAX_MULTI_PAGE_COUNT;
        binding.shutterBtn.setEnabled(!processing && !capturing && !pageLimitReached);
        binding.btnGallery.setEnabled(!processing && !capturing);
        binding.btnManual.setEnabled(!processing && !capturing);
        binding.scannerTabs.setEnabled(!processing && !capturing);
        binding.btnFinishMultiPage.setEnabled(!processing && !capturing);
    }

    private void renderState(@Nullable OcrScannerViewModel.UiState state) {
        renderBusyState();
        if (state == null) return;
        if (state.status == OcrScannerViewModel.UiState.ERROR) {
            model.consumeResult();
            showError(state.messageRes);
        } else if (state.status == OcrScannerViewModel.UiState.SUCCESS) {
            model.consumeResult();
            if (state.draft != null) {
                showFeedback(getString(R.string.scanner_ready));
                openDraft(state.draft);
            } else if (state.recognizedText != null) {
                String raw = state.recognizedText.length() > 700
                        ? state.recognizedText.substring(0, 700) + "…"
                        : state.recognizedText;
                showFeedback(getString(R.string.scanner_qr_result, raw));
            }
        }
    }

    private void startCamera(boolean captureAfterStart) {
        if (binding == null || cameraStarting) return;
        if (activeCamera != null) {
            if (captureAfterStart) takePicture();
            return;
        }

        cameraStarting = true;
        captureWhenCameraReady = captureAfterStart;
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (binding == null) {
                cameraStarting = false;
                return;
            }
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.scannerPreview.getSurfaceProvider());
                capture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                cameraProvider.unbindAll();
                activeCamera = cameraProvider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, capture);

                boolean hasTorch = activeCamera.getCameraInfo().hasFlashUnit();
                binding.scannerFlashBtn.setEnabled(hasTorch);
                if (!hasTorch && model.torchEnabled()) {
                    model.setTorchEnabled(false);
                    showError(R.string.scanner_torch_unavailable);
                }
                renderTorchState(model.torchEnabled());
                if (hasTorch && model.torchEnabled()) applyTorch(true);

                cameraStarting = false;
                if (captureWhenCameraReady) {
                    captureWhenCameraReady = false;
                    takePicture();
                }
            } catch (Exception exception) {
                cameraStarting = false;
                captureWhenCameraReady = false;
                showError(R.string.camera_denied);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePicture() {
        if (model.isProcessing()) return;
        if (model.mode() == OcrScannerViewModel.Mode.MULTI_PAGE) {
            capturePageToFile();
            return;
        }
        if (activeCamera == null || capture == null) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startCamera(true);
            } else {
                permission.launch(Manifest.permission.CAMERA);
            }
            return;
        }
        captureCurrentImage();
    }

    private void captureCurrentImage() {
        if (capture == null || model.isProcessing()) return;
        capture.takePicture(ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy proxy) {
                        if (binding == null || proxy.getImage() == null) {
                            proxy.close();
                            showError(R.string.scanner_error);
                            return;
                        }
                        InputImage image = InputImage.fromMediaImage(proxy.getImage(),
                                proxy.getImageInfo().getRotationDegrees());
                        dispatchImage(image, proxy::close);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        showError(R.string.scanner_error);
                    }
                });
    }

    private void dispatchImage(InputImage image, Runnable closeImage) {
        if (model.mode() == OcrScannerViewModel.Mode.TAX_QR) {
            model.scanQr(image, closeImage);
        } else {
            model.scan(image, closeImage);
        }
    }

    private void handleGalleryImage(@NonNull Uri uri) {
        if (model.isProcessing()) return;
        if (model.mode() == OcrScannerViewModel.Mode.MULTI_PAGE) {
            try {
                requireContext().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Photo Picker providers may grant access without persistable permission.
            }
            addPage(uri);
            return;
        }
        try {
            dispatchImage(InputImage.fromFilePath(requireContext(), uri), () -> { });
        } catch (Exception exception) {
            showError(R.string.scanner_error);
        }
    }

    private void capturePageToFile() {
        if (capture == null) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startCamera(true);
            } else {
                permission.launch(Manifest.permission.CAMERA);
            }
            return;
        }
        if (!model.beginPageCapture()) return;

        File file;
        try {
            File directory = new File(requireContext().getCacheDir(), "ocr-multipage");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create scanner cache directory");
            }
            file = File.createTempFile("page-", ".jpg", directory);
        } catch (IOException exception) {
            model.finishPageCapture();
            showError(R.string.scanner_error);
            return;
        }

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();
        capture.takePicture(options, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        addPage(Uri.fromFile(file));
                        model.finishPageCapture();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        model.finishPageCapture();
                        showError(R.string.scanner_error);
                    }
                });
    }

    private void addPage(@NonNull Uri uri) {
        if (model.addPage(uri)) {
            showFeedback(getString(R.string.scanner_page_added, model.pageCountValue(),
                    OcrScannerViewModel.MAX_MULTI_PAGE_COUNT));
            renderPageCount(model.pageCountValue());
        } else {
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                if (file.getParentFile() != null
                        && "ocr-multipage".equals(file.getParentFile().getName())) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
            showError(R.string.scanner_no_more_pages, OcrScannerViewModel.MAX_MULTI_PAGE_COUNT);
        }
    }

    private void renderPageCount(@Nullable Integer count) {
        if (binding == null || model == null) return;
        int pages = count == null ? 0 : count;
        boolean multiPage = model.mode() == OcrScannerViewModel.Mode.MULTI_PAGE;
        binding.btnFinishMultiPage.setVisibility(multiPage && pages > 0 ? View.VISIBLE : View.GONE);
        binding.btnFinishMultiPage.setText(getString(R.string.scanner_finish_pages, pages));
        renderBusyState();
    }

    private void toggleTorch() {
        if (activeCamera == null || !activeCamera.getCameraInfo().hasFlashUnit()) {
            binding.scannerFlashBtn.setEnabled(false);
            showError(R.string.scanner_torch_unavailable);
            return;
        }
        applyTorch(!model.torchEnabled());
    }

    private void applyTorch(boolean enabled) {
        Camera camera = activeCamera;
        if (camera == null) return;
        ListenableFuture<Void> result = camera.getCameraControl().enableTorch(enabled);
        result.addListener(() -> {
            try {
                result.get();
                if (enabled && camera.getCameraInfo().getTorchState().getValue() != null
                        && camera.getCameraInfo().getTorchState().getValue() != TorchState.ON) {
                    model.setTorchEnabled(false);
                    renderTorchState(false);
                    showError(R.string.scanner_torch_unavailable);
                    return;
                }
                model.setTorchEnabled(enabled);
                renderTorchState(enabled);
            } catch (Exception exception) {
                model.setTorchEnabled(false);
                renderTorchState(false);
                if (enabled) showError(R.string.scanner_torch_unavailable);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void renderTorchState(boolean enabled) {
        if (binding == null) return;
        binding.scannerFlashBtn.setSelected(enabled);
        binding.scannerFlashBtn.setImageResource(
                enabled ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
    }

    private void renderPreview(@Nullable Expense draft) {
        if (binding == null) return;
        String missing = getString(R.string.not_detected);
        binding.liveStore.setText(getString(R.string.scanner_store,
                draft == null ? missing : draft.title));
        binding.liveCategory.setText(getString(R.string.scanner_category,
                draft == null ? missing : draft.category));
        binding.liveAmount.setText(getString(R.string.scanner_amount,
                draft == null ? missing : MoneyUtils.formatAmount(draft.amount)));
        binding.liveDate.setText(getString(R.string.scanner_date,
                draft == null ? missing : displayDate(draft.date)));
    }

    private String displayDate(String raw) {
        try {
            Date value = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(raw);
            return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(value);
        } catch (ParseException exception) {
            return raw;
        }
    }

    private final android.os.Handler feedbackHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable hideFeedback = () -> {
        if (binding != null) {
            binding.scannerFeedbackToast.setVisibility(View.GONE);
        }
    };

    private void showFeedback(@NonNull String message) {
        if (binding == null) return;
        feedbackHandler.removeCallbacks(hideFeedback);
        binding.scannerFeedbackToast.setText(message);
        binding.scannerFeedbackToast.setVisibility(View.VISIBLE);
        // Auto-hide after 3 seconds so the user doesn't have to dismiss it manually.
        feedbackHandler.postDelayed(hideFeedback, 3000L);
    }

    private void showError(int message) {
        if (binding != null) showFeedback(getString(message));
    }

    private void showError(int message, int argument) {
        if (binding != null) showFeedback(getString(message, argument));
    }

    private void openDraft(@Nullable Expense draft) {
        NavController controller = NavHostFragment.findNavController(this);
        NavBackStackEntry previous = controller.getPreviousBackStackEntry();
        if (previous != null && previous.getDestination().getId() == R.id.addExpenseFragment) {
            if (draft != null) {
                SavedStateHandle result = previous.getSavedStateHandle();
                result.set(OCR_TITLE, draft.title);
                result.set(OCR_AMOUNT, draft.amount);
                result.set(OCR_CATEGORY, draft.category);
                result.set(OCR_DATE, draft.date);
                result.set(OCR_TYPE, draft.type);
                result.set(OCR_READY, true);
            }
            controller.popBackStack();
            return;
        }

        Bundle args = new Bundle();
        if (draft != null) {
            args.putString("title", draft.title);
            args.putLong("amount", draft.amount);
            args.putString("category", draft.category);
            args.putString("date", draft.date);
            args.putString("type", draft.type);
            args.putString("transactionType", draft.type);
        }
        controller.navigate(R.id.action_global_addExpenseFragment, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (model != null && activeCamera != null && model.torchEnabled()) applyTorch(true);
    }

    @Override
    public void onStop() {
        if (activeCamera != null) activeCamera.getCameraControl().enableTorch(false);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (cameraProvider != null) cameraProvider.unbindAll();
        activeCamera = null;
        capture = null;
        cameraStarting = false;
        binding = null;
        super.onDestroyView();
    }
}