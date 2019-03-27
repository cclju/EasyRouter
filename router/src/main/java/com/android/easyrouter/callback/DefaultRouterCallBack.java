package com.android.easyrouter.callback;

import com.android.easyrouter.dispatcher.dispatcherimpl.model.IntentWrapper;
import com.android.easyrouter.util.EasyRouterLogUtils;


public class DefaultRouterCallBack implements IRouterCallBack {
    @Override
    public void onFound(IntentWrapper intentWrapper) {
        EasyRouterLogUtils.i("onFound");
    }

    @Override
    public void onLost(IntentWrapper intentWrapper) {
        EasyRouterLogUtils.e("onLost please cheak for reason");
    }

    @Override
    public void onOpenSuccess(IntentWrapper intentWrapper) {
        EasyRouterLogUtils.i("onOpenSuccess");
    }

    @Override
    public void onOpenFailed(IntentWrapper intentWrapper, Throwable e) {
        EasyRouterLogUtils.e("onOpenFailed please cheak for reason :" + e.toString());
        e.printStackTrace();
    }
}
