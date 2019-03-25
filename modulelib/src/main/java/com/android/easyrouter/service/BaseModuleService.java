package com.android.easyrouter.service;

import android.content.Context;

/**
 * 配置不同模块暴露方法，不同模块需要实现各自的方法
 */

public interface BaseModuleService extends IBaseModuleService {

    public interface ModuleInteractService extends BaseModuleService {
        void runModuleInteract(Context context);
    }

    public interface AppModuleService extends BaseModuleService {
        void runAppModule();
    }
}
