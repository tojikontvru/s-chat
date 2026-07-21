package org.thoughtcrime.securesms.oauth;

import android.net.Uri;
import android.util.Base64;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Scanner;

public class OAuthHelper {

  private static final HashMap<String, OAuthSession> pendingSessions = new HashMap<>();
  private static AuthResult pendingResult;
  private static String pendingDomain;

  public static class OAuthSession {
    public final String domain;
    public final String codeVerifier;
    public OAuthSession(String domain, String codeVerifier) {
      this.domain = domain;
      this.codeVerifier = codeVerifier;
    }
  }

  public static void storeResult(AuthResult result, String domain) {
    pendingResult = result;
    pendingDomain = domain;
  }

  public static AuthResult getPendingResult() {
    AuthResult r = pendingResult;
    pendingResult = null;
    return r;
  }

  public static String getPendingDomain() {
    String d = pendingDomain;
    pendingDomain = null;
    return d;
  }

  public static class AuthResult {
    public final String accessToken;
    public final String refreshToken;
    public final String email;

    public AuthResult(String accessToken, String refreshToken, String email) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.email = email;
    }
  }

  public static void storeSession(String state, String domain, String codeVerifier) {
    pendingSessions.put(state, new OAuthSession(domain, codeVerifier));
  }

  public static OAuthSession getSession(String state) {
    return pendingSessions.remove(state);
  }

  public static String generateCodeVerifier() {
    byte[] code = new byte[32];
    new SecureRandom().nextBytes(code);
    return Base64.encodeToString(code, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
  }

  public static String generateCodeChallenge(String verifier) {
    try {
      byte[] bytes = verifier.getBytes(StandardCharsets.US_ASCII);
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
      return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String buildAuthUrl(OAuthConfig.Provider provider, String codeChallenge, String state) {
    String redirectUri = isGoogleProvider(provider) ? OAuthConfig.GOOGLE_REDIRECT_URI : OAuthConfig.REDIRECT_URI;
    return provider.authUrl
      + "?client_id=" + Uri.encode(provider.clientId)
      + "&redirect_uri=" + Uri.encode(redirectUri)
      + "&response_type=code"
      + "&scope=" + Uri.encode(provider.scope)
      + "&state=" + Uri.encode(state)
      + "&code_challenge=" + Uri.encode(codeChallenge)
      + "&code_challenge_method=S256"
      + "&access_type=offline"
      + "&prompt=consent";
  }

  private static boolean isGoogleProvider(OAuthConfig.Provider provider) {
    return "gmail.com".equals(provider.name);
  }

  public static AuthResult exchangeCode(OAuthConfig.Provider provider, String code, String codeVerifier) {
    try {
      String redirectUri = isGoogleProvider(provider) ? OAuthConfig.GOOGLE_REDIRECT_URI : OAuthConfig.REDIRECT_URI;
      URL url = new URL(provider.tokenUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setDoOutput(true);

      String body = "grant_type=authorization_code"
        + "&code=" + Uri.encode(code)
        + "&redirect_uri=" + Uri.encode(redirectUri)
        + "&client_id=" + Uri.encode(provider.clientId)
        + "&code_verifier=" + Uri.encode(codeVerifier);
      if (!provider.clientSecret.isEmpty()) {
        body += "&client_secret=" + Uri.encode(provider.clientSecret);
      }

      OutputStream os = conn.getOutputStream();
      os.write(body.getBytes(StandardCharsets.UTF_8));
      os.close();

      int responseCode = conn.getResponseCode();
      Scanner sc = new Scanner(responseCode >= 200 && responseCode < 300
        ? conn.getInputStream() : conn.getErrorStream());
      String response = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";
      sc.close();

      if (responseCode < 200 || responseCode >= 300) {
        throw new RuntimeException("Token exchange failed (" + responseCode + "): " + response);
      }

      return parseTokenResponse(response, provider.name);
    } catch (Exception e) {
      throw new RuntimeException("Token exchange failed: " + e.getMessage(), e);
    }
  }

  public static AuthResult refreshToken(OAuthConfig.Provider provider, String refreshToken) {
    try {
      URL url = new URL(provider.tokenUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setDoOutput(true);

      String body = "grant_type=refresh_token"
        + "&refresh_token=" + Uri.encode(refreshToken)
        + "&client_id=" + Uri.encode(provider.clientId);
      if (!provider.clientSecret.isEmpty()) {
        body += "&client_secret=" + Uri.encode(provider.clientSecret);
      }

      OutputStream os = conn.getOutputStream();
      os.write(body.getBytes(StandardCharsets.UTF_8));
      os.close();

      int responseCode = conn.getResponseCode();
      Scanner sc = new Scanner(responseCode >= 200 && responseCode < 300
        ? conn.getInputStream() : conn.getErrorStream());
      String response = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";
      sc.close();

      if (responseCode < 200 || responseCode >= 300) {
        throw new RuntimeException("Token refresh failed (" + responseCode + "): " + response);
      }

      return parseTokenResponse(response, provider.name);
    } catch (Exception e) {
      throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
    }
  }

  private static AuthResult parseTokenResponse(String json, String defaultEmail) {
    try {
      org.json.JSONObject obj = new org.json.JSONObject(json);
      String accessToken = obj.optString("access_token", "");
      String refreshToken = obj.optString("refresh_token", "");
      String email = obj.optString("email", obj.optString("login", defaultEmail));
      return new AuthResult(accessToken, refreshToken, email);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse token response: " + e.getMessage(), e);
    }
  }

  public static String getState() {
    byte[] code = new byte[16];
    new SecureRandom().nextBytes(code);
    return Base64.encodeToString(code, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
  }
}
