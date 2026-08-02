package org.thoughtcrime.securesms.username;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UsernameSetupActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Toast.makeText(this, "Username requires Google Play Services", Toast.LENGTH_LONG).show();
    finish();
  }
}
