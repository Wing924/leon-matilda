package com.google.android.glass.leon.camera;

import java.io.File;
import java.util.Random;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

public class GlassService extends Service {

	private static final String LIVE_CARD_TAG = "LiveCardDemo";

	private LiveCard mLiveCard;
	private RemoteViews mLiveCardView;

	private int homeScore, awayScore;
	private Random mPointsGenerator;

	private final Handler mHandler = new Handler();
	private final UpdateLiveCardRunnable mUpdateLiveCardRunnable = new UpdateLiveCardRunnable();
	private static final long DELAY_MILLIS = 30000;

	private String currentUri = "";

	@Override
	public void onCreate() {
		super.onCreate();
		mPointsGenerator = new Random();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("TAG", "Start Command");
		if (mLiveCard == null) {

			// Get an instance of a live card
			mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
		}
		// Inflate a layout into a remote view
		mLiveCardView = new RemoteViews(getPackageName(),
				R.layout.service_image_item);

		// Set up initial RemoteViews values
		homeScore = 0;
		awayScore = 0;
		Log.i("TAG", "Start Command 2");
		File gallery = new File(PictureActivity.GALLERY_PATH);
		final File[] listImages1 = gallery.listFiles();
		if (listImages1 == null || listImages1.length == 0) {

		} else {
			Log.i("TAG", "Start Command 3");
			currentUri = listImages1[listImages1.length - 1].getAbsolutePath();
			new AsyncTask<Void, Void, Bitmap>() {

				@Override
				protected Bitmap doInBackground(Void... params) {
					Bitmap bitmap = BitmapFactory
							.decodeFile(listImages1[listImages1.length - 1]
									.getAbsolutePath());
					Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
							640, 360, true);

					return scaledBitmap;
				}

				@Override
				protected void onPostExecute(Bitmap result) {
					if (result != null)
						mLiveCardView.setImageViewBitmap(R.id.serviceImage,
								result);
					super.onPostExecute(result);
				}

			}.execute();

		}
		// Set up the live card's action with a pending intent
		// to show a menu when tapped
		Intent menuIntent = new Intent(this, ServiceMenuActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("bundle", currentUri);
		menuIntent.putExtras(bundle);
		mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
		if (mLiveCard.isPublished()) {
			mLiveCard.unpublish();
		}
		// Publish the live card
		mLiveCard.publish(PublishMode.SILENT);

		// Queue the update text runnable
		mHandler.post(mUpdateLiveCardRunnable);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {
			// Stop the handler from queuing more Runnable jobs
			mUpdateLiveCardRunnable.setStop(true);

			mLiveCard.unpublish();
			mLiveCard = null;
		}
		super.onDestroy();
	}

	/**
	 * Runnable that updates live card contents
	 */
	private class UpdateLiveCardRunnable implements Runnable {

		private boolean mIsStopped = false;

		/*
		 * Updates the card with a fake score every 30 seconds as a
		 * demonstration. You also probably want to display something useful in
		 * your live card.
		 * 
		 * If you are executing a long running task to get data to update a live
		 * card(e.g, making a web call), do this in another thread or AsyncTask.
		 */
		public void run() {
			if (!isStopped()) {
				// Generate fake points.
				homeScore += mPointsGenerator.nextInt(3);
				awayScore += mPointsGenerator.nextInt(3);

				// Update the remote view with the new scores.
				// mLiveCardView.setTextViewText(R.id.home_team_name_text_view,
				// String.valueOf(homeScore));
				// Always call setViews() to update the live card's RemoteViews.
				mLiveCard.setViews(mLiveCardView);

				// Queue another score update in 30 seconds.
				mHandler.postDelayed(mUpdateLiveCardRunnable, DELAY_MILLIS);
			}
		}

		public boolean isStopped() {
			return mIsStopped;
		}

		public void setStop(boolean isStopped) {
			this.mIsStopped = isStopped;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		/*
		 * If you need to set up interprocess communication (activity to a
		 * service, for instance), return a binder object so that the client can
		 * receive and modify data in this service.
		 * 
		 * A typical use is to give a menu activity access to a binder object if
		 * it is trying to change a setting that is managed by the live card
		 * service. The menu activity in this sample does not require any of
		 * these capabilities, so this just returns null.
		 */
		return null;
	}
}