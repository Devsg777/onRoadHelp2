package com.example.onroadhelp.ui2.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HelperHomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HelperHomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Helper home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}