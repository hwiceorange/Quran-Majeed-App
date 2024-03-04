package com.raiadnan.quranreader.calender.fragment;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.calender.model.Event;
import com.raiadnan.quranreader.fragments.BaseFragment;

public class EventDetailFragment extends BaseFragment {
    private Event event;
    public int getLayoutId() {
        return R.layout.fragment_event_detail;
    }

    public static EventDetailFragment newInstance() {
        Bundle bundle = new Bundle();
        EventDetailFragment eventDetailFragment = new EventDetailFragment();
        eventDetailFragment.setArguments(bundle);
        return eventDetailFragment;
    }

    public void setEvent(Event event2) {
        this.event = event2;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        ((TextView) view.findViewById(R.id.tv_title)).setText(this.event.getEventName());
        WebView webView = (WebView) view.findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(this.event.getUrl());
    }
}
