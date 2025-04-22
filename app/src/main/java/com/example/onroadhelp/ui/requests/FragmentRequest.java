package com.example.onroadhelp.ui.requests; // Replace with your actual package name

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.onroadhelp.adapter.RequestPagerAdapter;
import com.example.onroadhelp.databinding.FragmentRequestBinding; // Import your binding class
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class FragmentRequest extends Fragment {

    private FragmentRequestBinding binding;
    private RequestPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRequestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new FragmentActiveRequests()); // We will create this next
        fragments.add(new FragmentRequestHistory()); // We will create this later

        pagerAdapter = new RequestPagerAdapter(getChildFragmentManager(), getLifecycle(), fragments);
        binding.viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Active");
            } else if (position == 1) {
                tab.setText("History");
            }
        }).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for preventing memory leaks
    }
}