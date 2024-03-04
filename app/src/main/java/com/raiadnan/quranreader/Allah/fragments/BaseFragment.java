package com.raiadnan.quranreader.Allah.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.raiadnan.quranreader.R;


public abstract class BaseFragment extends Fragment {
    public AppCompatActivity activity;
    protected View view;
    public abstract int getLayoutId();

    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        this.activity = (AppCompatActivity) getActivity();
        //   FlurryAgent.logEvent(getClass().getSimpleName());
    }


    @Nullable
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        this.view = layoutInflater.inflate(getLayoutId(), viewGroup, false);

        return this.view;
    }

    public static void addFragment(AppCompatActivity appCompatActivity, Fragment fragment) {
        if (appCompatActivity.getSupportFragmentManager().findFragmentByTag(fragment.getClass().getName()) == null) {
            addFragment(appCompatActivity, fragment, fragment.getClass().getName());
        }
    }

    private static void addFragment(AppCompatActivity appCompatActivity, @NonNull Fragment fragment, @NonNull String str) {
        appCompatActivity.getSupportFragmentManager().beginTransaction().addToBackStack(str).setCustomAnimations(R.anim.anim_in, R.anim.anim_out, R.anim.anim_in, R.anim.anim_out).add(R.id.main, fragment, str).commitAllowingStateLoss();
    }
}
