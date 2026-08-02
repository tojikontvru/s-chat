package org.thoughtcrime.securesms.username;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;

public class UsernameSearchHelper {

  public static boolean isAvailable() {
    return true;
  }

  public static void showSearchDialog(Activity activity) {
    final EditText input = new EditText(activity);
    input.setHint(R.string.username_search_hint);
    input.setSingleLine(true);
    input.setPadding(48, 32, 48, 32);

    new AlertDialog.Builder(activity)
      .setTitle(R.string.username_search)
      .setView(input)
      .setPositiveButton(android.R.string.search_go, (dialog, which) -> {
        final String query = input.getText().toString().trim();
        if (query.isEmpty()) return;
        searchAndShowResult(activity, query);
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  private static void searchAndShowResult(Activity activity, String username) {
    UsernameService.searchExactUsername(username)
      .addOnSuccessListener(doc -> {
        if (doc.exists()) {
          String email = doc.getString("email");
          new AlertDialog.Builder(activity)
            .setTitle(R.string.username_found)
            .setMessage(activity.getString(R.string.username_found, username, email))
            .setPositiveButton(R.string.chat, (d, w) -> {
              Intent intent = new Intent(activity, ConversationActivity.class);
              intent.putExtra(ConversationActivity.ADDRESS_EXTRA, email);
              intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, -1);
              activity.startActivity(intent);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        } else {
          new AlertDialog.Builder(activity)
            .setTitle(R.string.username_not_found)
            .setMessage(activity.getString(R.string.username_not_found))
            .setPositiveButton(android.R.string.ok, null)
            .show();
        }
      })
      .addOnFailureListener(e -> {
        Toast.makeText(activity, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
      });
  }
}
