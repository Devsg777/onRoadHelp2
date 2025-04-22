package com.example.onroadhelp.ui2.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HelperDashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;


    public HelperDashboardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Helper dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }


}