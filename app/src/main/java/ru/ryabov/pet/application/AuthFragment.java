package ru.ryabov.pet.application;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;

import ru.ryabov.pet.application.databinding.FragmentAuthBinding;

public class AuthFragment extends Fragment {

    private static final String TAG = "AUTH_FRAGMENT";
    public static final String USER_SUCCESS_AUTH = "UserSuccessAuth";
    private FragmentAuthBinding binding;
    private FirebaseAuth mAuth;
    private Activity activity;
    private EditText editEmail;
    private EditText editPassword;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        activity = getActivity();

        editEmail = binding.editTextTextEmailAddress;
        editPassword = binding.editTextTextPassword;

        binding.buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEmailAndPasswordValid(editEmail,  editPassword)) return;
                signUpNewUser(editEmail.getText().toString(), editPassword.getText().toString());
            }
        });

        binding.buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEmailAndPasswordValid(editEmail,  editPassword)) return;
                signIn(editEmail.getText().toString(), editPassword.getText().toString());
            }
        });
    }

    private boolean isEmailAndPasswordValid(EditText editEmail, EditText editPassword) {
        if (editEmail.getText() == null || editEmail.getText().toString().isEmpty()){
            Toast.makeText(getContext(), "Please fill email",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (editPassword.getText() == null || editEmail.getText().toString().isEmpty()){
            Toast.makeText(getContext(), "Please fill password",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void signUpNewUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Executors.newSingleThreadExecutor(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            activity.runOnUiThread(() -> successAuth());
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Looper.prepare();
                            Toast.makeText(getContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            activity.runOnUiThread(() -> failAuth());
                        }
                    }
                });
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(Executors.newSingleThreadExecutor(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            activity.runOnUiThread(() -> successAuth());
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Looper.prepare();
                            Toast.makeText(getContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            activity.runOnUiThread(() -> failAuth());
                        }
                    }
                });
    }

    private void failAuth() {
        NavHostFragment.findNavController(AuthFragment.this)
                .navigate(R.id.action_SecondFragment_to_FirstFragment);
    }

    private void successAuth() {
        Intent intent = new Intent();
        intent.setAction(USER_SUCCESS_AUTH);
        activity.sendBroadcast(intent);
        NavHostFragment.findNavController(AuthFragment.this)
                .navigate(R.id.action_SecondFragment_to_MainFragment);
    }

    private void updateUI(FirebaseUser user) {

    }
}