package com.google.android.glass.leon.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.glass.leon.camera.R;

/**
 * Activity showing the options menu.
 */
public class ServiceMenuActivity extends Activity {

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.voice_menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection.
		switch (item.getItemId()) {
		case R.id.menu_take_picture:
			Intent picIntent = new Intent(this, PictureActivity.class);
			picIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(picIntent);
			return true;
		case R.id.menu_gallery:
			Intent galIntent = new Intent(this, GalleryActivity.class);
			galIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(galIntent);
			return true;
		case R.id.menu_service_share:
			Intent intent = getIntent();
			if (intent != null) {
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					String uri = bundle.getString("bundle");
					if (uri != null && uri != "") {
						share(uri);
					} else {
						finish();
					}
				} else {
					finish();
				}
			} else {
				Toast.makeText(this, "Failed to get content",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			return true;
		case R.id.menu_back_main:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void share(String path) {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, path);
		shareIntent.setType("image/jpeg");
		startActivity(Intent.createChooser(shareIntent, "Share with"));

	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing else to do, closing the activity.
		finish();
	}
}