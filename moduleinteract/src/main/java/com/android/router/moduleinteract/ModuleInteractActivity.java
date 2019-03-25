package com.android.router.moduleinteract;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.android.easyrouter.EasyRouter;
import com.android.easyrouter.annotation.DisPatcher;

/**
 * Created by liuzhao on 2017/9/19.
 */
@DisPatcher("easyrouter://moduleInteract")
public class ModuleInteractActivity extends Activity implements View.OnClickListener {

//    @AutoAssign
    int age;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_interact);

        findViewById(R.id.btn_go_to_main).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (v.getId() == R.id.btn_go_to_main) {
            EasyRouter.open("easyrouter://maintwo");
        }
    }

}
