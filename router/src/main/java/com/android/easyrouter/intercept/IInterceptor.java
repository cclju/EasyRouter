package com.android.easyrouter.intercept;

import com.android.easyrouter.dispatcher.dispatcherimpl.model.IntentWrapper;


public interface IInterceptor {

    boolean intercept(IntentWrapper intentWrapper);

    void onIntercepted(IntentWrapper intentWrapper);
}
