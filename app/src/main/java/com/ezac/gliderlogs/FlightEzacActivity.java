package com.ezac.gliderlogs;

/*
 *  Copyright (c) <2015> <Pro-Serv, P van der Wielen, EZAC>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *	documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 *	the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 *	and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *	
 *	Commercial usage of  (the "Software") is not prohibited
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 *	WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 *	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 *	ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

@SuppressLint("SetJavaScriptEnabled")
public class FlightEzacActivity extends FragmentActivity {

	protected static String TAG = "EzacURLPage";

	// private LinearLayout layout;
	final Context context = FlightEzacActivity.this;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.ezac_list);

		// hide soft keyboard
		setMode();

		Button close = (Button) findViewById(R.id.button_close);

		WebView browser = (WebView) findViewById(R.id.webview_1);
		browser.setWebViewClient(new WebViewClient());
		browser.getSettings().setSupportZoom(true);
		browser.getSettings().setBuiltInZoomControls(true);
		browser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		browser.setScrollbarFadingEnabled(true);
		browser.getSettings().setLoadsImagesAutomatically(true);
		browser.getSettings().setDomStorageEnabled(true);
		browser.getSettings().setAppCacheEnabled(true);
		browser.getSettings().setAppCacheMaxSize(1024*1024*32);
		String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
		browser.getSettings().setAppCachePath(appCachePath);
		browser.getSettings().setAllowFileAccess(true);
		browser.getSettings().setJavaScriptEnabled(true);
		browser.loadUrl("http://www.ezac.nl/drupal/");

		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(RESULT_OK);
				finish();
			}
		});
	}

	public void setMode() {
		// hide soft keyboard on app launch
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
}
