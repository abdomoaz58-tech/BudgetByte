package com.example.budgetbyte.util;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/** Presents a biometric/device-credential prompt without granting access on failure. */
public final class BiometricHelper {
    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private BiometricHelper() { }

    public static boolean canAuthenticate(FragmentActivity activity) {
        return BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticate(FragmentActivity activity, AuthenticationCallback callback) {
        BiometricPrompt prompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onAuthenticated();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errorMessage) {
                        super.onAuthenticationError(errorCode, errorMessage);
                        callback.onError(errorMessage);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onFailed();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock BudgetByte")
                .setSubtitle("Confirm your identity to view financial data")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build();
        prompt.authenticate(promptInfo);
    }

    public interface AuthenticationCallback {
        void onAuthenticated();
        void onFailed();
        void onError(CharSequence errorMessage);
    }
}
