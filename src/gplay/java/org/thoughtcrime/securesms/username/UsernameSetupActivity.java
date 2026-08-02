package org.thoughtcrime.securesms.username;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestoreException;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

public class UsernameSetupActivity extends AppCompatActivity {

  private EditText usernameInput;
  private Button saveButton;
  private ProgressBar progressBar;
  private TextView statusText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_username_setup);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(R.string.username_setup);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    usernameInput = findViewById(R.id.username_input);
    saveButton = findViewById(R.id.save_button);
    progressBar = findViewById(R.id.progress);
    statusText = findViewById(R.id.status_text);

    loadCurrentUsername();

    saveButton.setOnClickListener(v -> saveUsername());
  }

  private void loadCurrentUsername() {
    String saved = getSharedPreferences("s_chat_prefs", MODE_PRIVATE)
      .getString("username", null);
    if (saved != null) {
      usernameInput.setText(saved);
      statusText.setText(getString(R.string.username_current, saved));
    }
  }

  private void saveUsername() {
    final String username = usernameInput.getText().toString().trim();
    if (TextUtils.isEmpty(username)) {
      usernameInput.setError(getString(R.string.username_empty_error));
      return;
    }
    if (username.length() < 3) {
      usernameInput.setError(getString(R.string.username_short_error));
      return;
    }
    if (!username.matches("^[a-zA-Z0-9._]+$")) {
      usernameInput.setError(getString(R.string.username_chars_error));
      return;
    }

    showLoading(true);

    String email = DcHelper.get(this, DcHelper.CONFIG_ADDRESS);
    String displayName = DcHelper.get(this, DcHelper.CONFIG_DISPLAY_NAME);
    if (displayName == null || displayName.isEmpty()) {
      displayName = email;
    }

    UsernameService.setUsername(username, email, displayName)
      .addOnSuccessListener(v -> {
        getSharedPreferences("s_chat_prefs", MODE_PRIVATE)
          .edit().putString("username", username).apply();
        showLoading(false);
        statusText.setText(getString(R.string.username_saved, username));
        Toast.makeText(this, R.string.username_saved_toast, Toast.LENGTH_SHORT).show();
      })
      .addOnFailureListener(e -> {
        showLoading(false);
        String msg = e.getMessage();
        if (e instanceof FirebaseFirestoreException) {
          Toast.makeText(this, getString(R.string.username_error, msg), Toast.LENGTH_LONG).show();
        } else {
          Toast.makeText(this, getString(R.string.username_error, msg), Toast.LENGTH_LONG).show();
        }
      });
  }

  private void showLoading(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    saveButton.setEnabled(!show);
    usernameInput.setEnabled(!show);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
