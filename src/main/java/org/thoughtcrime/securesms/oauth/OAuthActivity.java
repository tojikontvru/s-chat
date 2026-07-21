package org.thoughtcrime.securesms.oauth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.thoughtcrime.securesms.R;

public class OAuthActivity extends AppCompatActivity {

  public static final String EXTRA_PROVIDER = "provider_domain";
  public static final String RESULT_ACCESS_TOKEN = "access_token";
  public static final String RESULT_REFRESH_TOKEN = "refresh_token";
  public static final String RESULT_EMAIL = "email";

  private OAuthConfig.Provider provider;
  private String codeVerifier;
  private String state;
  private WebView webView;
  private ProgressBar progressBar;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.oauth_activity);

    String domain = getIntent().getStringExtra(EXTRA_PROVIDER);
    provider = OAuthConfig.getProvider(domain);
    if (provider == null) {
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    setTitle("Вход через " + provider.name);

    webView = findViewById(R.id.oauth_webview);
    progressBar = findViewById(R.id.oauth_progress);

    codeVerifier = OAuthHelper.generateCodeVerifier();
    String codeChallenge = OAuthHelper.generateCodeChallenge(codeVerifier);
    state = OAuthHelper.getState();

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        handleRedirect(url);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        progressBar.setVisibility(android.view.View.GONE);
      }
    });

    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.loadUrl(OAuthHelper.buildAuthUrl(provider, codeChallenge, state));
  }

  private void handleRedirect(String url) {
    if (url == null) return;
    if (!url.startsWith(OAuthConfig.REDIRECT_URI) && !url.startsWith(OAuthConfig.GOOGLE_REDIRECT_URI)) return;

    Uri uri = android.net.Uri.parse(url);
    String code = uri.getQueryParameter("code");
    String returnedState = uri.getQueryParameter("state");

    if (code != null && state.equals(returnedState)) {
      exchangeCode(code);
    } else {
      setResult(RESULT_CANCELED);
      finish();
    }
  }

  private void exchangeCode(final String code) {
    new Thread(() -> {
      try {
        OAuthHelper.AuthResult result = OAuthHelper.exchangeCode(provider, code, codeVerifier);
        Intent intent = new Intent();
        intent.putExtra(RESULT_ACCESS_TOKEN, result.accessToken);
        intent.putExtra(RESULT_REFRESH_TOKEN, result.refreshToken);
        intent.putExtra(RESULT_EMAIL, result.email);
        setResult(RESULT_OK, intent);
      } catch (Exception e) {
        Intent intent = new Intent();
        intent.putExtra("error", e.getMessage());
        setResult(RESULT_CANCELED, intent);
      }
      runOnUiThread(() -> finish());
    }).start();
  }

  @Override
  public void onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      setResult(RESULT_CANCELED);
      super.onBackPressed();
    }
  }
}
