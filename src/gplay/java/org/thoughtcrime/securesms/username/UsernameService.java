package org.thoughtcrime.securesms.username;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UsernameService {

  private static final String COLLECTION = "usernames";

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
    Map<String, Object> data = new HashMap<>();
    data.put("email", email);
    data.put("displayName", displayName);
    data.put("updatedAt", System.currentTimeMillis());
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .document(username.toLowerCase().trim())
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

  public static Task<QuerySnapshot> searchUsername(String query) {
    String q = query.toLowerCase().trim();
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .whereGreaterThanOrEqualTo("email", q)
      .whereLessThanOrEqualTo("email", q + "\uf8ff")
      .limit(10)
      .get();
  }

  public static Task<DocumentSnapshot> searchExactUsername(String username) {
    return FirebaseFirestore.getInstance()
      .collection(COLLECTION)
      .document(username.toLowerCase().trim())
      .get();
  }
}
