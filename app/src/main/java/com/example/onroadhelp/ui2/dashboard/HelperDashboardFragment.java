package com.example.onroadhelp.ui2.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.onroadhelp.databinding.FragmentHelperDashboardBinding
        ;

public class HelperDashboardFragment extends Fragment {

    private FragmentHelperDashboardBinding
            binding;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HelperDashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(HelperDashboardViewModel.class);

        binding = FragmentHelperDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}