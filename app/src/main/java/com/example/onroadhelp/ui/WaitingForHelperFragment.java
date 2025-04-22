package com.example.onroadhelp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.onroadhelp.R;
import com.example.onroadhelp.databinding.FragmentWaitingForHelperBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class WaitingForHelperFragment extends Fragment {

    private FragmentWaitingForHelperBinding binding;
    private String requestId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration sosRequestListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWaitingForHelperBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            requestId = getArguments().getString("requestId");
            binding.textViewWaitingMessage.setText(getString(R.string.waiting_for_helper_message, requestId));
            startSosRequestListener();
        } else {
            binding.textViewWaitingMessage.setText(R.string.no_request_id);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (sosRequestListener != null) {
            sosRequestListener.remove();
        }
    }

    private void startSosRequestListener() {
        if (requestId != null) {
            sosRequestListener = db.collection("sos_requests").document(requestId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.w("WaitingFragment", "Listen failed.", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("status");
                            String acceptedHelperId = documentSnapshot.getString("acceptedHelperId");

                            if ("accepted".equals(status) && acceptedHelperId != null) {
                                Log.d("WaitingFragment", "Request accepted by helper: " + acceptedHelperId);
                                navigateToNavigationFragment(acceptedHelperId);
                            } else if ("canceled".equals(status) || "completed".equals(status) || "failed_no_providers".equals(status)) {
                                // Handle other status changes as needed (e.g., show a message and go back)
                                Log.i("WaitingFragment", "Request status updated: " + status);
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> {
                                        binding.textViewWaitingMessage.setText(getString(R.string.request_status_updated, status));
                                        // Optionally navigate back after a short delay
                                        requireView().postDelayed(() -> Navigation.findNavController(requireView()).popBackStack(), 3000);
                                    });
                                }
                            }
                        } else {
                            Log.d("WaitingFragment", "Current data: null");
                            if (isAdded()) {
                                binding.textViewWaitingMessage.setText(R.string.request_not_found);
                                requireView().postDelayed(() -> Navigation.findNavController(requireView()).popBackStack(), 3000);
                            }
                        }
                    });
        }
    }

    private void navigateToNavigationFragment(String helperId) {
        if (isAdded()) {
            NavController navController = Navigation.findNavController(requireView());
            Bundle bundle = new Bundle();
            bundle.putString("requestId", requestId);
            bundle.putString("helperId", helperId); // Pass helper ID if needed in NavigationFragment
            navController.navigate(R.id.action_waitingForHelperFragment_to_navigationFragment, bundle);
        }
    }
}