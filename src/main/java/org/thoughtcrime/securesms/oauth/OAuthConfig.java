package org.thoughtcrime.securesms.oauth;

import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.BuildConfig;

public class OAuthConfig {

  public static class Provider {
    public final String name;
    public final String authUrl;
    public final String tokenUrl;
    public final String scope;
    public final String imapHost;
    public final int imapPort;
    public final String smtpHost;
    public final int smtpPort;
    public final String clientId;
    public final String clientSecret;

    public Provider(String name, String authUrl, String tokenUrl, String scope,
                    String imapHost, int imapPort, String smtpHost, int smtpPort,
                    String clientId, String clientSecret) {
      this.name = name; this.authUrl = authUrl; this.tokenUrl = tokenUrl;
      this.scope = scope; this.imapHost = imapHost; this.imapPort = imapPort;
      this.smtpHost = smtpHost; this.smtpPort = smtpPort;
      this.clientId = clientId; this.clientSecret = clientSecret;
    }
  }

  public static final String REDIRECT_URI = "s-oauth://callback";

  // =========================================================
  // ВСТАВЬ СВОИ CLIENT ID И CLIENT SECRET В secrets.properties
  // =========================================================
  // 1. Скопируй secrets.properties.example → secrets.properties
  // 2. Замени значения на свои
  // 3. secrets.properties добавлен в .gitignore (не попадёт в репозиторий)

  public static final Provider GOOGLE = new Provider(
    "gmail.com",
    "https://accounts.google.com/o/oauth2/v2/auth",
    "https://oauth2.googleapis.com/token",
    "https://mail.google.com/",
    "imap.gmail.com", 993,
    "smtp.gmail.com", 587,
    BuildConfig.OAUTH_GOOGLE_CLIENT_ID,
    BuildConfig.OAUTH_GOOGLE_CLIENT_SECRET
  );

  public static final Provider YANDEX = new Provider(
    "yandex.ru",
    "https://oauth.yandex.com/authorize",
    "https://oauth.yandex.com/token",
    "mail:imap_full,mail:smtp_full",
    "imap.yandex.com", 993,
    "smtp.yandex.com", 465,
    BuildConfig.OAUTH_YANDEX_CLIENT_ID,
    BuildConfig.OAUTH_YANDEX_CLIENT_SECRET
  );

  public static final Provider MAILRU = new Provider(
    "mail.ru",
    "https://o2.mail.ru/login",
    "https://o2.mail.ru/token",
    "mail.imap,mail.smtp",
    "imap.mail.ru", 993,
    "smtp.mail.ru", 465,
    BuildConfig.OAUTH_MAILRU_CLIENT_ID,
    BuildConfig.OAUTH_MAILRU_CLIENT_SECRET
  );

  public static final Provider VK = new Provider(
    "vk.com",
    "https://oauth.vk.com/authorize",
    "https://oauth.vk.com/access_token",
    "mail",
    "imap.vk.com", 993,
    "smtp.vk.com", 465,
    BuildConfig.OAUTH_VK_CLIENT_ID,
    BuildConfig.OAUTH_VK_CLIENT_SECRET
  );

  public static @Nullable Provider getProvider(String domain) {
    switch (domain) {
      case "gmail.com": return GOOGLE;
      case "yandex.ru": return YANDEX;
      case "mail.ru": return MAILRU;
      case "vk.com": return VK;
      default: return null;
    }
  }
}
