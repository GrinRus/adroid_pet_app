package ru.ryabov.pet.application.dao;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class FirebaseDAO {

    private static final String TAG = "FIREBASE_DAO";
    private static final String DB_INIT_SUCCESS = "DB_INIT_SUCCESS";
    private static FirebaseDAO firebaseDAO;
    private final FirebaseFirestore db;
    private final List<Note> notes = new ArrayList<>();
    private boolean isInit;

    private FirebaseDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseDAO getInstance() {
        if (firebaseDAO == null) {
            firebaseDAO = new FirebaseDAO();
        }
        return firebaseDAO;
    }

    public FirebaseDAO initDataSet(String userId) {
        notes.clear();
        initUserNotes(userId);
        return firebaseDAO;
    }

    public Task<DocumentReference> saveNote(Note note) {
        return db.collection("notes").add(note).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                notes.add(note);
            }
        });
    }

    private Task<QuerySnapshot> initUserNotes(String userId) {
        isInit = false;
        return db.collection("notes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    if (document.get("userId", String.class).equals(userId)) {
                        Note note = toNote(document);
                        Log.d(TAG, "getNotes: " + note);
                        notes.add(note);
                    }
                }
                isInit = true;
                Log.d(TAG, "initUserNotes: " + "true");
            }
        });
    }

    private Note toNote(QueryDocumentSnapshot document) {
        Note result = new Note();
        result.setText(document.get("test", String.class));
        result.setDateTime(document.get("dateTime", String.class));
        result.setText(document.get("userId", String.class));
        return result;
    }
}
