package com.raiadnan.quranreader.prayertimes.prayers.fragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.fragments.SettingFragment;
import com.raiadnan.quranreader.prayertimes.prayers.dialog.CalculationMethodDialog;
import com.raiadnan.quranreader.prayertimes.prayers.dialog.JuristicDialog;
import com.raiadnan.quranreader.prayertimes.prayers.dialog.LatitudeAdjustmentDialog;
import com.raiadnan.quranreader.prayertimes.prayers.dialog.ManualCorrectionDialog;
import com.raiadnan.quranreader.prayertimes.prayers.helper.AutoSettingJsonParse;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayerTimeModel;
import java.util.HashMap;

public class PrayerTimeSettingFragment extends BaseFragment {
    private LatitudeAdjustmentDialog adjustmentDialog;
    private View btnCalculationMethod;
    private View btnJuristic;
    private View btnLatitudeAdjustment;
    private View btnManualCorrection;
    private CalculationMethodDialog calculationMethodDialog;
    private App.SimpleCallback callback;
    private ManualCorrectionDialog correctionDialog;
    private String[] dataAdjustment;
    private String[] dataJuristic;
    private String[] dataMethod;
    private JuristicDialog juristicDialog;
    private Switch swAutoSetting;
    private Switch swDayLightSaving;
    private TextView tvCalculationMethodValue;
    private TextView tvJuristicValue;
    private TextView tvLatitudeAdjustmentValue;
    private TextView tvManualCorrectionValue;
    private View viewHideAutoSetting;


    public int getLayoutId() {
        return R.layout.fragment_prayer_time_setting;
    }

    public static PrayerTimeSettingFragment newInstance() {
        Bundle bundle = new Bundle();
        PrayerTimeSettingFragment prayerTimeSettingFragment = new PrayerTimeSettingFragment();
        prayerTimeSettingFragment.setArguments(bundle);
        return prayerTimeSettingFragment;
    }

    public void setCallback(App.SimpleCallback simpleCallback) {
        this.callback = simpleCallback;
    }

