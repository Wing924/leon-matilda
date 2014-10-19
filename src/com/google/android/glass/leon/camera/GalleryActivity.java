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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
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
public class GalleryActivity extends Activity {

	private static final String TAG = GalleryActivity.class.getSimpleName();

	// Index of api demo cards.
	// Visible for testing.
	static final int CARD_BUILDER = 0;
	static final int GESTURE_DETECTOR = 1;
	static final int THEMING = 2;
	static final int OPENGL = 3;
	static final int VOICE_MENU = 4;

	private CardScrollAdapter mAdapter;
	private CardScrollView mCardScroller;
	File[] listImages = null;
	private int currentPos = 0;
	private List<CardBuilder> list = null;

	// Visible for testing.
	CardScrollView getScroller() {
		return mCardScroller;
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		list = createCards(this);
		mAdapter = new CardAdapter(this, list, listImages);
		mCardScroller = new CardScrollView(this);
		if (list != null) {
			mCardScroller.setAdapter(mAdapter);
		}
		setContentView(mCardScroller);
		setCardScrollerListener();
	}

	/**
	 * Create list of API demo cards.
	 */
	private List<CardBuilder> createCards(Context context) {

		File gallery = new File(PictureActivity.GALLERY_PATH);

		File[] listImages1 = gallery.listFiles();
		if (listImages1 == null || listImages1.length == 0) {
			return null;
		}

		ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
		listImages = new File[listImages1.length];
		int j = 0;
		for (int i = 0; i < listImages1.length; i++) {
			File image = listImages1[i];

			if (image.isDirectory()) {

			} else {
				listImages[j] = listImages1[listImages1.length - 1 - i];
				j++;
				final String imageName = image.getAbsolutePath();
				cards.add(CARD_BUILDER, new CardBuilder(context,
						CardBuilder.Layout.TEXT).setText(imageName));
			}
		}
		// cards.add(CARD_BUILDER, new CardBuilder(context,
		// CardBuilder.Layout.TEXT).setText(R.string.text_cards));
		// cards.add(GESTURE_DETECTOR, new CardBuilder(context,
		// CardBuilder.Layout.TEXT)
		// .setText(R.string.text_gesture_detector));
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
						currentPos = position;
						int soundEffect = Sounds.TAP;
						// Play sound.
						AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
						am.playSoundEffect(soundEffect);
						openOptionsMenu();
					}
				});
	}

	public class CardAdapter extends CardScrollAdapter {

		private final List<CardBuilder> mCards;
		private Context myContext;
		private final File[] myListFiles;

		public CardAdapter(Context context, List<CardBuilder> cards,
				File[] listFiles) {
			mCards = cards;
			myContext = context;
			myListFiles = listFiles;
		}

		@Override
		public int getCount() {
			return mCards.size();
		}

		@Override
		public Object getItem(int position) {
			return mCards.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = null;
			LayoutInflater inflater = (LayoutInflater) myContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			File image = listImages[position];
			// Bitmap bitmap =
			// decodeSampledBitmapFromData(image.getAbsolutePath());

			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = inflater.inflate(R.layout.image_item, parent,
						false);
				viewHolder.imageView = (ImageView) convertView
						.findViewById(R.id.imageView1);
				viewHolder.uri = listImages[position].getAbsolutePath();
				convertView.setTag(viewHolder);
			}
			viewHolder = (ViewHolder) convertView.getTag();
			new DownloadAsyncTask().execute(viewHolder);
			return convertView;
		}

		@Override
		public int getViewTypeCount() {
			return CardBuilder.getViewTypeCount();
		}

		@Override
		public int getItemViewType(int position) {
			return mCards.get(position).getItemViewType();
		}

		@Override
		public int getPosition(Object item) {
			for (int i = 0; i < mCards.size(); i++) {
				if (getItem(i).equals(item)) {
					return i;
				}
			}
			return AdapterView.INVALID_POSITION;
		}

		/**
		 * A non-adapter method to append a card at the end without notifying
		 * the adapter of the data change yet ({@link #notifyDataSetChanged}).
		 */
		public void appendCardWithoutNotification(CardBuilder card) {
			mCards.add(card);
		}
	}

	private static class ViewHolder {
		ImageView imageView;
		String uri;
		Bitmap bitmap;
	}

	private class DownloadAsyncTask extends
			AsyncTask<ViewHolder, Void, ViewHolder> {

		@Override
		protected ViewHolder doInBackground(ViewHolder... params) {
			// TODO Auto-generated method stub
			// load image directly
			ViewHolder viewHolder = params[0];
			viewHolder.bitmap = decodeSampledBitmapFromData(viewHolder.uri);

			return viewHolder;
		}

		@Override
		protected void onPostExecute(ViewHolder result) {
			// TODO Auto-generated method stub
			if (result.bitmap == null) {
				result.imageView.setImageResource(R.drawable.black_bg);
			} else {
				result.imageView.setImageBitmap(result.bitmap);
			}
		}
	}

	public static Bitmap decodeSampledBitmapFromData(String path) {

		// First decode with inJustDecodeBounds=true to check dimensions
		Bitmap b = BitmapFactory.decodeFile(path);
		return Bitmap.createScaledBitmap(b, 640, 360, true);
	}

	private void deletePic(String IMAGE_FILE_NAME) {
		File file = new File(IMAGE_FILE_NAME);

		// TODO : May crash here
		if (file.delete()) {
			list = createCards(this);
			mAdapter.notifyDataSetChanged();
			Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.detail_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection.
		String path = listImages[currentPos].getAbsolutePath();
		switch (item.getItemId()) {
		case R.id.share_item:
			share(path);
			return true;
		case R.id.delete_item:
			deletePic(path);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
