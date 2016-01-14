package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
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
    private ArrayList<String> memoPaths = new ArrayList<>();

    private String patient = null;
    private String receiver = null;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState");

        // Save the user's current game state
        savedInstanceState.putStringArrayList(STATE_VIDEOS, videoPaths);
        savedInstanceState.putStringArrayList(STATE_PICTURES, imagePaths);
        savedInstanceState.putStringArrayList(STATE_MEMOS, memoPaths);
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
            memoPaths = savedInstanceState.getStringArrayList(STATE_MEMOS);
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
     * Launch the finish report intent.
     */
    private void finishReport(String email, String subject) {
        ArrayList<String> mediaPaths = new ArrayList<>();
        mediaPaths.addAll(imagePaths);
        mediaPaths.addAll(videoPaths);
        mediaPaths.addAll(memoPaths);

        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra("recipient_email", email);
        intent.putExtra("subject", subject);
        intent.putExtra("media_files", mediaPaths);
        intent.putExtra("text", "Patient: " + patient);
        startActivityForResult(intent, FINISH_REPORT_REQUEST);
    }

    /*
     * Save state.
     */
    private void saveState() {
        String serializedVideoPaths = (new JSONArray(videoPaths)).toString();
        String serializedImagePaths = (new JSONArray(imagePaths)).toString();
        String serializedMemoPaths = (new JSONArray(memoPaths)).toString();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(STATE_VIDEOS, serializedVideoPaths);
        editor.putString(STATE_PICTURES, serializedImagePaths);
        editor.putString(STATE_MEMOS, serializedMemoPaths);
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
        String serializedMemoPaths = sharedPref.getString(STATE_MEMOS, "[]");

        imagePaths = new ArrayList<>();
        videoPaths = new ArrayList<>();
        memoPaths = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(serializedVideoPaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                videoPaths.add(jsonArray.getString(i));
            }

            jsonArray = new JSONArray(serializedImagePaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                imagePaths.add(jsonArray.getString(i));
            }

            jsonArray = new JSONArray(serializedMemoPaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                memoPaths.add(jsonArray.getString(i));
            }
        }
        catch (JSONException e) {
            // ignore
        }

        Log.i(TAG, "Restored patient: " + patient);
        Log.i(TAG, "Restored receiver: " + receiver);
        Log.i(TAG, "Restored imagePaths: " + imagePaths);
        Log.i(TAG, "Restored videoPaths: " + videoPaths);
        Log.i(TAG, "Restored memoPaths: " + memoPaths);

        updateUI();
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

            imagePaths.add(data.getStringExtra("path"));
            saveState();
            updateUI();
        }
        else if (requestCode == RECORD_VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received video: " + data.getStringExtra("path"));

            videoPaths.add(data.getStringExtra("path"));
            saveState();
            updateUI();
        } else if (requestCode == RECORD_MEMO_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received memo: " + data.getStringExtra("path"));

            memoPaths.add(data.getStringExtra("path"));
            saveState();
            updateUI();
        }
        else if (requestCode == SCAN_PATIENT_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received patient QR: " + data.getStringExtra("result"));

            patient = data.getStringExtra("result");

            saveState();
            updateUI();
        }
        else if (requestCode == SCAN_RECEIVER_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received receiver QR: " + data.getStringExtra("result"));

            String mail = data.getStringExtra("result");

            // Check for valid email
            if (EmailValidator.getInstance().isValid(mail)) {
                receiver = mail;

                saveState();
                updateUI();
            }
            else {
                Toast.makeText(getApplicationContext(), "Invalid receiver: " + mail + ". Scan again.", Toast.LENGTH_LONG).show();
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
    private void updateTextField(int id, String value, Integer color) {
        TextView v = (TextView) findViewById(id);
        if(value != null) {
            v.setText(value);
        }
        if (color != null) {
            v.setTextColor(color);
        }
        v.invalidate();
   }

    /**
     * Update the UI.
     */
    private void updateUI() {
        updateTextField(R.id.imageNumber, String.valueOf(imagePaths.size()), imagePaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.imageLabel, null, imagePaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.videoNumber, String.valueOf(videoPaths.size()), videoPaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.videoLabel, null, videoPaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.memoNumber, String.valueOf(memoPaths.size()), memoPaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.memoLabel, null, memoPaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.receiverIdentifier, receiver, receiver != null ? Color.WHITE : null);
        updateTextField(R.id.patientIdentifier, patient, patient != null ? Color.WHITE : null);
    }
}
