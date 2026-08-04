package org.thoughtcrime.securesms.username;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsernameService {

  private static final String COLLECTION = "usernames";
  public static final String FIELD_USERNAME = "username";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_DISPLAYNAME = "displayName";

  public static class UsernameResult {
    public final String username;
    public final String email;
    public final String displayName;

    public UsernameResult(String username, String email, String displayName) {
      this.username = username;
      this.email = email;
      this.displayName = displayName;
    }
  }

  public static Task<Void> setUsername(String username, String email, String displayName) {
    String key = username.toLowerCase().trim();
    Map<String, Object> data = new HashMap<>();
    data.put(FIELD_USERNAME, key);
    data.put(FIELD_EMAIL, email);
    data.put(FIELD_DISPLAYNAME, displayName);
    data.put("updatedAt", System.currentTimeMillis());
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .document(key)
      .set(data);
  }

  public static Task<Void> removeUsername(String username) {
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .document(username.toLowerCase().trim())
      .delete();
  }

  public static Task<DocumentSnapshot> getUsernameDoc(String username) {
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .document(username.toLowerCase().trim())
      .get();
  }

  public static Task<DocumentSnapshot> searchExactUsername(String username) {
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .document(username.toLowerCase().trim())
      .get();
  }

  public static Task<List<UsernameResult>> searchUsername(String query) {
    String q = query.toLowerCase().trim();
    if (q.isEmpty()) {
      return Tasks.forResult(new ArrayList<UsernameResult>());
    }
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .orderBy(FIELD_USERNAME)
      .startAt(q)
      .endAt(q + "\uf8ff")
      .limit(10)
      .get()
      .continueWith(task -> {
        List<UsernameResult> results = new ArrayList<>();
        if (task.isSuccessful()) {
          QuerySnapshot snap = task.getResult();
          if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
              String u = doc.getString(FIELD_USERNAME);
              String e = doc.getString(FIELD_EMAIL);
              String dn = doc.getString(FIELD_DISPLAYNAME);
              if (u != null && e != null) {
                results.add(new UsernameResult(u, e, dn));
              }
            }
          }
        }
        return results;
      });
  }
}