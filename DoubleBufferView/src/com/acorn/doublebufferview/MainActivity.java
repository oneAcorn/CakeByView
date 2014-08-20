package com.acorn.doublebufferview;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.acorn.doublebufferview.CakeView.CakeValue;

public class MainActivity extends Activity {
	RelativeLayout container;
	CakeView gameView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		container = (RelativeLayout) findViewById(R.id.container);
		gameView = (CakeView) findViewById(R.id.cakeView1);
		List<CakeValue> values = new ArrayList<CakeView.CakeValue>();
		values.add(new CakeValue("asdf", 20,"dsafsaofijodsajgodsaijgodsagdsagdsag"));
		values.add(new CakeValue("asdf", 20));
//		values.add(new CakeValue("asdf", 10));
		// values.add(new CakeValue("asdf", 2));
		// values.add(new CakeValue("asdf", 1));
		// values.add(new CakeValue("asdf", 17));
		gameView.setData(values);
		// gameView.setBackgroundColor(0xff3108f1);
		// container.addView(gameView, new LayoutParams(400, 400));
		findViewById(R.id.btn).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gameView.startAnim();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
