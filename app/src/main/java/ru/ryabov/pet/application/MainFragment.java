package ru.ryabov.pet.application;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import ru.ryabov.pet.application.dao.FirebaseDAO;
import ru.ryabov.pet.application.dao.Note;
import ru.ryabov.pet.application.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {
    private static final int SPAN_COUNT = 2;
    private static final String TAG = "MAIN_FRAGMENT";
    private FragmentMainBinding binding;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private LayoutManagerType mCurrentLayoutManagerType;
    private CustomAdapter mAdapter;
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private List<Note> mDataset = new ArrayList<>();
    private FirebaseDAO dao;
    private FirebaseAuth mAuth;
    private ImageButton mAddButton;

    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null){
            NavHostFragment.findNavController(MainFragment.this)
                    .navigate(R.id.action_MainFragment_to_FirstFragment);
        }
        dao = FirebaseDAO.getInstance().initDataSet(mAuth.getUid());
        mDataset = dao.getNotes();
        Thread thread = new Thread(new InitDatasetRunnable(dao));
        thread.start();
        binding = FragmentMainBinding.inflate(inflater, container, false);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView = binding.recyclerView;
        mAdapter = new CustomAdapter(mDataset, this);
        mAddButton = binding.addButton;


        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        // Set CustomAdapter as the adapter for RecyclerView.
        mRecyclerView.setAdapter(mAdapter);

        mAddButton.setOnClickListener((v -> {
            NavHostFragment.findNavController(MainFragment.this)
                    .navigate(R.id.action_MainFragment_to_AddNoteFragment);
        }));

        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        mAdapter.notifyDataSetChanged();
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @RequiredArgsConstructor
    private class InitDatasetRunnable implements Runnable {

        private final FirebaseDAO dao;

        @Override
        public void run() {
            Log.d(TAG, "initDatasetListener: " + "created");
            while (!dao.isInit()) {
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "initDatasetListener: " + " run notify");
            getActivity().runOnUiThread(() -> {
                mAdapter.notifyDataSetChanged();
                Log.d(TAG, "initDatasetListener: " + " notify complete");
            });
        }
    }

    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        if (layoutManagerType == LayoutManagerType.GRID_LAYOUT_MANAGER) {
            mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
            mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
        } else {
            mLayoutManager = new LinearLayoutManager(getActivity());
            mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }
}
