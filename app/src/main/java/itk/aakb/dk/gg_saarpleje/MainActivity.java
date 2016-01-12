package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.view.WindowUtils;

import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity {
    public static final String FILE_DIRECTORY = "saarpleje";

    private static final String TAG = "saarpleje MainActivity";
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_PATIENT_REQUEST = 103;
    private static final int SCAN_RECEIVER_REQUEST = 104;
    private static final int FINISH_REPORT_REQUEST = 105;
    private static final int RECORD_MEMO_REQUEST = 106;
    private static final String STATE_VIDEOS = "videos";
    private static final String STATE_PICTURES = "pictures";
    private static final String STATE_PATIENT = "patient";
    private static final String STATE_RECEIVER = "receiver";
    private static final String STATE_MEMOS = "memos";

    private ArrayList<String> imagePaths = new ArrayList<>();
    private ArrayList<String> videoPaths = new ArrayList<>();
    private ArrayList<String> audioPaths = new ArrayList<>();

    private String patient = null;
    private String receiver = null;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState");

        // Save the user's current game state
        savedInstanceState.putStringArrayList(STATE_VIDEOS, videoPaths);
        savedInstanceState.putStringArrayList(STATE_PICTURES, imagePaths);
        savedInstanceState.putStringArrayList(STATE_MEMOS, audioPaths);
        savedInstanceState.putString(STATE_PATIENT, patient);
        savedInstanceState.putString(STATE_RECEIVER, receiver);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * On create.
     *
     * @param savedInstanceState the bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Requests a voice menu on this activity. As for any other
        // window feature, be sure to request this before
        // setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Set the main activity view.
        setContentView(R.layout.activity_layout);

        // for debug: list all files in directory
        // @TODO: remove
        getDirectoryListing();

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            Log.i(TAG, "Restoring savedInstance");

            // Restore state members from saved instance
            imagePaths = savedInstanceState.getStringArrayList(STATE_PICTURES);
            videoPaths = savedInstanceState.getStringArrayList(STATE_VIDEOS);
            patient = savedInstanceState.getString(STATE_PATIENT);
            receiver = savedInstanceState.getString(STATE_RECEIVER);
        } else {
            Log.i(TAG, "Restoring state");

            // Probably initialize members with default values for a new instance
            restoreState();
        }
    }

    /**
     * On create panel menu.
     *
     * @param featureId the feature id
     * @param menu the menu to create
     *
     * @return boolean
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            if (receiver != null && patient != null) {
                getMenuInflater().inflate(R.menu.main, menu);
            }
            else {
                getMenuInflater().inflate(R.menu.start, menu);
            }

            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * On create options menu.
     *
     * @param menu The menu to create
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (receiver != null && patient != null) {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.start, menu);
        }

        return true;
    }

    /**
     * On menu item selected.
     * <p/>
     * Processes the voice commands from the main menu.
     *
     * @param featureId the feature id
     * @param item the selected menu item
     *
     * @return boolean
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.take_image_menu_item:
                    Log.i(TAG, "menu: take before image");

                    takePicture();

                    break;
                case R.id.record_video_menu_item:
                    Log.i(TAG, "menu: record video");

                    break;
                case R.id.record_video_menu_item_30_seconds:
                    Log.i(TAG, "menu: record 30 seconds video");

                    recordVideo(30);

                    break;
                case R.id.record_video_menu_item_1_minute:
                    Log.i(TAG, "menu: record 1 minute video");

                    recordVideo(60);

                    break;
                case R.id.record_video_menu_item_2_minutes:
                    Log.i(TAG, "menu: record 2 minutes video");

                    recordVideo(120);

                    break;
                case R.id.record_video_menu_item_4_minutes:
                    Log.i(TAG, "menu: record 4 minutes video");

                    recordVideo(240);

                    break;
                case R.id.record_video_menu_item_unlimited:
                    Log.i(TAG, "menu: record unlimited video");

                    recordVideo(true);

                    break;
                case R.id.record_memo_menu_item:
                    Log.i(TAG, "menu: record memo");

                    recordMemo();

                    break;
                case R.id.finish_menu_item:
                    Log.i(TAG, "menu: finish report");

                    finishReport(receiver, "Sårplejerapport – " + new Date());

                    break;
                case R.id.confirm_cancel:
                    Log.i(TAG, "menu: Confirm: cancel and exit");

                    cleanDirectory();
                    deleteState();

                    finish();

                    break;
                case R.id.scan_patient_menu_item:
                    Intent scanPatientIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(scanPatientIntent, SCAN_PATIENT_REQUEST);

                    break;
                case R.id.add_receiver_menu_item:
                    Intent addReceiverIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(addReceiverIntent, SCAN_RECEIVER_REQUEST);

                    break;
                default:
                    return true;
            }
            return true;
        }

        // Pass through to super if not handled
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Launch the image capture intent.
     */
    private void takePicture() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    /**
     * Launch the record video intent.
     *
     * @param unlimited whether or not the video should be unlimited.
     */
    private void recordVideo(boolean unlimited) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("UNLIMITED", unlimited);
        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
    }

    /**
     * Launch the record video intent.
     *
     * @param duration the duration of the video to record
     */
    private void recordVideo(int duration) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("SECONDS", duration);
        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
    }

    /**
<<<<<<< HEAD
     * Launch the finish report intent.
     */
    private void finishReport(String email, String subject) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra("recipient_email", email)
              .putExtra("subject", subject)
              .putExtra("text", "Patient: " + patient);
        startActivityForResult(intent, FINISH_REPORT_REQUEST);
    }

    /*
=======
<<<<<<< .merge_file_e0Eo27
>>>>>>> development
     * Save state.
     */
    private void saveState() {
        String serializedVideoPaths = (new JSONArray(videoPaths)).toString();
        String serializedImagePaths = (new JSONArray(imagePaths)).toString();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(STATE_VIDEOS, serializedVideoPaths);
        editor.putString(STATE_PICTURES, serializedImagePaths);
        editor.putString(STATE_PATIENT, patient);
        editor.putString(STATE_RECEIVER, receiver);
        editor.apply();
    }

    /**
     * Remove state.
     */
    private void deleteState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Restore state.
     */
    private void restoreState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        patient = sharedPref.getString(STATE_PATIENT, null);
        receiver = sharedPref.getString(STATE_RECEIVER, null);
        String serializedVideoPaths = sharedPref.getString(STATE_VIDEOS, "[]");
        String serializedImagePaths = sharedPref.getString(STATE_PICTURES, "[]");

        imagePaths = new ArrayList<>();
        videoPaths = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(serializedVideoPaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                videoPaths.add(jsonArray.getString(i));
            }

            jsonArray = new JSONArray(serializedImagePaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                imagePaths.add(jsonArray.getString(i));
            }
        }
        catch (JSONException e) {
            // ignore
        }

        Log.i(TAG, "Restored patient: " + patient);
        Log.i(TAG, "Restored receiver: " + receiver);
        Log.i(TAG, "Restored imagePaths: " + imagePaths);
        Log.i(TAG, "Restored videoPaths: " + videoPaths);

        updateTextField(R.id.imageNumber, String.valueOf(imagePaths.size()));
        updateTextField(R.id.videoNumber, String.valueOf(videoPaths.size()));
        updateTextField(R.id.memoNumber, String.valueOf((audioPaths.size())));
        updateTextField(R.id.receiverIdentifier, receiver);
        updateTextField(R.id.patientIdentifier, patient);
    }

    /**
     * Empty the directory.
     */
    private void cleanDirectory() {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FILE_DIRECTORY);
        Log.i(TAG, "Cleaning directory: " + f.getAbsolutePath());

        File[] files = f.listFiles();
        if (files != null && files.length > 0) {
            for (File inFile : files) {
                boolean success = inFile.delete();
                if (!success) {
                    Log.e(TAG, "file: " + inFile + " was not deleted (continuing).");
                }
            }
        }
        else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    /**
     * List all files in FILE_DIRECTORY.
     */
    private void getDirectoryListing() {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FILE_DIRECTORY);
        Log.i(TAG, "Listing files in: " + f.getAbsolutePath());

        File[] files = f.listFiles();
        if (files != null && files.length > 0) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    Log.i(TAG, inFile + "(dir)");
                } else {
                    Log.i(TAG, "" + inFile);
                }
            }
        } else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    /**
     * Launch the record memo intent.
     *
     */
    private void recordMemo() {
        Intent intent = new Intent(this, MemoActivity.class);
        startActivityForResult(intent, RECORD_MEMO_REQUEST);
    }

    /**
     * On activity result.
     * <p/>
     * When an intent returns, it is intercepted in this method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received image: " + data.getStringExtra("path"));

            processPictureWhenReady(data.getStringExtra("path"));
        }
        else if (requestCode == RECORD_VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received video: " + data.getStringExtra("path"));

            processVideoWhenReady(data.getStringExtra("path"));
        } else if (requestCode == RECORD_MEMO_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received memo: " + data.getStringExtra("path"));

            processAudioWhenReady(data.getStringExtra("path"));
        }
        else if (requestCode == SCAN_PATIENT_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received QR: " + data.getStringExtra("result"));

            patient = data.getStringExtra("result");

            saveState();

            TextView text = (TextView) findViewById(R.id.patientIdentifier);
            text.setText(patient);
            text.invalidate();
        }
        else if (requestCode == SCAN_RECEIVER_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received QR: " + data.getStringExtra("result"));

            String s = data.getStringExtra("result");

            boolean valid = EmailValidator.getInstance().isValid(s);

            if (valid) {
                receiver = s;

                saveState();

                TextView text = (TextView) findViewById(R.id.receiverIdentifier);
                text.setText(receiver);
                text.invalidate();
            }
            else {
                Toast.makeText(getApplicationContext(), "Invalid receiver: " + s + ". Scan again.", Toast.LENGTH_LONG).show();
            }
        }
        else if (requestCode == FINISH_REPORT_REQUEST && resultCode == RESULT_OK) {
            cleanDirectory();
            deleteState();
            Toast.makeText(getApplicationContext(), "Report sent.", Toast.LENGTH_LONG).show();
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Update a ui text view.
     * @param id id of the text view
     * @param value value to assign
     */
    private void updateTextField(int id, String value) {
        TextView v = (TextView) findViewById(id);
        v.setText(value);
        v.invalidate();
   }

    /**
     * Update the UI when a step has been completed.
     *
     * @param step that step that has been completed.
     */
    private void setStepAccept(int step) {
        Log.i(TAG, "Step " + step + " has been completed.");

        TextView textCountView = null;
        TextView textLabelView = null;

        if (step == 0) {
            textCountView = (TextView) findViewById(R.id.imageNumber);
            textCountView.setText(String.valueOf(imagePaths.size()));
        }
        else if (step == 1) {
            textCountView = (TextView) findViewById(R.id.videoNumber);
            textCountView.setText(String.valueOf(videoPaths.size()));

            textLabelView = (TextView) findViewById(R.id.videoLabel);
        } else if (step == 2) {
            textCountView = (TextView) findViewById(R.id.memoNumber);
            textCountView.setText(String.valueOf(audioPaths.size()));

            textLabelView = (TextView) findViewById(R.id.memoLabel);
        }

        if (textCountView != null) {
            textCountView.setTextColor(Color.WHITE);
            textCountView.invalidate();
        }
        if (textLabelView != null) {
            textLabelView.setTextColor(Color.WHITE);
            textLabelView.invalidate();
        }
    }

    /**
     * Process the picture.
     *
     * @param picturePath path to the image.
     */
    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            // The picture is ready. We are not gonna work with it, but now we know it has been
            // saved to disc.

            imagePaths.add(picturePath);

            saveState();

            setStepAccept(0);

            Log.i(TAG, "Before picture ready, with path: " + picturePath);
        }
        else {
            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
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


    /**
     * Process the video.
     *
     * @param videoPath path to the image.
     */
    private void processVideoWhenReady(final String videoPath) {
        final File videoFile = new File(videoPath);

        if (videoFile.exists()) {
            // The video is ready. We are not gonna work with it, but now we know it has been
            // saved to disc.
            videoPaths.add(videoPath);

            saveState();

            Log.i(TAG, "Video ready, with path: " + videoPath);

            setStepAccept(1);
        }
        else {
            final File parentDirectory = videoFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(videoFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processVideoWhenReady(videoPath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    /**
     * Process the audio.
     *
     * @param audioPath path to the image.
     */
    private void processAudioWhenReady(final String audioPath) {
        final File audioFile = new File(audioPath);

        if (audioFile.exists()) {
            // The video is ready. We are not gonna work with it, but now we know it has been
            // saved to disc.
            audioPaths.add(audioPath);

            Log.i(TAG, "Audio ready, with path: " + audioPath);

            setStepAccept(2);
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).
            // @TODO: Add progress bar. Return to main menu when video is ready.

            final File parentDirectory = audioFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(audioFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processAudioWhenReady(audioPath);
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
