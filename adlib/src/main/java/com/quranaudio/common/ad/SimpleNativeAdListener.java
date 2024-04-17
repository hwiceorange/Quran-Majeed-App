package com.quranaudio.common.ad;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.quranaudio.common.ad.model.AdItem;
import com.quranaudio.common.ad.model.LoadingState;

/**
 * native 广告监听回调
 */
public class SimpleNativeAdListener extends AdListener {
	long startTime;
	AdLoadCallback callback;
	AdShowCallback showCallback;
	String mAdPosition, adId, mFunctionTag;

	NativeAd nativeAd;
	long showedTime;
	AdItem adItem;

	public SimpleNativeAdListener(String adPosition, String adId, String functionTag, AdLoadCallback callback, AdShowCallback showCallback, AdItem adItem){
		this.callback=callback;
		this.showCallback=showCallback;
		this.mAdPosition = adPosition;
		this.adId = adId;
		this.mFunctionTag = functionTag;
		this.adItem = adItem;
		startTime=System.currentTimeMillis();
	}

	public void setShowCallback(AdShowCallback showCallback) {
		this.showCallback = showCallback;
	}

	public void setNativeAd(NativeAd nativeAd){
		this.nativeAd = nativeAd;
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
		adItem.setLoadingState(LoadingState.FAILED);
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
		reportEvent("onAdLoaded",System.currentTimeMillis() - startTime, null, null);
//		if(callback!=null) {
//			callback.onAdLoaded(null);
//		}
	}

	@Override public void onAdOpened() {
		super.onAdOpened();
	}

	private void reportEvent(String event, long time, String errorCode, String errorMessage) {
		String adapter = "";
		if(nativeAd!=null) {
			adapter = nativeAd.getResponseInfo().getMediationAdapterClassName();
		}
//		reportEvent();
//		ReportHelper.uploadData(new Report.Builder()
//				.actionParam(mAdPosition != null ? mAdPosition : "")
//				.action(event)
//				.refer(mFunctionTag!=null? mFunctionTag: "")
//				.sid(String.valueOf(System.currentTimeMillis()))
//				.top(errorCode != null ? errorCode : "")
//				.followTopics(errorMessage != null ? errorMessage : "")
//				.itemId(adId!= null ? adId : "")
//				.intervalTime(time)
//				.title(adId!= null ? adId : "")
//				.bottom(adapter)
//				.build());
	}
}
