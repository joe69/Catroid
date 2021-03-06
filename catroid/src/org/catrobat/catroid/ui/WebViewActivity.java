/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2015 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.utils.DownloadUtil;
import org.catrobat.catroid.utils.Utils;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends BaseActivity {

	public static final String INTENT_PARAMETER_URL = "url";
	public static final String ANDROID_APPLICATION_EXTENSION = ".apk";
	private WebView webView;
	private boolean callMainMenu = false;
	private String url;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);

		ActionBar actionBar = getSupportActionBar();
		actionBar.hide();

		Intent intent = getIntent();
		url = intent.getStringExtra(INTENT_PARAMETER_URL);
		if (url == null) {
			url = Constants.BASE_URL_HTTPS;
		}

		webView = (WebView) findViewById(R.id.webView);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new MyWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);

		String language = String.valueOf(Constants.CURRENT_CATROBAT_LANGUAGE_VERSION);
		String flavor = Constants.FLAVOR_DEFAULT;
		String version = Utils.getVersionName(getApplicationContext());
		String platform = Constants.PLATFORM_DEFAULT;
		webView.getSettings().setUserAgentString("Catrobat/" + language + " " + flavor + "/"
				+ version + " Platform/" + platform);

		webView.loadUrl(url);

		webView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
					long contentLength) {
				Log.e("TAG", "contentDisposition: " + contentDisposition + "   " + mimetype);
				if (contentDisposition.contains(Constants.CATROBAT_EXTENSION)) {
					DownloadUtil.getInstance().prepareDownloadAndStartIfPossible(WebViewActivity.this, url);
				} else {
					DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

					request.setTitle(getString(R.string.notification_download_title_pending) + " " + DownloadUtil.getInstance().getProjectNameFromUrl(url));
					request.setDescription(getString(R.string.notification_download_pending));
					request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, 
							DownloadUtil.getInstance().getProjectNameFromUrl(url) + ANDROID_APPLICATION_EXTENSION);
					request.setMimeType(mimetype);

					registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

					DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
					downloadManager.enqueue(request);
				}
			}
		});
	}

	BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {

			long id = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
			DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setDataAndType(downloadManager.getUriForDownloadedFile(id),
					downloadManager.getMimeTypeForDownloadedFile(id));
			startActivity(intent);
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			callMainMenu = false;
			webView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private class MyWebViewClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String urlClient, Bitmap favicon) {
			if (callMainMenu && urlClient.equals(url)) {
				Intent intent = new Intent(getBaseContext(), MainMenuActivity.class);
				startActivity(intent);
			}

		}

		@Override
		public void onPageFinished(WebView view, String url) {
			callMainMenu = true;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			if (checkIfWebViewVisitExternalWebsite(url)) {
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
				return true;
			}
			return false;
		}

		private boolean checkIfWebViewVisitExternalWebsite(String url) {
			if (url.contains(Constants.BASE_URL_HTTPS) || url.contains(Constants.CATROBAT_ABOUT_URL)) {
				return false;
			}
			return true;
		}

	}
}
