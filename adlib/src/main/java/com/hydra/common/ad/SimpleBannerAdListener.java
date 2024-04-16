package com.hydra.common.ad;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;


/**
 * banner 广告监听回调
 */
public class SimpleBannerAdListener extends AdListener {
	long startTime;
	AdLoadCallback callback;
	AdShowCallback showCallback;
	String mAdPosition, adId, mFunctionTag;
	long showedTime;
	ViewGroup admobAdView;
	AdView adView;
	public SimpleBannerAdListener(AdView adView, ViewGroup admobAdView, String adPosition, String adId, String functionTag, AdLoadCallback callback, AdShowCallback showCallback){
		this.adView = adView;
		this.admobAdView = admobAdView;
		this.callback=callback;
		this.showCallback=showCallback;
		this.mAdPosition = adPosition;
		this.adId = adId;
		this.mFunctionTag = functionTag;
		startTime=System.currentTimeMillis();
	}


	@Override public void onAdClicked() {
		super.onAdClicked();
		reportEvent("onClick", System.currentTimeMillis() - showedTime, null, null);
		if(showCallback!=null) {
			showCallback.onAdClicked(null);
		}
	}

	@Override public void onAdClosed() {
		super.onAdClosed();
		reportEvent("onClose", System.currentTimeMillis() - showedTime, null, null);
		if(showCallback!=null) {
			showCallback.onAdClosed(null);
		}
	}

	@Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
		super.onAdFailedToLoad(loadAdError);
		reportEvent(
			"onAdFailedToLoad",
			System.currentTimeMillis() - startTime,
			loadAdError.getCode() + "",
			loadAdError.getMessage());
		if(callback!=null) {
			callback.onAdFailedToLoad(adId);
		}
	}

	@Override public void onAdImpression() {
		super.onAdImpression();
		showedTime = System.currentTimeMillis();
		reportEvent("onShow", 0, null, null);
		reportEvent("onImpression", 0, null, null);
		if(showCallback!=null) {
			showCallback.onShow(null);
			showCallback.onAdImpression(null);
		}
	}

	@Override public void onAdLoaded() {
		super.onAdLoaded();
		if(admobAdView!=null) {
			admobAdView.setVisibility(View.VISIBLE);
		}
		reportEvent("onAdLoaded",System.currentTimeMillis() - startTime, null, null);
		if(callback!=null) {
			callback.onAdLoaded(null);
		}
		adView.setOnPaidEventListener(new AdmobOnPaidEventListener(mAdPosition, mFunctionTag, adId, adView));
	}

	@Override public void onAdOpened() {
		super.onAdOpened();
	}

	private String getAdAdapterName() {
		if(adView!=null && adView.getResponseInfo() != null){
			return adView.getResponseInfo().getMediationAdapterClassName();
		}
		return "";
	}

	private String getAdAdResponseId() {
		if(adView!=null && adView.getResponseInfo() != null){
			return adView.getResponseInfo().getResponseId();
		}
		return "";
	}

	private void reportEvent(String event, long time, String errorCode, String errorMessage) {
//		ReportHelper.uploadData(new Report.Builder()
//			.actionParam(mAdPosition != null ? mAdPosition : "")
//			.action(event)
//			.refer(mFunctionTag!=null? mFunctionTag: "")
//			.sid(String.valueOf(System.currentTimeMillis()))
//			.top(errorCode != null ? errorCode : "")
//			.followTopics(errorMessage != null ? errorMessage : "")
//			.itemId(adId!= null ? adId : "")
//			.intervalTime(time)
//			.title(adId!= null ? adId : "")
//			.build());
	}
}
