package com.google.android.glass.leon.camera;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.content.Intents;
import com.google.android.glass.leon.camera.R;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

public class PictureActivity extends Activity {

	// App responds to voice trigger "test the camera", takes a picture with
	// GlassSnapshotActivity and then returns.

	private static final String SERVER = "http://192.168.15.11:8080/leon/picture/cut";
	public static final String SERVICE_PICTURE = "service_picture";
	private static final String TAG = "TAG";
	private static final int TAKE_PHOTO_CODE = 1;
	private static final int TAKE_ACTION = 3;
	private static final int TAKE_PICTURE_REQUEST = 2;
	private String IMAGE_NAME = "ImageTest.jpg";
	private String IMAGE_FILE_NAME = "/sdcard/ImageTest.jpg";
	public static String ON_RESULT_FILE_PATH = "on_result";
	public static String ON_RESULT_FILE_NAME = "on_result";

	private boolean picTaken = false; // flag to indicate if we just returned
										// from the picture taking intent
	private boolean fromService = false;

	private ProgressBar myProgressBar;
	private TextView message;
	protected boolean mbActive;

	private String inputQueryString;
	private String queryCategory;

	final Handler myHandler = new Handler(); // handles looking for the returned
												// image file
	private int numberOfImageFileAttempts = 0;

	private String responseBody = "";

	// private TextToSpeech mSpeech;

	private boolean readyForMenu = false;
	private boolean gotImageMatch = false;

	// private TextToSpeech tts;
	private boolean initialized = false;
	private String queuedText;

	private GestureDetector mGestureDetector;

	private boolean TAKE_BY_INTENT = false;

	public static String GALLERY_PATH = Environment
			.getExternalStorageDirectory().getPath() + "/in_my_hand/";
	// "/leon_sample/";
	// Index of api demo cards.
	// Visible for testing.
	static final int CARD_BUILDER = 0;
	static final int GESTURE_DETECTOR = 1;
	static final int THEMING = 2;
	static final int OPENGL = 3;
	static final int VOICE_MENU = 4;

	// private CardScrollAdapter mAdapter;
	// private CardScrollView mCardScroller;

