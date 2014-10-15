package com.leon.glasscamerasnapshot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.darrenvenn.glasscamerasnapshot.R;
import com.google.android.glass.content.Intents;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class MainActivity extends Activity {

	// App responds to voice trigger "test the camera", takes a picture with
	// GlassSnapshotActivity and then returns.

	private static final String TAG = "TAG";
	private static final int TAKE_PHOTO_CODE = 1;
	private static final int TAKE_PICTURE_REQUEST = 2;
	private static final String IMAGE_FILE_NAME = "/sdcard/ImageTest.jpg";

	private boolean picTaken = false; // flag to indicate if we just returned
										// from the picture taking intent
	private String theImageFile = ""; // this holds the name of the image that
										// was returned by the camera

	private TextView text1;
	private TextView text2;

	private ProgressBar myProgressBar;
	protected boolean mbActive;

	private String inputQueryString;
	private String queryCategory;

	final Handler myHandler = new Handler(); // handles looking for the returned
												// image file
	private int numberOfImageFileAttempts = 0;

	private String responseBody = "";

	private TextToSpeech mSpeech;

	private boolean readyForMenu = false;
	private boolean gotImageMatch = false;

	// private GestureDetector mGestureDetector;

	private boolean TAKE_BY_INTENT = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "creating activity");

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_main);
		text1 = (TextView) findViewById(R.id.text1);
		text2 = (TextView) findViewById(R.id.text2);
		text1.setText("");
		text2.setText("");
		myProgressBar = (ProgressBar) findViewById(R.id.my_progressBar);
		LinearLayout llResult = (LinearLayout) findViewById(R.id.resultLinearLayout);
		TextView tvResult = (TextView) findViewById(R.id.tap_instruction);
		llResult.setVisibility(View.INVISIBLE);
		tvResult.setVisibility(View.INVISIBLE);
		myProgressBar.setVisibility(View.INVISIBLE);

		// Even though the text-to-speech engine is only used in response to a
		// menu action, we
		// initialize it when the application starts so that we avoid delays
		// that could occur
		// if we waited until it was needed to start it up
		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Do nothing.
			}
		});

		// mGestureDetector = createGestureDetector(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Here we launch our intent to take the snapshot.
		// You must specify the file name that you wish the image to be saved as
		// (imageFileName), in the extras for the intent,
		// along with the maximum amount of time to wish to wait to acquire the
		// camera (maximumWaitTimeForCamera - time in
		// milliseconds, e.g. 2000 = 2 seconds). This is done because the first
		// call to get the camera does not always
		// work (especially when the app is responding to a voice trigger) so
		// repeated calls are made until the camera is
		// acquired or we give up.
		// You must also specify the width and height of the preview image to
		// show, and also the width and height of the
		// image to be saved from the camera (Snapshot width and height). Valid
		// values are as follows:
		//
		// Preview Sizes
		// width=1920 height=1080
		// width=1280 height=960
		// width=1280 height=720
		// width=1024 height=768
		// width=1024 height=576
		// width=960 height=720
		// width=800 height=480
		// width=768 height=576
		// width=720 height=576
		// width=720 height=480
		// width=640 height=480
		// width=640 height=368
		// width=640 height=360
		// width=512 height=384
		// width=512 height=288
		// width=416 height=304
		// width=416 height=240
		// width=352 height=288
		// width=320 height=240
		// width=320 height=192
		// width=256 height=144
		// width=240 height=160
		// width=224 height=160
		// width=176 height=144
		// width=960 height=1280
		// width=720 height=1280
		// width=768 height=1024
		// width=576 height=1024
		// width=720 height=960
		// width=480 height=800
		// width=576 height=768
		// width=576 height=720
		// width=480 height=720
		// width=480 height=640
		// width=368 height=640
		// width=384 height=512
		// width=288 height=512
		// width=304 height=416
		// width=240 height=416
		// width=288 height=352
		// width=240 height=320
		// width=192 height=320
		// width=144 height=256
		// width=160 height=240
		// width=160 height=224
		// width=144 height=176
		//
		// Snapshot Sizes
		// width=2592 height=1944
		// width=2560 height=1888
		// width=2528 height=1856
		// width=2592 height=1728
		// width=2592 height=1458
		// width=2560 height=1888
		// width=2400 height=1350
		// width=2304 height=1296
		// width=2240 height=1344
		// width=2160 height=1440
		// width=2112 height=1728
		// width=2112 height=1188
		// width=2048 height=1152
		// width=2048 height=1536
		// width=2016 height=1512
		// width=2016 height=1134
		// width=2000 height=1600
		// width=1920 height=1080
		// width=1600 height=1200
		// width=1600 height=900
		// width=1536 height=864
		// width=1408 height=792
		// width=1344 height=756
		// width=1296 height=972
		// width=1280 height=1024
		// width=1280 height=720
		// width=1152 height=864
		// width=1280 height=960
		// width=1024 height=768
		// width=1024 height=576
		// width=640 height=480
		// width=320 height=240

		if (!picTaken) {
			if (!TAKE_BY_INTENT) {
				Log.i("TAG", "------- Start Glass -----");
				Intent intent = new Intent(this, GlassSnapshotActivity.class);
				intent.putExtra("imageFileName", IMAGE_FILE_NAME);
				intent.putExtra("previewWidth", 640);
				intent.putExtra("previewHeight", 360);
				intent.putExtra("snapshotWidth", 1280);
				intent.putExtra("snapshotHeight", 720);
				intent.putExtra("maximumWaitTimeForCamera", 2000);
				startActivityForResult(intent, 1);
			} else {
				takePicture();
			}
		} else {
			// do nothing
		}
	}

	/*
	 * Send generic motion events to the gesture detector
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		// if (mGestureDetector != null) {
		// return mGestureDetector.onMotionEvent(event);
		// }
		return false;
	}

	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		// Create a base listener for generic gestures
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					// do something on tap
					Log.v(TAG, "tap");
					// if (readyForMenu) {
					openOptionsMenu();
					// }
					return true;
				} else if (gesture == Gesture.TWO_TAP) {
					// do something on two finger tap
					return true;
				} else if (gesture == Gesture.SWIPE_RIGHT) {
					// do something on right (forward) swipe
					return true;
				} else if (gesture == Gesture.SWIPE_LEFT) {
					// do something on left (backwards) swipe
					return true;
				}
				return false;
			}
		});
		gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
			@Override
			public void onFingerCountChanged(int previousCount, int currentCount) {
				// do something on finger count changes
			}
		});
		gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
			@Override
			public boolean onScroll(float displacement, float delta,
					float velocity) {
				// do something on scrolling
				return false;
			}
		});
		return gestureDetector;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		picTaken = true;
		if (!TAKE_BY_INTENT) {
			switch (requestCode) {
			case (1): {
				if (resultCode == Activity.RESULT_OK) {
					// TODO Extract the data returned from the child Activity.
					Log.v(TAG, "onActivityResult");

					File f = new File(IMAGE_FILE_NAME);
					if (f.exists()) {
						Log.v(TAG, "image file from camera was found");

						Bitmap b = BitmapFactory.decodeFile(IMAGE_FILE_NAME);
						Log.v(TAG,
								"bmp width=" + b.getWidth() + " height="
										+ b.getHeight());
						ImageView image = (ImageView) findViewById(R.id.bgPhoto);
						image.setImageBitmap(b);

						text1 = (TextView) findViewById(R.id.text1);
						text2 = (TextView) findViewById(R.id.text2);
						text1.setText("The image shown was saved successfully to a file named:");
						text2.setText("\n" + IMAGE_FILE_NAME);

						LinearLayout llResult = (LinearLayout) findViewById(R.id.resultLinearLayout);
						llResult.setVisibility(View.VISIBLE);
						TextView line1 = (TextView) findViewById(R.id.titleOfWork);
						TextView line2 = (TextView) findViewById(R.id.Singer);
						TextView tap = (TextView) findViewById(R.id.tap_instruction);
						line1.setText("");
						line2.setText("");
						tap.setVisibility(View.VISIBLE);
					}
				} else {
					Log.v(TAG, "onActivityResult returned bad result code");
					// finish();
				}
				break;
			}
			}
		} else {
			if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
				String thumbnailPath = data
						.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
				String picturePath = data
						.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

				processPictureWhenReady(picturePath);
				// TODO: Show the thumbnail to the user while the full picture
				// is
				// being
				// processed.
			}

		}
		// Start to send pic to server
		// Should place on result_code = OK

	}

	private void takePicture() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, TAKE_PHOTO_CODE);
	}

	public static String excutePost(String targetURL, String urlParameters) {
		URL url;
		HttpURLConnection connection = null;
		try {
			// Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	@Override
	protected void onDestroy() {

		// Close the Text to Speech Library
		if (mSpeech != null) {
			mSpeech.stop();
			mSpeech.shutdown();
			mSpeech = null;
			Log.d(TAG, "TTS Destroyed");
		}
		super.onDestroy();
	}

	/*
	 * @Override protected void onActivityResult(int requestCode, int
	 * resultCode, Intent data) { if (requestCode == TAKE_PICTURE_REQUEST &&
	 * resultCode == RESULT_OK) { String thumbnailPath = data
	 * .getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH); String picturePath =
	 * data .getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
	 * 
	 * processPictureWhenReady(picturePath); // TODO: Show the thumbnail to the
	 * user while the full picture is // being // processed. }
	 * 
	 * super.onActivityResult(requestCode, resultCode, data); }
	 */
	private void processPictureWhenReady(final String picturePath) {
		final File pictureFile = new File(picturePath);

		if (pictureFile.exists()) {
			// The picture is ready; process it.
		} else {
			// The file does not exist yet. Before starting the file observer,
			// you
			// can update your UI to let the user know that the application is
			// waiting for the picture (for example, by displaying the thumbnail
			// image and a progress indicator).

			final File parentDirectory = pictureFile.getParentFile();
			FileObserver observer = new FileObserver(parentDirectory.getPath(),
					FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
				// Protect against additional pending events after CLOSE_WRITE
				// or MOVED_TO is handled.
				private boolean isFileWritten;

				@Override
				public void onEvent(int event, String path) {
					if (!isFileWritten) {
						// For safety, make sure that the file that was created
						// in
						// the directory is actually the one that we're
						// expecting.
						File affectedFile = new File(parentDirectory, path);
						isFileWritten = affectedFile.equals(pictureFile);

						if (isFileWritten) {
							stopWatching();

							// Now that the file is ready, recursively call
							// processPictureWhenReady again (on the UI thread).
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									processPictureWhenReady(picturePath);
								}
							});
						}
					}
				}
			};
			observer.startWatching();
		}
	}

}