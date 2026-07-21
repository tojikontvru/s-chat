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

  private static final String GOOGLE_DOMAIN = "gmail.com";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();

    // Mode 1: Browser redirect (s-oauth://callback)
    if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
      handleRedirect(intent.getData());
      return;
    }

    // Mode 2: Google WebView flow
    String domain = intent.getStringExtra(EXTRA_PROVIDER);
    if (GOOGLE_DOMAIN.equals(domain)) {
      startGoogleWebView(domain);
    } else {
      finish();
    }
  }

  private void startGoogleWebView(String domain) {
    setContentView(R.layout.oauth_activity);

    OAuthConfig.Provider provider = OAuthConfig.getProvider(domain);
    if (provider == null) {
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    setTitle(getString(R.string.app_name) + " — Google");

    String codeVerifier = OAuthHelper.generateCodeVerifier();
    String codeChallenge = OAuthHelper.generateCodeChallenge(codeVerifier);
    String state = OAuthHelper.getState();
    OAuthHelper.storeSession(state, domain, codeVerifier);

    WebView webView = findViewById(R.id.oauth_webview);
    ProgressBar progressBar = findViewById(R.id.oauth_progress);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        if (url.startsWith("http://localhost")) {
          Uri uri = Uri.parse(url);
          String code = uri.getQueryParameter("code");
          String returnedState = uri.getQueryParameter("state");
          if (code != null && returnedState != null) {
            OAuthHelper.OAuthSession session = OAuthHelper.getSession(returnedState);
            if (session != null) {
              exchangeCode(code, session.codeVerifier, provider);
              return;
            }
          }
          setResult(RESULT_CANCELED);
          finish();
        }
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

  private void handleRedirect(Uri data) {
    String code = data.getQueryParameter("code");
    String returnedState = data.getQueryParameter("state");

    if (code == null || returnedState == null) {
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    OAuthHelper.OAuthSession session = OAuthHelper.getSession(returnedState);
    if (session == null) {
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    OAuthConfig.Provider provider = OAuthConfig.getProvider(session.domain);
    if (provider == null) {
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    exchangeCode(code, session.codeVerifier, provider);
  }

  private void exchangeCode(final String code, final String codeVerifier, final OAuthConfig.Provider provider) {
    new Thread(() -> {
      try {
        OAuthHelper.AuthResult result = OAuthHelper.exchangeCode(provider, code, codeVerifier);
        OAuthHelper.storeResult(result, provider.name);
        setResult(RESULT_OK);
      } catch (Exception e) {
        setResult(RESULT_CANCELED);
      }
      runOnUiThread(this::finish);
    }).start();
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_CANCELED);
    super.onBackPressed();
  }
}
