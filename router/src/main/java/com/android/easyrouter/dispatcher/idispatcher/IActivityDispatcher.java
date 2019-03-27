package com.android.easyrouter.dispatcher.idispatcher;

import android.app.Activity;

import com.android.easyrouter.callback.IRouterCallBack;


public interface IActivityDispatcher {

    boolean open(String url);

    boolean open(Activity activity, String url);

    boolean open(Activity activity, String url, IRouterCallBack routerCallBack);

}
