package com.bro.barbershop.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private View view;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        binding.cardMenuCustomer.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_nav_home_to_nav_customer));

        binding.cardMenuTransaction.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_nav_home_to_nav_transaction));

        binding.cardMenuReport.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_nav_home_to_nav_report));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}