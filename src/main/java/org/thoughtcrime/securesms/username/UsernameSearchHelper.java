package org.thoughtcrime.securesms.username;

import android.app.Activity;

public class UsernameSearchHelper {

  public static boolean isAvailable() {
    return false;
  }

  public static void showSearchDialog(Activity activity) {
    if (activity instanceof android.content.Context) {
      android.widget.Toast.makeText(
        (android.content.Context) activity,
        "Username search requires Google Play Services",
        android.widget.Toast.LENGTH_LONG
      ).show();
    }
  }
}