	// Visible for testing.
	// CardScrollView getScroller() {
	// return mCardScroller;
	// }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "creating activity");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				int fromSer = bundle.getInt(SERVICE_PICTURE);
				if (fromSer == 1) {
					fromService = true;
				}
			}
		}

		// Create gallery folder
		if (!createGalleryFolder()) {
			Toast.makeText(this, "Failed to create private gallery",
					Toast.LENGTH_SHORT).show();
			finish();
		}
		Log.i("TAG", Environment.getExternalStorageDirectory().getPath());
		// text1 = (TextView) findViewById(R.id.text1);
		// text2 = (TextView) findViewById(R.id.text2);
		// text1.setText("");
		// text2.setText("");
		myProgressBar = (ProgressBar) findViewById(R.id.my_progressBar);
		// LinearLayout llResult = (LinearLayout)
		// findViewById(R.id.resultLinearLayout);
		// TextView tvResult = (TextView) findViewById(R.id.tap_instruction);
		// llResult.setVisibility(View.INVISIBLE);
		// tvResult.setVisibility(View.INVISIBLE);
		myProgressBar.setVisibility(View.VISIBLE);
		message = (TextView) findViewById(R.id.message);

		// Even though the text-to-speech engine is only used in response to a
		// menu action, we
		// initialize it when the application starts so that we avoid delays
		// that could occur
		// if we waited until it was needed to start it up
		// mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
		// @Override
		// public void onInit(int status) {
		// // Do nothing.
		// }
		// });
		// tts = new TextToSpeech(this /* context */, this /* listener */);
		mGestureDetector = createGestureDetector(this);

	}

	private boolean createGalleryFolder() {
		File folder = new File(GALLERY_PATH);
		boolean success = true;
		if (!folder.exists()) {
			success = folder.mkdir();
		}
		if (success) {
			return true;
		} else {
			return false;
		}
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
			// ArrayList<String> voiceResults = getIntent().getExtras()
			// .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
			// if (voiceResults != null && voiceResults.size() > 0) {
			// String spokenText = voiceResults.get(0);
			//
			// TextView capturedSpeechToTextViewObj = ((TextView)
			// findViewById(R.id.speakNow));
			// capturedSpeechToTextViewObj.setText(spokenText);
			// }
			if (!TAKE_BY_INTENT) {
				Log.i("TAG", "------- Start Glass -----");
				Calendar calendar = Calendar.getInstance();
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

		}
	}

	@Override
	public void onStop() {
		if (fromService) {
			Intent menuIntent = new Intent(this, ApiDemoActivity.class);
			menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(menuIntent);
		}
		super.onStop();
	}

	/*
	 * Send generic motion events to the gesture detector
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		// speak("Hello Glass");
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
					getWindow().invalidatePanelMenu(
							WindowUtils.FEATURE_VOICE_COMMANDS);
					return true;
				} else if (gesture == Gesture.SWIPE_RIGHT) {
					getWindow().invalidatePanelMenu(
							WindowUtils.FEATURE_VOICE_COMMANDS);
					return true;
				} else if (gesture == Gesture.SWIPE_LEFT) {
					getWindow().invalidatePanelMenu(
							WindowUtils.FEATURE_VOICE_COMMANDS);
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
			if (!TAKE_BY_INTENT) {
				Log.i("TAG", "------- Start Glass -----");
				Calendar calendar = Calendar.getInstance();
				Intent intent = new Intent(this, GlassSnapshotActivity.class);
				intent.putExtra("imageFileName", IMAGE_FILE_NAME);
				intent.putExtra("previewWidth", 640);
				intent.putExtra("previewHeight", 360);
				intent.putExtra("snapshotWidth", 1280);
				intent.putExtra("snapshotHeight", 720);
				intent.putExtra("maximumWaitTimeForCamera", 2000);
				startActivityForResult(intent, TAKE_PHOTO_CODE);
			} else {
				takePicture();
			}
			return true;
		case R.id.menu_gallery:
			Intent galIntent = new Intent(this, GalleryActivity.class);
			galIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(galIntent);
		case R.id.menu_back_main:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing else to do, closing the activity.
		finish();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		picTaken = true;
		if (!TAKE_BY_INTENT) {
			switch (requestCode) {
			case (TAKE_PHOTO_CODE): {
				if (resultCode == Activity.RESULT_OK) {
					// TODO Extract the data returned from the child Activity.
					Log.v(TAG, "onActivityResult");
					Bundle bundle = data.getExtras();
					IMAGE_FILE_NAME = bundle.getString(ON_RESULT_FILE_PATH);
					Log.i("TAG", "---- " + IMAGE_FILE_NAME);
					IMAGE_NAME = bundle.getString(ON_RESULT_FILE_NAME);
					Log.i("TAG", "---- " + IMAGE_NAME);
					File f = new File(IMAGE_FILE_NAME);
					if (f.exists()) {
						Log.v(TAG, "image file from camera was found");

						new AsyncTask<Void, Void, Bitmap>() {

							@Override
							protected Bitmap doInBackground(Void... params) {
								Bitmap b = BitmapFactory
										.decodeFile(IMAGE_FILE_NAME);
								b = Bitmap
										.createScaledBitmap(b, 640, 360, true);
								Log.v(TAG, "bmp width=" + b.getWidth()
										+ " height=" + b.getHeight());

								return b;
							}

							@Override
							protected void onPostExecute(Bitmap result) {
								// myProgressBar.setVisibility(View.GONE);
								// message.setText("Done");
								if (result != null) {
									// setPicToView(result);
									ImageView imageView = (ImageView) findViewById(R.id.bgPhoto);
									imageView.setImageBitmap(result);
								}

								// if (false)
								new AsyncTask<Void, Void, Bitmap>() {

									@Override
									protected Bitmap doInBackground(
											Void... params) {
										MultipartEntity multipartEntity = getBitmapAndPost();
										return multipost(SERVER,
												multipartEntity);
									}

									@Override
									protected void onPostExecute(Bitmap result) {
										super.onPostExecute(result);
										if (result != null) {
											myProgressBar
													.setVisibility(View.GONE);
											message.setText("");
											Toast.makeText(getBaseContext(),
													"Processing done",
													Toast.LENGTH_SHORT).show();
											setPicToView(result);
											startService(new Intent(
													getBaseContext(),
													GlassService.class));
										} else {
											Toast.makeText(getBaseContext(),
													"Failed to Load Glass",
													Toast.LENGTH_SHORT).show();
											myProgressBar
													.setVisibility(View.GONE);
											message.setText("Failed to process picture :( !");

											// File bitmapFile = new
											// File(IMAGE_FILE_NAME);
											// Bitmap bitmap =
											// BitmapFactory.decodeFile(bitmapFile
											// .getAbsolutePath());
											// setPicToView(bitmap);
										}
										// startActivityForResult(
										// new Intent(
										// RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
										// TAKE_ACTION);

									}

								}.execute();

								super.onPostExecute(result);
							}

						}.execute();

					}
				} else {
					Log.v(TAG, "onActivityResult returned bad result code");
					myProgressBar.setVisibility(View.GONE);
					message.setText("Nothing have recorded \n Will exit after 2 seconds");
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						finish();
					}
				}
				break;
			}
			case TAKE_ACTION:
				ArrayList<String> voiceResults = data.getExtras()
						.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
				if (voiceResults != null && voiceResults.size() > 0) {
					String spokenText = voiceResults.get(0);
					if (spokenText.contains("s")) {
						sharePic();
					} else if (spokenText.contains("d")) {
						deletePic();
					}
				}

				break;
			default:
				// finish();
				myProgressBar.setVisibility(View.GONE);
				message.setText("Nothing have recorded");
				break;
			}
		} else {
			if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
				String thumbnailPath = data
						.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
				String picturePath = data
						.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

				processPictureWhenReady(picturePath);
				// finish();
				// TODO: Show the thumbnail to the user while the full picture
				// is
				// being
				// processed.
			}

		}
		// Start to send pic to server
		// Should place on result_cod : 'Gie = OK
		// new AsyncTask<Void, Void, Void>() {
		// @Override
		// protected Void doInBackground(Void... params) {
		// String glass = null;
		// try {
		// glass = excutePost(SERVER, "give me your glass");
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// Log.i("TAG", "----------> " + glass);
		// return null;
		// }
		// }.execute();
	}

	public static Bitmap decodeSampledBitmapFromData(byte[] data, int reqWidth,
			int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		boolean debug = true;
		if (debug) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);
			options.inSampleSize = 4; // saved image will be one half the width
										// and
										// height of the original (image
										// captured is
										// double the resolution of the screen
										// size)
			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;

			return BitmapFactory.decodeByteArray(data, 0, data.length, options);
		} else {
			return BitmapFactory.decodeByteArray(data, 0, data.length);
		}
	}

	private void takePicture() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, TAKE_PHOTO_CODE);
	}

	public String excutePost(String targetURL, String text) throws IOException {
		URL url = new URL(targetURL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");

		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("text", text));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs,
				"UTF-8");
		entity.writeTo(connection.getOutputStream());
		connection.connect();

		// Get Response
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuffer response = new StringBuffer();
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append('\n');
		}
		rd.close();
		connection.disconnect();
		return response.toString();
	}

	@Override
	protected void onDestroy() {

		// Close the Text to Speech Library
		// if (mSpeech != null) {
		// mSpeech.stop();
		// mSpeech.shutdown();
		// mSpeech = null;
		// Log.d(TAG, "TTS Destroyed");
		// }
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

	private MultipartEntity getBitmapAndPost() {
		File bitmapFile = new File(IMAGE_FILE_NAME);
		Log.i("TAG", "---- " + IMAGE_FILE_NAME);
		Log.i("TAG", "PATH : " + bitmapFile.getAbsolutePath());
		Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
		ContentBody contentPart = new ByteArrayBody(bos.toByteArray(),
				IMAGE_FILE_NAME);
		MultipartEntity reqEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
		reqEntity.addPart("file", contentPart);
		return reqEntity;
	}

	private Bitmap multipost(String urlString, MultipartEntity reqEntity) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);

			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.addRequestProperty("Content-length",
					reqEntity.getContentLength() + "");
			conn.addRequestProperty(reqEntity.getContentType().getName(),
					reqEntity.getContentType().getValue());

			OutputStream os = conn.getOutputStream();
			reqEntity.writeTo(conn.getOutputStream());
			os.close();
			conn.connect();

			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return (BitmapFactory.decodeStream(conn.getInputStream()));
			}

		} catch (Exception e) {
			Log.e("TAG", "multipart post error " + e + "(" + urlString + ")");
		}
		return null;
	}

	private void setPicToView(Bitmap bitmap) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(IMAGE_FILE_NAME);
			final BufferedOutputStream bos = new BufferedOutputStream(fos, 8192);
			bitmap.compress(CompressFormat.JPEG, 100, bos);
			bos.flush();
			bos.close();
			fos.close();
			Bitmap b = Bitmap.createScaledBitmap(bitmap, 640, 360, true);
			ImageView imageView = (ImageView) findViewById(R.id.bgPhoto);
			imageView.setImageBitmap(b);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void sendGoogleGlass() throws IOException {
		File bitmapFile = new File(IMAGE_FILE_NAME);
		Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
		String attachmentName = IMAGE_NAME;
		String attachmentFileName = IMAGE_NAME;
		String crlf = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		HttpURLConnection httpUrlConnection = null;
		URL url = new URL(SERVER);
		httpUrlConnection = (HttpURLConnection) url.openConnection();
		httpUrlConnection.setUseCaches(false);
		httpUrlConnection.setDoOutput(true);

		httpUrlConnection.setRequestMethod("POST");
		httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
		httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
		httpUrlConnection.setRequestProperty("Content-Type",
				"multipart/form-data;boundary=" + boundary);

		DataOutputStream request = new DataOutputStream(
				httpUrlConnection.getOutputStream());

		request.writeBytes(twoHyphens + boundary + crlf);
		request.writeBytes("Content-Disposition: form-data; name=\""
				+ attachmentName + "\";filename=\"" + attachmentFileName + "\""
				+ crlf);
		request.writeBytes(crlf);
		byte[] pixels = new byte[bitmap.getWidth() * bitmap.getHeight()];
		for (int i = 0; i < bitmap.getWidth(); ++i) {
			for (int j = 0; j < bitmap.getHeight(); ++j) {
				pixels[i + j] = (byte) ((bitmap.getPixel(i, j)));
			}
		}
		request.write(pixels);
		request.writeBytes(crlf);
		request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
		request.flush();
		request.close();

		// get response

		InputStream responseStream = new BufferedInputStream(
				httpUrlConnection.getInputStream());

		BufferedReader responseStreamReader = new BufferedReader(
				new InputStreamReader(responseStream));
		String line = "";
		StringBuilder stringBuilder = new StringBuilder();
		while ((line = responseStreamReader.readLine()) != null) {
			stringBuilder.append(line).append("\n");
		}
		responseStreamReader.close();

		String response = stringBuilder.toString();

	}

	// @Override
	// public void onInit(int status) {
	// if (status == TextToSpeech.SUCCESS) {
	// initialized = true;
	// tts.setLanguage(Locale.ENGLISH);
	//
	// if (queuedText != null) {
	// speak(queuedText);
	// }
	// }
	//
	// }

	// public void speak(String text) {
	// // If not yet initialized, queue up the text.
	// if (!initialized) {
	// queuedText = text;
	// return;
	// }
	// queuedText = null;
	// // Before speaking the current text, stop any ongoing speech.
	// tts.stop();
	// // Speak the text.
	// tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	// }

	private void sharePic() {
		// speak("Share");
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, IMAGE_FILE_NAME);
		shareIntent.setType("image/jpeg");
		startActivity(Intent.createChooser(shareIntent, "Share with"));
	}

	private void deletePic() {
		File file = new File(IMAGE_FILE_NAME);
		if (file.delete()) {
			message.setText("Deleted");
			// speak("Deleted");
		} else {
			// speak("Failed to delete image");
			message.setText("Failed to delete :( !");
		}
		ImageView imageView = (ImageView) findViewById(R.id.bgPhoto);
		imageView.setImageBitmap(null);
		imageView.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.black_bg));

	}

}