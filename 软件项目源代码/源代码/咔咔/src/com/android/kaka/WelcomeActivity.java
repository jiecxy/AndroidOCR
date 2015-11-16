package com.android.kaka;

import com.mikewong.tool.kaka.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		Toast.makeText(this, "点击进入下一页", 1).show();
	}
	
	
	public void next(View v)
	{
		Intent intent = new Intent(this,MainActivity.class);
		startActivity(intent);
	}
	
	
}
