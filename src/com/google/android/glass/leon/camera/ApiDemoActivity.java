/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.glass.leon.camera;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.leon.camera.R;
import com.google.android.glass.leon.camera.card.CardAdapter;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

/**
 * Creates a card scroll view with examples of different GDK APIs.
 * 
 * <ol>
 * <li>Cards
 * <li>GestureDetector
 * <li>textAppearance[Large|Medium|Small]
 * <li>OpenGL LiveCard
 * <li>VoiceMenu
 * </ol>
 */
public class ApiDemoActivity extends Activity {

	private static final String TAG = ApiDemoActivity.class.getSimpleName();

	// Index of api demo cards.
	// Visible for testing.
	static final int CARD_BUILDER = 0;
	static final int GESTURE_DETECTOR = 1;
	static final int THEMING = 2;
	static final int OPENGL = 3;
	static final int VOICE_MENU = 4;

	private CardScrollAdapter mAdapter;
	private CardScrollView mCardScroller;

	private boolean mVoiceMenuEnabled = true;

	// Visible for testing.
	CardScrollView getScroller() {
		return mCardScroller;
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		// be sure to request this before setContentView() is called
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

		// Ensure screen stays on during demo.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mAdapter = new CardAdapter(createCards(this));
		mCardScroller = new CardScrollView(this);
		mCardScroller.setAdapter(mAdapter);
		setContentView(mCardScroller);
		setCardScrollerListener();
		openOptionsMenu();
	}

	/**
	 * Create list of API demo cards.
	 */
	private List<CardBuilder> createCards(Context context) {
		ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
		cards.add(CARD_BUILDER, new CardBuilder(context,
				CardBuilder.Layout.TEXT).setText(R.string.text_cards));
		cards.add(GESTURE_DETECTOR, new CardBuilder(context,
				CardBuilder.Layout.TEXT).setText("Gallery"));
		// cards.add(THEMING, new CardBuilder(context, CardBuilder.Layout.TEXT)
		// .setText(R.string.text_theming));
		// cards.add(OPENGL, new CardBuilder(context, CardBuilder.Layout.TEXT)
		// .setText(R.string.text_opengl));
		// cards.add(VOICE_MENU, new CardBuilder(context,
		// CardBuilder.Layout.TEXT)
		// .setText(R.string.text_voice_menu));
		return cards;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCardScroller.activate();
	}

	@Override
	protected void onPause() {
		mCardScroller.deactivate();
		super.onPause();
	}

	/**
	 * Different type of activities can be shown, when tapped on a card.
	 */
	private void setCardScrollerListener() {
		mCardScroller
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						Log.d(TAG, "Clicked view at position " + position
								+ ", row-id " + id);
						int soundEffect = Sounds.TAP;
						mVoiceMenuEnabled = !mVoiceMenuEnabled;
						getWindow().invalidatePanelMenu(
								WindowUtils.FEATURE_VOICE_COMMANDS);
						switch (position) {
						case CARD_BUILDER:
							startActivity(new Intent(ApiDemoActivity.this,
									PictureActivity.class));
							break;

						case GESTURE_DETECTOR:
							startActivity(new Intent(ApiDemoActivity.this,
									GalleryActivity.class));
							break;
						//
						// case THEMING:
						// startActivity(new Intent(ApiDemoActivity.this,
						// ThemingActivity.class));
						// break;
						//
						// case OPENGL:
						// startService(new Intent(ApiDemoActivity.this,
						// OpenGlService.class));
						// break;
						//
						// case VOICE_MENU:
						// startActivity(new Intent(ApiDemoActivity.this,
						// VoiceMenuActivity.class));
						// break;

						default:
							soundEffect = Sounds.ERROR;
							Log.d(TAG, "Don't show anything");
						}

						// Play sound.
						AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
						am.playSoundEffect(soundEffect);
					}
				});
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			getMenuInflater().inflate(R.menu.voice_menu_main, menu);
			return true;
		}
		// Good practice to pass through, for options menu.
		return super.onCreatePanelMenu(featureId, menu);
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// MenuInflater inflater = getMenuInflater();
	// inflater.inflate(R.menu.voice_menu_main, menu);
	// return true;
	// }

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			// Dynamically decides between enabling/disabling voice menu.
			return mVoiceMenuEnabled;
		}
		// Good practice to pass through, for options menu.
		return super.onPreparePanel(featureId, view, menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			switch (item.getItemId()) {
			case R.id.menu_take_picture:
				startActivity(new Intent(ApiDemoActivity.this,
						PictureActivity.class));
				break;
			case R.id.menu_gallery:
				startActivity(new Intent(ApiDemoActivity.this,
						GalleryActivity.class));
				break;
			case R.id.menu_back_main:
				finish();
				break;

			default:
				return true; // No change.
			}
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

}