    @SuppressLint("WrongConstant")
    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initViewData();
        vibrate();
        this.swDayLightSaving.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrayerTimeSettingsPref.get().setDaylightSaving(isChecked);
            }
        });
        this.swAutoSetting.setChecked(PrayerTimeSettingsPref.get().isAutoSettings());
        this.viewHideAutoSetting.setVisibility(PrayerTimeSettingsPref.get().isAutoSettings() ? 8 : 0);
        this.swAutoSetting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                PrayerTimeSettingFragment.lambda$onViewCreated$1(PrayerTimeSettingFragment.this, compoundButton, z);
            }
        });
        initializeSalatSettings();
        click();

        ImageView imgFavorite = (ImageView) view.findViewById(R.id.setting_btn_back);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new SettingFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.home_host_fragment, fragment).commit();

            }
        });
    }

    @SuppressLint("WrongConstant")
    public static  void lambda$onViewCreated$1(PrayerTimeSettingFragment prayerTimeSettingFragment, CompoundButton compoundButton, boolean z) {
        if (!z) {
            prayerTimeSettingFragment.saveAutoSettings();
            prayerTimeSettingFragment.initializeSalatSettings();
        }
        prayerTimeSettingFragment.viewHideAutoSetting.setVisibility(z ? 8 : 0);
        PrayerTimeSettingsPref.get().setAutoSettings(z);
    }

    private void vibrate() {
        Switch switchR = (Switch) this.view.findViewById(R.id.sw_vb);
        switchR.setChecked(PrayerTimeSettingsPref.get().getVibrate());
        switchR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrayerTimeSettingsPref.get().setVibrate(isChecked);
            }
        });


    }

    private void click() {
        this.btnJuristic.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.showJuristic();
            }
        });
        this.btnLatitudeAdjustment.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.showAdjustment();
            }
        });
        this.btnManualCorrection.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.showCorrection();
            }
        });
        this.btnCalculationMethod.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.showCalculationMethod();
            }
        });
        this.view.findViewById(R.id.bt_fajr).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.openToneSetting(PrayerFragment.FAJR);
            }
        });
        this.view.findViewById(R.id.bt_sunrise).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.openToneSetting(PrayerFragment.SUNRISE);
            }
        });
        this.view.findViewById(R.id.bt_dhuhr).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.openToneSetting(PrayerFragment.DHUHR);
            }
        });
        this.view.findViewById(R.id.bt_asr).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.openToneSetting(PrayerFragment.ASR);
            }
        });
        this.view.findViewById(R.id.bt_maghrib).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.openToneSetting(PrayerFragment.MAGHRIB);
            }
        });
        this.view.findViewById(R.id.bt_isha).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerTimeSettingFragment.this.openToneSetting(PrayerFragment.ISHA);
            }
        });
    }

    public void openToneSetting(final String str) {
        ToneNotificationFragment newInstance = ToneNotificationFragment.newInstance();
        newInstance.setPrayItem(str);
        // BaseFragment.addFragment(PrayerTimeSettingFragment.this.activity, newInstance);;
    }


    public void showJuristic() {
        JuristicDialog juristicDialog2 = this.juristicDialog;
        if (juristicDialog2 == null || !juristicDialog2.isShowing()) {
            this.juristicDialog = new JuristicDialog(this.activity, new DialogInterface.OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PrayerTimeSettingFragment.this.tvJuristicValue.setText(PrayerTimeSettingFragment.this.dataJuristic[PrayerTimeSettingsPref.get().getJuristic() - 1]);
                }
            });
            this.juristicDialog.show();
        }
    }


    public void showAdjustment() {
        LatitudeAdjustmentDialog latitudeAdjustmentDialog = this.adjustmentDialog;
        if (latitudeAdjustmentDialog == null || !latitudeAdjustmentDialog.isShowing()) {
            this.adjustmentDialog = new LatitudeAdjustmentDialog(this.activity, new DialogInterface.OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PrayerTimeSettingFragment.this.tvLatitudeAdjustmentValue.setText(PrayerTimeSettingFragment.this.dataAdjustment[PrayerTimeSettingsPref.get().getLatdAdjst() - 1]);
                }
            });
            this.adjustmentDialog.show();
        }
    }


    public void showCorrection() {
        ManualCorrectionDialog manualCorrectionDialog = this.correctionDialog;
        if (manualCorrectionDialog == null || !manualCorrectionDialog.isShowing()) {
            this.correctionDialog = new ManualCorrectionDialog(this.activity, new DialogInterface.OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PrayerTimeSettingFragment.this.showManualCorrectionData();
                }
            });
            this.correctionDialog.show();
        }
    }


    public void showCalculationMethod() {
        CalculationMethodDialog calculationMethodDialog2 = this.calculationMethodDialog;
        if (calculationMethodDialog2 == null || !calculationMethodDialog2.isShowing()) {
            this.calculationMethodDialog = new CalculationMethodDialog(this.activity, new DialogInterface.OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PrayerTimeSettingFragment.this.tvCalculationMethodValue.setText(PrayerTimeSettingFragment.this.dataMethod[PrayerTimeSettingsPref.get().getCalculationMethodIndex()]);
                }
            });
            this.calculationMethodDialog.show();
        }
    }

    private void initViewData() {
        this.tvJuristicValue = (TextView) this.view.findViewById(R.id.tv_juristic_value);
        this.tvCalculationMethodValue = (TextView) this.view.findViewById(R.id.tv_calculation_method_value);
        this.tvManualCorrectionValue = (TextView) this.view.findViewById(R.id.tv_manual_correction_value);
        this.tvLatitudeAdjustmentValue = (TextView) this.view.findViewById(R.id.tv_latitude_adjustment_value);
        this.swAutoSetting = (Switch) this.view.findViewById(R.id.sw_auto_setting);
        this.swDayLightSaving = (Switch) this.view.findViewById(R.id.sw_day_light_saving);
        this.viewHideAutoSetting = this.view.findViewById(R.id.view_hide_auto_setting);
        this.btnJuristic = this.view.findViewById(R.id.bt_juristic);
        this.btnCalculationMethod = this.view.findViewById(R.id.bt_calculation_method);
        this.btnLatitudeAdjustment = this.view.findViewById(R.id.bt_latitude_adjustment);
        this.btnManualCorrection = this.view.findViewById(R.id.bt_manual_corrections);
        this.dataJuristic = getResources().getStringArray(R.array.arr_juristic);
        this.dataMethod = getResources().getStringArray(R.array.array_calculation_methods_new);
        this.dataAdjustment = getResources().getStringArray(R.array.arr_latitude_adjustment);
    }

    private void initializeSalatSettings() {
        this.swDayLightSaving.setChecked(PrayerTimeSettingsPref.get().isDaylightSaving());
        HashMap<String, Integer> settings = PrayerTimeSettingsPref.get().getSettings();
        int intValue = settings.get(PrayerTimeSettingsPref.JURISTIC).intValue();
        int calculationMethodIndex = PrayerTimeSettingsPref.get().getCalculationMethodIndex();
        int intValue2 = settings.get(PrayerTimeSettingsPref.LATITUDE_ADJUSTMENT).intValue();
        this.tvJuristicValue.setText(this.dataJuristic[intValue - 1]);
        this.tvCalculationMethodValue.setText(this.dataMethod[calculationMethodIndex]);
        this.tvLatitudeAdjustmentValue.setText(this.dataAdjustment[intValue2 - 1]);
        showManualCorrectionData();
    }


    public void showManualCorrectionData() {
        int correctionsFajr = PrayerTimeSettingsPref.get().getCorrectionsFajr();
        int correctionsSunrize = PrayerTimeSettingsPref.get().getCorrectionsSunrize();
        int correctionsZuhar = PrayerTimeSettingsPref.get().getCorrectionsZuhar();
        int correctionsAsar = PrayerTimeSettingsPref.get().getCorrectionsAsar();
        int correctionsMaghrib = PrayerTimeSettingsPref.get().getCorrectionsMaghrib();
        int correctionsIsha = PrayerTimeSettingsPref.get().getCorrectionsIsha();
        this.tvManualCorrectionValue.setText(correctionsFajr + ", " + correctionsSunrize + ", " + correctionsZuhar + ", " + correctionsAsar + ", " + correctionsMaghrib + ", " + correctionsIsha);
    }

    private void saveAutoSettings() {
        PrayerTimeModel autoSettings = new AutoSettingJsonParse().getAutoSettings(this.activity);
        PrayerTimeSettingsPref.get().setJuristic(autoSettings.getJuristicIndex());
        PrayerTimeSettingsPref.get().setCalculationMethod(autoSettings.getConventionNumber());
        PrayerTimeSettingsPref.get().setCalculationMethodIndex(autoSettings.getConventionPosition());
        PrayerTimeSettingsPref.get().setDaylightSaving(false);
        PrayerTimeSettingsPref.get().setJuristic(autoSettings.getJuristicIndex());
        PrayerTimeSettingsPref.get().setsLatdAdjst(2);
        int[] corrections = autoSettings.getCorrections();
        PrayerTimeSettingsPref.get().setCorrectionsFajr(corrections[0]);
        PrayerTimeSettingsPref.get().setCorrectionsSunrize(corrections[1]);
        PrayerTimeSettingsPref.get().setCorrectionsZuhar(corrections[2]);
        PrayerTimeSettingsPref.get().setCorrectionsAsar(corrections[3]);
        PrayerTimeSettingsPref.get().setCorrectionsMaghrib(corrections[4]);
        PrayerTimeSettingsPref.get().setCorrectionsIsha(corrections[5]);
    }

    public void onDestroy() {
        super.onDestroy();
        App.SimpleCallback simpleCallback = this.callback;
        if (simpleCallback != null) {
            simpleCallback.callback(Integer.valueOf(SettingFragment.PRAYER_CHANGE));
        }
    }
}
