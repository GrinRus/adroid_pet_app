package ru.ryabov.pet.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ru.ryabov.pet.application.dao.FirebaseDAO;
import ru.ryabov.pet.application.dao.Note;
import ru.ryabov.pet.application.databinding.NoteAddFragmentBinding;

public class NoteAddFragment extends Fragment {

    private NoteAddFragmentBinding binding;
    private FirebaseAuth mAuth;
    private EditText mEditText;
    private Button mSubmitButton;
    private FirebaseDAO db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = NoteAddFragmentBinding.inflate(inflater, container, false);

        db = FirebaseDAO.getInstance();
        mEditText = binding.editTextTextPersonName;
        mSubmitButton = binding.button;
        mAuth = FirebaseAuth.getInstance();

        mSubmitButton.setOnClickListener((view) -> {
            Note note = new Note();
            note.setText(mEditText.getText().toString());
            note.setDateTime(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)));
            note.setUserId(mAuth.getUid());

            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.saveNote(note).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Note add",
                                Toast.LENGTH_SHORT).show();
                        getActivity().runOnUiThread(() -> NavHostFragment.findNavController(NoteAddFragment.this).navigate(R.id.action_NoteAddFragment_toMainFragment));
                    }
                }).addOnFailureListener(task -> {
                    Toast.makeText(getContext(), "Please retry add note",
                            Toast.LENGTH_SHORT).show();
                });
            });
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
