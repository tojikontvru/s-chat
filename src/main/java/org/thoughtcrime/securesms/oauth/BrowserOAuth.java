package org.thoughtcrime.securesms.oauth;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class BrowserOAuth {

  public interface Callback {
    void onResult(String code, String state, String redirectUri);
    void onError(String error);
  }

  public static void startFlow(Activity activity, String authUrl, Callback callback) {
    try {
      ServerSocket serverSocket = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));
      serverSocket.setSoTimeout(120000);
      int port = serverSocket.getLocalPort();
      String redirectUri = "http://localhost:" + port;

      String finalAuthUrl = authUrl + "&redirect_uri=" + Uri.encode(redirectUri);

      new Thread(() -> {
        try {
          Socket socket = serverSocket.accept();
          Scanner sc = new Scanner(socket.getInputStream());
          String requestLine = sc.hasNextLine() ? sc.nextLine() : "";

          String query = "";
          String[] parts = requestLine.split(" ");
          if (parts.length >= 2) {
            String path = parts[1];
            int qIdx = path.indexOf('?');
            if (qIdx >= 0) query = path.substring(qIdx + 1);
          }

          String html = "<html><body><h2>S Chat</h2><p>Готово! Можете закрыть это окно.</p>"
              + "<script>setTimeout(function(){window.close()},500);</script>"
              + "</body></html>";
          String body = "HTTP/1.1 200 OK\r\nContent-Length: "
              + html.getBytes(StandardCharsets.UTF_8).length
              + "\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" + html;
          OutputStream os = socket.getOutputStream();
          os.write(body.getBytes(StandardCharsets.UTF_8));
          os.close();
          sc.close();
          socket.close();
          serverSocket.close();

          String code = null, state = null;
          for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
              String k = URLDecoder.decode(kv[0], "UTF-8");
              String v = URLDecoder.decode(kv[1], "UTF-8");
              if ("code".equals(k)) code = v;
              if ("state".equals(k)) state = v;
            }
          }

          if (code != null) {
            String finalCode = code;
            String finalState = state;
            activity.runOnUiThread(() -> callback.onResult(finalCode, finalState, redirectUri));
          } else {
            activity.runOnUiThread(() -> callback.onError("No code in callback"));
          }
        } catch (Exception e) {
          try { serverSocket.close(); } catch (Exception ignored) {}
          activity.runOnUiThread(() -> callback.onError(e.getMessage()));
        }
      }).start();

      CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build();
      customTabsIntent.launchUrl(activity, Uri.parse(finalAuthUrl));
    } catch (Exception e) {
      callback.onError(e.getMessage());
    }
  }
}
