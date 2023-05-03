package com.ispd.mommybook.activities;

import android.content.Context;

public class ActivitiesManager {

    private ActivitiesTouch mActivitiesTouch = null;

    public ActivitiesManager(Context context) {
        mActivitiesTouch = new ActivitiesTouch(context);
    }

    public void SetTouchViewVisible(boolean visible) {
        mActivitiesTouch.SetVisible(visible);
    }

    public void SetTouchButton() {
        mActivitiesTouch.SetTouchButton();
    }
}
