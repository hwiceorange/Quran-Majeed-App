package com.quran.quranaudio.online.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Google Authentication Manager
 * Handles Google Sign-In and Sign-Out functionality
 */
public class GoogleAuthManager {
    private static final String TAG = "GoogleAuthManager";
    
    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final GoogleSignInClient googleSignInClient;
    
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }
    
    @SuppressWarnings("deprecation")
    public GoogleAuthManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        
        // Check Google Play Services availability first
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int status = googleAPI.isGooglePlayServicesAvailable(context);
        
        if (status != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available: " + status);
            if (googleAPI.isUserResolvableError(status)) {
                Log.w(TAG, "Google Play Services error is user-resolvable");
            }
        } else {
            Log.d(TAG, "Google Play Services is available and up to date");
        }
        
        // Configure Google Sign-In with Web Client ID from google-services.json
        // This ensures the Client ID is always synchronized with Firebase configuration
        String webClientId = context.getString(com.quran.quranaudio.online.R.string.default_web_client_id);
        Log.d(TAG, "========== Google Sign-In Configuration ==========");
        Log.d(TAG, "📝 Package Name: " + context.getPackageName());
        Log.d(TAG, "📝 Web Client ID: " + webClientId);
        Log.d(TAG, "📝 Web Client ID length: " + (webClientId != null ? webClientId.length() : 0));
        
        // Check Firebase Auth state
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        Log.d(TAG, "🔐 Current Firebase User: " + (currentUser != null ? currentUser.getEmail() : "null"));
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        
        this.googleSignInClient = GoogleSignIn.getClient(context, gso);
        
        // Check last signed-in account
        GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(context);
        Log.d(TAG, "📱 Last Google Account: " + (lastAccount != null ? lastAccount.getEmail() : "null"));
        Log.d(TAG, "✅ GoogleSignInClient initialized successfully");
        Log.d(TAG, "==================================================");
    }
    
    /**
     * Get the current signed-in user
     * @return FirebaseUser or null if not signed in
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Check if user is currently signed in
     * @return true if signed in, false otherwise
     */
    public boolean isUserSignedIn() {
        return getCurrentUser() != null;
    }
    
    /**
     * Get user display name
     * @return User's display name or empty string
     */
    @NonNull
    public String getUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.getDisplayName() != null ? user.getDisplayName() : "";
    }
    
    /**
     * Get user email
     * @return User's email or empty string
     */
    @NonNull
    public String getUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.getEmail() != null ? user.getEmail() : "";
    }
    
    /**
     * Get user photo URL
     * @return User's photo URI or null
     */
    @Nullable
    public Uri getUserPhotoUrl() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getPhotoUrl() : null;
    }
    
    /**
     * Get the Google Sign-In intent
     * Launch this intent using ActivityResultLauncher
     * @return Intent for Google Sign-In
     */
    public Intent getSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }
    
    /**
     * Log detailed diagnostics for Google Sign-In failures
     * Useful when Activity.RESULT_CANCELED is returned but intent carries error info
     */
    @SuppressWarnings("deprecation")
    public void logSignInDiagnostics(@Nullable Intent data, @NonNull String sourceTag) {
        Log.d(TAG, "[" + sourceTag + "] logSignInDiagnostics invoked");
        if (data == null) {
            Log.e(TAG, "[" + sourceTag + "] Sign-in intent data is null");
            return;
        }
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            Log.w(TAG, "[" + sourceTag + "] Diagnostics retrieved account unexpectedly: "
                    + (account != null ? account.getEmail() : "null"));
        } catch (ApiException e) {
            String statusCodeString = GoogleSignInStatusCodes.getStatusCodeString(e.getStatusCode());
            Log.e(TAG, "[" + sourceTag + "] Google Sign-In ApiException captured");
            Log.e(TAG, "[" + sourceTag + "]   - Status Code: " + e.getStatusCode() + " (" + statusCodeString + ")");
            Log.e(TAG, "[" + sourceTag + "]   - Status Message: " + e.getStatusMessage());
            Log.e(TAG, "[" + sourceTag + "]   - Message: " + e.getMessage());
            Log.e(TAG, "[" + sourceTag + "]   - Cause: " + e.getCause());
            Status status = e.getStatus();
            if (status != null) {
                Log.e(TAG, "[" + sourceTag + "]   - Status: " + status);
            }
        }
        Bundle extras = data.getExtras();
        if (extras != null && !extras.isEmpty()) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, "[" + sourceTag + "]   - Extra: " + key + " => " + value);
            }
        } else {
            Log.d(TAG, "[" + sourceTag + "]   - No extras found in intent");
        }
    }
    
    /**
     * Handle the result from Google Sign-In activity
     * Call this in your onActivityResult or ActivityResultCallback
     * Enhanced with detailed logging and error handling
     * @param data Intent data from the result
     * @param callback Callback for authentication result
     */
    @SuppressWarnings("deprecation")
    public void handleSignInResult(Intent data, AuthCallback callback) {
        Log.d(TAG, "handleSignInResult() called");
        
        if (data == null) {
            Log.e(TAG, "Intent data is null");
            callback.onFailure("Sign-in data is missing");
            return;
        }
        
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        Log.d(TAG, "Task created from intent");
        
        try {
            // Attempt to get the account from the task
            GoogleSignInAccount account = task.getResult(ApiException.class);
            
            if (account != null) {
                Log.d(TAG, "GoogleSignInAccount retrieved successfully");
                Log.d(TAG, "  - Display Name: " + account.getDisplayName());
                Log.d(TAG, "  - Email: " + account.getEmail());
                Log.d(TAG, "  - ID: " + account.getId());
                Log.d(TAG, "  - Photo URL: " + account.getPhotoUrl());
                Log.d(TAG, "  - ID Token: " + (account.getIdToken() != null ? "Present" : "NULL"));
                
                // Check if ID Token is present (required for Firebase auth)
                if (account.getIdToken() == null || account.getIdToken().isEmpty()) {
                    Log.e(TAG, "ID Token is missing! Check Firebase configuration");
                    callback.onFailure("Authentication token is missing. Please check Firebase configuration.");
                    return;
                }
                
                firebaseAuthWithGoogle(account, callback);
            } else {
                Log.e(TAG, "GoogleSignInAccount is null after task completion");
                callback.onFailure("Failed to retrieve account information");
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In failed with ApiException", e);
            Log.e(TAG, "  - Status Code: " + e.getStatusCode());
            Log.e(TAG, "  - Status Message: " + e.getStatusMessage());
            Log.e(TAG, "  - Error Message: " + e.getMessage());
            
            // Provide user-friendly error messages
            String errorMessage;
            switch (e.getStatusCode()) {
                case 12501:  // SIGN_IN_CANCELLED
                    errorMessage = "Sign-in was canceled";
                    break;
                case 12500:  // SIGN_IN_FAILED
                    errorMessage = "Sign-in failed. Please try again.";
                    break;
                case 7:      // NETWORK_ERROR
                    errorMessage = "Network error. Please check your connection.";
                    break;
                default:
                    errorMessage = "Sign-in error (Code: " + e.getStatusCode() + ")";
            }
            
            callback.onFailure(errorMessage);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during sign-in", e);
            callback.onFailure("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Authenticate with Firebase using Google credentials
     * @param account GoogleSignInAccount
     * @param callback Callback for authentication result
     */
    @SuppressWarnings("deprecation")
    private void firebaseAuthWithGoogle(GoogleSignInAccount account, AuthCallback callback) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
        
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("Firebase authentication succeeded but user is null");
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        callback.onFailure("Firebase authentication failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }
    
    /**
     * Sign out the current user
     * @param callback Callback when sign-out is complete
     */
    public void signOut(Runnable callback) {
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d(TAG, "User signed out");
            if (callback != null) {
                callback.run();
            }
        });
    }
    
    /**
     * Revoke access (sign out and revoke permissions)
     * @param callback Callback when revoke is complete
     */
    public void revokeAccess(Runnable callback) {
        firebaseAuth.signOut();
        googleSignInClient.revokeAccess().addOnCompleteListener(task -> {
            Log.d(TAG, "User access revoked");
            if (callback != null) {
                callback.run();
            }
        });
    }
}

