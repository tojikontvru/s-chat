package org.thoughtcrime.securesms.username;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;

import java.util.ArrayList;
import java.util.List;

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
        searchAndShowResults(activity, query);
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  private static void searchAndShowResults(Activity activity, String query) {
    UsernameService.searchUsername(query)
      .addOnSuccessListener(results -> {
        if (results == null || results.isEmpty()) {
          showNotFound(activity, query);
          return;
        }
        showResultsList(activity, results);
      })
      .addOnFailureListener(e -> {
        Toast.makeText(activity, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
      });
  }

  private static void showResultsList(Activity activity, List<UsernameService.UsernameResult> results) {
    List<String> labels = new ArrayList<>();
    for (UsernameService.UsernameResult r : results) {
      String label = "@" + r.username;
      if (r.displayName != null && !r.displayName.isEmpty()) {
        label += "  (" + r.displayName + ")";
      }
      labels.add(label);
    }

    CharSequence[] items = labels.toArray(new CharSequence[0]);

    new AlertDialog.Builder(activity)
      .setTitle(R.string.username_search_results)
      .setItems(items, (dialog, which) -> {
        UsernameService.UsernameResult r = results.get(which);
        openChat(activity, r.email);
      })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  private static void showNotFound(Activity activity, String query) {
    new AlertDialog.Builder(activity)
      .setTitle(R.string.username_not_found)
      .setMessage(activity.getString(R.string.username_not_found_hint, query))
      .setPositiveButton(android.R.string.ok, null)
      .show();
  }

  private static void openChat(Activity activity, String email) {
    Intent intent = new Intent(activity, ConversationActivity.class);
    intent.putExtra(ConversationActivity.ADDRESS_EXTRA, email);
    intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, -1);
    activity.startActivity(intent);
  }
}