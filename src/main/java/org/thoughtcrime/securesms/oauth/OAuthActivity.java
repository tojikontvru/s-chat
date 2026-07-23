package org.thoughtcrime.securesms.oauth;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OAuthActivity extends AppCompatActivity {

  public static final String EXTRA_PROVIDER = "provider_domain";
  public static final String RESULT_ACCESS_TOKEN = "access_token";
  public static final String RESULT_REFRESH_TOKEN = "refresh_token";
  public static final String RESULT_EMAIL = "email";

  private static final String YANDEX_DOMAIN = "yandex.ru";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();

    // Mode: Browser redirect (s-oauth://callback) - Yandex OAuth
    if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
      handleRedirect(intent.getData());
      return;
    }
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
