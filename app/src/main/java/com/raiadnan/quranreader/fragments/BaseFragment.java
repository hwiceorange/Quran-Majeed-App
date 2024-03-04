package com.raiadnan.quranreader.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.raiadnan.quranreader.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

//import com.flurry.android.FlurryAgent;

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
        View findViewById = this.view.findViewById(R.id.bt_back);
        if (findViewById != null) {
            findViewById.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    BaseFragment.this.activity.getSupportFragmentManager().popBackStack();
                }
            });
        }
        return this.view;
    }

    public static void addFragment(AppCompatActivity appCompatActivity, Fragment fragment) {
        if (appCompatActivity.getSupportFragmentManager().findFragmentByTag(fragment.getClass().getName()) == null) {
            addFragment(appCompatActivity, fragment, fragment.getClass().getName());
        }
    }

    private static void addFragment(AppCompatActivity appCompatActivity, @NonNull Fragment fragment, @NonNull String str) {
        appCompatActivity.getSupportFragmentManager().beginTransaction().addToBackStack(str).setCustomAnimations(R.anim.anim_in, R.anim.anim_out, R.anim.anim_in, R.anim.anim_out).add(R.id.home_host_fragment, fragment, str).commitAllowingStateLoss();
    }
}
