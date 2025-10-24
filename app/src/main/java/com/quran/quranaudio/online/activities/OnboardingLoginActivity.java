package com.quran.quranaudio.online.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseUser;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.Utils.GoogleAuthManager;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;

/**
 * Onboarding Login Activity
 * Shown only once to new users after splash screen
 * Allows Google login or skip to main app
 */
public class OnboardingLoginActivity extends AppCompatActivity {
    
    private static final String TAG = "OnboardingLogin";
    private static final String PREF_NAME = "OnboardingPrefs";
    private static final String KEY_HAS_SHOWN_LOGIN = "has_shown_login_screen";
    
    private GoogleAuthManager googleAuthManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    private CardView btnGoogleSignIn;
    private TextView btnSkip;
    private TextView tvPrivacyPolicy;
    
    private boolean isLoggingIn = false; // Prevent double-tap
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_login);
        
        // Initialize views
        initViews();
        
        // Initialize Google Auth Manager
        googleAuthManager = new GoogleAuthManager(this);
        
        // Register Google Sign-In Launcher
        registerGoogleSignInLauncher();
        
        // Set up click listeners
        setupClickListeners();
        
        // Mark that login screen has been shown
        markLoginScreenShown();
        
        Log.d(TAG, "OnboardingLoginActivity created");
    }
    
    /**
     * Initialize all views
     */
    private void initViews() {
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnSkip = findViewById(R.id.btnSkip);
        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
    }
    
    /**
     * Register Google Sign-In activity result launcher
     */
    private void registerGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    googleAuthManager.handleSignInResult(
                        result.getData(),
                        new GoogleAuthManager.AuthCallback() {
                            @Override
                            public void onSuccess(FirebaseUser user) {
                                isLoggingIn = false;
                                Log.d(TAG, "Google Sign-In successful: " + user.getEmail());
                                Toast.makeText(
                                    OnboardingLoginActivity.this,
                                    "Welcome, " + user.getDisplayName() + "!",
                                    Toast.LENGTH_SHORT
                                ).show();
                                navigateToMainActivity();
                            }
                            
                            @Override
                            public void onFailure(String error) {
                                isLoggingIn = false;
                                Log.e(TAG, "Google Sign-In failed: " + error);
                                Toast.makeText(
                                    OnboardingLoginActivity.this,
                                    "Sign in failed. Please try again.",
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    );
                } else {
                    isLoggingIn = false;
                    Log.d(TAG, "Google Sign-In cancelled by user");
                }
            }
        );
    }
    
    /**
     * Set up click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Google Sign-In button
        btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());
        
        // Skip button
        btnSkip.setOnClickListener(v -> handleSkip());
        
        // Privacy Policy link
        tvPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
    }
    
    /**
     * Handle Google Sign-In button click
     */
    private void handleGoogleSignIn() {
        if (isLoggingIn) {
            Log.d(TAG, "Already logging in, ignoring duplicate click");
            return;
        }
        
        isLoggingIn = true;
        Log.d(TAG, "Starting Google Sign-In flow");
        
        try {
            Intent signInIntent = googleAuthManager.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        } catch (Exception e) {
            isLoggingIn = false;
            Log.e(TAG, "Error launching Google Sign-In", e);
            Toast.makeText(
                this,
                "Unable to start sign in. Please try again.",
                Toast.LENGTH_SHORT
            ).show();
        }
    }
    
    /**
     * Handle Skip button click
     */
    private void handleSkip() {
        Log.d(TAG, "User skipped login");
        navigateToMainActivity();
    }
    
    /**
     * Open privacy policy in browser
     */
    private void openPrivacyPolicy() {
        try {
            String privacyUrl = getString(R.string.privacy_policy_url);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
            startActivity(browserIntent);
            Log.d(TAG, "Opened privacy policy");
        } catch (Exception e) {
            Log.e(TAG, "Error opening privacy policy", e);
            Toast.makeText(this, "Unable to open privacy policy", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to Main Activity
     */
    private void navigateToMainActivity() {
        // Small delay for smooth transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(OnboardingLoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 300);
    }
    
    /**
     * Mark that login screen has been shown (prevent future displays)
     */
    private void markLoginScreenShown() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HAS_SHOWN_LOGIN, true).apply();
        Log.d(TAG, "Marked login screen as shown");
    }
    
    /**
     * Check if login screen has been shown before
     * @return true if already shown, false if first time
     */
    public static boolean hasShownLoginScreen(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_HAS_SHOWN_LOGIN, false);
    }
    
    /**
     * Prevent back button from closing the activity
     * User must either login or skip
     */
    @Override
    public void onBackPressed() {
        // Do nothing - user must choose login or skip
        Log.d(TAG, "Back button pressed - ignored (must login or skip)");
    }
}

