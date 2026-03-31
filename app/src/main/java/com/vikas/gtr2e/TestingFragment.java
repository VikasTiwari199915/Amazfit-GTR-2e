package com.vikas.gtr2e;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vikas.gtr2e.databinding.FragmentTestingBinding;

public class TestingFragment extends Fragment {

    FragmentTestingBinding binding;

    public TestingFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTestingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.button.setOnClickListener(v->{
            GTR2eApp.getGTR2eManager().performAction("TEST", binding.paraInput.getEditText().getText().toString());
        });
    }
}