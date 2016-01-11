package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.view.WindowUtils;

import org.apache.commons.validator.routines.EmailValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "saarpleje";
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_PATIENT_REQUEST = 103;
    private static final int SCAN_RECEIVER_REQUEST = 104;
    private static final int FINISH_REPORT_REQUEST = 105;

    private List<String> imagePaths = new ArrayList<>();
    private List<String> videoPaths = new ArrayList<>();
    private String patient = null;
    private String receiver = null;

    /**
     * On create.
     *
     * @param bundle the bundle
     */
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Requests a voice menu on this activity. As for any other
        // window feature, be sure to request this before
        // setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Set the main activity view.
        setContentView(R.layout.activity_layout);
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
                case R.id.finish_menu_item:
                    Log.i(TAG, "menu: finish report");

                    finishReport("googleglass@mikkelricky.dk", "Google Glass", "Sårplejerapport – " + new Date());

                    break;
                case R.id.confirm_cancel:
                    Log.i(TAG, "menu: Confirm: cancel and exit");
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
    private void finishReport(String email, String name, String subject) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent
                .putExtra("recipient_email", email)
                .putExtra("recipient_name", name)
                .putExtra("subject", subject);
        startActivityForResult(intent, FINISH_REPORT_REQUEST);
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
        }
        else if (requestCode == SCAN_PATIENT_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received QR: " + data.getStringExtra("result"));

            patient = data.getStringExtra("result");

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

                TextView text = (TextView) findViewById(R.id.receiverIdentifier);
                text.setText(receiver);
                text.invalidate();
            }
            else {
                Toast.makeText(getApplicationContext(), "Invalid receiver: " + s + ". Scan again.", Toast.LENGTH_LONG).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
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
            textCountView = (TextView) findViewById(R.id.beforeImageNumber);
            textCountView.setText(String.valueOf(imagePaths.size()));

            textLabelView = (TextView) findViewById(R.id.beforeImageLabel);
        }
        else if (step == 1) {
            textCountView = (TextView) findViewById(R.id.videoNumber);
            textCountView.setText(String.valueOf(videoPaths.size()));

            textLabelView = (TextView) findViewById(R.id.videoLabel);
        }

        if (textCountView != null && textLabelView != null) {
            textCountView.setTextColor(Color.WHITE);
            textLabelView.setTextColor(Color.WHITE);

            textLabelView.invalidate();
            textCountView.invalidate();
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
}
