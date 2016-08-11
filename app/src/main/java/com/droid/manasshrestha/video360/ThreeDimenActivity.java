package com.droid.manasshrestha.video360;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by ManasShrestha on 6/7/16.
 */
public class ThreeDimenActivity extends Activity {

    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.three_dimens);

//        recyclerView = (RecyclerView) findViewById(R.id.rv_feed);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(new FeedAdapter(this));

    }
}
