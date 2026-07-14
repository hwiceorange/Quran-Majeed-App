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
     * Check if current user is anonymous
     * @return true if user is signed in anonymously, false otherwise
     */
    public boolean isAnonymous() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isAnonymous();
    }
    
    /**
     * Sign in anonymously
     * Allows users to use the app without Google account
     * Data is saved to Firestore with anonymous UID
     * @param callback Callback for authentication result
     */
    public void signInAnonymously(AuthCallback callback) {
        Log.d(TAG, "🔓 Attempting anonymous sign-in...");
        
        // Check if already signed in (including anonymous)
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isAnonymous()) {
                Log.d(TAG, "✅ Already signed in anonymously: " + currentUser.getUid());
                callback.onSuccess(currentUser);
            } else {
                Log.d(TAG, "✅ Already signed in with Google: " + currentUser.getEmail());
                callback.onSuccess(currentUser);
            }
            return;
        }
        
        // Sign in anonymously
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "✅ Anonymous sign-in successful");
                            Log.d(TAG, "   → User ID: " + user.getUid());
                            Log.d(TAG, "   → Is Anonymous: " + user.isAnonymous());
                            callback.onSuccess(user);
                        } else {
                            Log.e(TAG, "❌ Anonymous sign-in succeeded but user is null");
                            callback.onFailure("Anonymous sign-in succeeded but user is null");
                        }
                    } else {
                        Log.e(TAG, "❌ Anonymous sign-in failed", task.getException());
                        callback.onFailure("Anonymous sign-in failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }
    
    /**
     * Link anonymous account with Google account
     * This preserves all data created during anonymous session
     * Must be called on an anonymous account
     * @param data Intent data from Google Sign-In
     * @param callback Callback for linking result
     */
    @SuppressWarnings("deprecation")
    public void linkAnonymousWithGoogle(Intent data, AuthCallback callback) {
        Log.d(TAG, "🔗 Attempting to link anonymous account with Google...");
        
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ No current user to link");
            callback.onFailure("No user is currently signed in");
            return;
        }
        
        if (!currentUser.isAnonymous()) {
            Log.w(TAG, "⚠️ Current user is not anonymous, performing regular sign-in");
            handleSignInResult(data, callback);
            return;
        }
        
        if (data == null) {
            Log.e(TAG, "❌ Intent data is null");
            callback.onFailure("Sign-in data is missing");
            return;
        }
        
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            
            if (account != null) {
                Log.d(TAG, "✅ GoogleSignInAccount retrieved");
                Log.d(TAG, "   → Email: " + account.getEmail());
                Log.d(TAG, "   → Display Name: " + account.getDisplayName());
                
                if (account.getIdToken() == null || account.getIdToken().isEmpty()) {
                    Log.e(TAG, "❌ ID Token is missing");
                    callback.onFailure("Authentication token is missing");
                    return;
                }
                
                // Create Google credential
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                
                // Link credential to anonymous account
                Log.d(TAG, "→ Linking Google credential to anonymous account...");
                currentUser.linkWithCredential(credential)
                        .addOnCompleteListener(linkTask -> {
                            if (linkTask.isSuccessful()) {
                                FirebaseUser linkedUser = linkTask.getResult().getUser();
                                if (linkedUser != null) {
                                    Log.d(TAG, "✅ Account linking successful!");
                                    Log.d(TAG, "   → User ID (unchanged): " + linkedUser.getUid());
                                    Log.d(TAG, "   → Email (new): " + linkedUser.getEmail());
                                    Log.d(TAG, "   → Is Anonymous: " + linkedUser.isAnonymous());
                                    Log.d(TAG, "   → ✨ All anonymous data is preserved!");
                                    callback.onSuccess(linkedUser);
                                } else {
                                    Log.e(TAG, "❌ Linking succeeded but user is null");
                                    callback.onFailure("Linking succeeded but user is null");
                                }
                            } else {
                                Exception exception = linkTask.getException();
                                Log.e(TAG, "❌ Account linking failed", exception);
                                
                                // Check if credential is already in use
                                if (exception != null && exception.getMessage() != null && 
                                    exception.getMessage().contains("already in use")) {
                                    Log.w(TAG, "⚠️ This Google account is already linked to another account");
                                    callback.onFailure("This Google account is already in use. Please use a different account.");
                                } else {
                                    callback.onFailure("Failed to link accounts: " + 
                                            (exception != null ? exception.getMessage() : "Unknown error"));
                                }
                            }
                        });
            } else {
                Log.e(TAG, "❌ GoogleSignInAccount is null");
                callback.onFailure("Failed to retrieve account information");
            }
        } catch (ApiException e) {
            Log.e(TAG, "❌ Google Sign-In failed with ApiException", e);
            Log.e(TAG, "   → Status Code: " + e.getStatusCode());
            Log.e(TAG, "   → Status Message: " + e.getStatusMessage());
            
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
            Log.e(TAG, "❌ Unexpected error during account linking", e);
            callback.onFailure("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Get user display name
     * @return User's display name, "Guest User" for anonymous, or empty string
     */
    @NonNull
    public String getUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return "";
        }
        
        // Anonymous user
        if (user.isAnonymous()) {
            return context.getString(com.quran.quranaudio.online.R.string.guest_user);
        }
        
        // Google user with display name
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        
        // Fallback to email prefix
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            return user.getEmail().split("@")[0];
        }
        
        return "";
    }
    
    /**
     * Get user email
     * @return User's email, "anonymous@guest.com" for anonymous, or empty string
     */
    @NonNull
    public String getUserEmail() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return "";
        }
        
        // Anonymous user
        if (user.isAnonymous()) {
            return "anonymous@guest.com";
        }
        
        // Google user with email
        return user.getEmail() != null ? user.getEmail() : "";
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

