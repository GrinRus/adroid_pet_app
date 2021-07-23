package ru.ryabov.pet.application.dao;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseFirestoreDAO {

    private final FirebaseFirestore db;

    public FirebaseFirestoreDAO() {
        db = FirebaseFirestore.getInstance();
    }
}
