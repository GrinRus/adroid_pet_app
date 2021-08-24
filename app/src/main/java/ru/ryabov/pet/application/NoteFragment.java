package ru.ryabov.pet.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import ru.ryabov.pet.application.databinding.NoteFragmentBinding;

public class NoteFragment extends Fragment {

    private NoteFragmentBinding binding;
    private TextView noteTextFirst;
    private TextView noteTextSecond;
    private TextView noteTextThird;
    private TextView noteTextFourth;
    private Button mToNotesButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = NoteFragmentBinding.inflate(inflater, container, false);

        noteTextFirst = binding.noteTextOne;
        noteTextSecond = binding.noteTextSecond;
        noteTextThird = binding.noteTextThird;
        noteTextFourth = binding.noteTextFourth;

        mToNotesButton = binding.button;

        noteTextFirst.setText(getArguments().getString("textViewFirst"));
        noteTextSecond.setText(getArguments().getString("textViewSecond"));
        noteTextThird.setText(getArguments().getString("textViewThird"));
        noteTextFourth.setText(getArguments().getString("textViewFourth"));

        mToNotesButton.setOnClickListener((v -> {
            NavHostFragment.findNavController(NoteFragment.this)
                    .navigate(R.id.action_NoteFragment_toMainFragment);
        }));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
