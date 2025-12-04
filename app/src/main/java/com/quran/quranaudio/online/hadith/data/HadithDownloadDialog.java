package com.quran.quranaudio.online.hadith.data;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.quran.quranaudio.online.R;

/**
 * Dialog for downloading Hadith data for a specific language
 */
public class HadithDownloadDialog extends DialogFragment {
    
    private static final String ARG_LANGUAGE = "language";
    private static final String ARG_LANGUAGE_NAME = "language_name";
    
    private String language;
    private String languageName;
    private OnDownloadCompleteListener listener;
    
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView statusText;
    private Button downloadButton;
    private Button cancelButton;
    
    private boolean isDownloading = false;
    
    public static HadithDownloadDialog newInstance(String language, String languageName) {
        HadithDownloadDialog dialog = new HadithDownloadDialog();
        Bundle args = new Bundle();
        args.putString(ARG_LANGUAGE, language);
        args.putString(ARG_LANGUAGE_NAME, languageName);
        dialog.setArguments(args);
        return dialog;
    }
    
    public void setOnDownloadCompleteListener(OnDownloadCompleteListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            language = getArguments().getString(ARG_LANGUAGE);
            languageName = getArguments().getString(ARG_LANGUAGE_NAME);
        }
        setCancelable(false);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_hadith_download, null);
        
        TextView titleText = view.findViewById(R.id.downloadTitle);
        statusText = view.findViewById(R.id.downloadStatus);
        progressBar = view.findViewById(R.id.downloadProgress);
        progressText = view.findViewById(R.id.downloadProgressText);
        downloadButton = view.findViewById(R.id.btnDownload);
        cancelButton = view.findViewById(R.id.btnCancel);
        
        titleText.setText(getString(R.string.download_hadith_title, languageName));
        statusText.setText(getString(R.string.download_hadith_message, languageName));
        
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
        
        downloadButton.setOnClickListener(v -> startDownload());
        cancelButton.setOnClickListener(v -> {
            if (!isDownloading) {
                dismiss();
            }
        });
        
        builder.setView(view);
        
        Dialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        return dialog;
    }
    
    private void startDownload() {
        isDownloading = true;
        
        downloadButton.setEnabled(false);
        downloadButton.setText(R.string.downloading);
        cancelButton.setEnabled(false);
        
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText("0%");
        statusText.setText(R.string.downloading_hadith_data);
        
        HadithDataHelper.downloadLanguage(
            requireContext(),
            language,
            progress -> {
                if (getContext() != null) {
                    progressBar.setProgress(progress);
                    progressText.setText(progress + "%");
                }
            },
            success -> {
                isDownloading = false;
                if (getContext() != null) {
                    if (success) {
                        statusText.setText(R.string.download_complete);
                        if (listener != null) {
                            listener.onDownloadComplete(true);
                        }
                        dismiss();
                    } else {
                        statusText.setText(R.string.download_failed);
                        downloadButton.setEnabled(true);
                        downloadButton.setText(R.string.retry);
                        cancelButton.setEnabled(true);
                    }
                }
            }
        );
    }
    
    public void show(FragmentManager manager) {
        show(manager, "HadithDownloadDialog");
    }
    
    public interface OnDownloadCompleteListener {
        void onDownloadComplete(boolean success);
    }
}

