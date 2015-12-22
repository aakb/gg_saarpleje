package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class VideoActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private static final String TAG = "VideoActivity";
    private Timer timer;
    private int timerExecutions = 0;
    private TextView countdownText;
    private String outputPath;
    private static final String FILE_DIRECTORY = "saarpleje";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching activity");

        setContentView(R.layout.activity_camera);

        countdownText = (TextView) findViewById(R.id.text_camera_countdown);

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
        }

        Log.i(TAG, "get camera instance");
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Reset timer executions.
        timerExecutions = 0;

        launchAutoVideo();
    }

    /**
     * Start process to take 10 s video.
     */
    private void launchAutoVideo() {
        countdownText.setText("10");

        // Catch all errors, and release camera on error.
        try {
            Log.i(TAG, "start preparing video recording");

            Log.i(TAG, "Setting camera hint");
            Camera.Parameters params = mCamera.getParameters();
            params.setRecordingHint(true);
            mCamera.setParameters(params);

            Log.i(TAG, "new media recorder");
            mMediaRecorder = new MediaRecorder();

            Log.i(TAG, "setting up error listener");
            mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                public void onError(MediaRecorder mediarecorder1, int k, int i1) {
                    Log.e(TAG, String.format("Media Recorder error: k=%d, i1=%d", k, i1));
                }

            });

            // Step 1: Unlock and set camera to MediaRecorder. Clear preview.
            Log.i(TAG, "unlock and set camera to MediaRecorder");
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            // Step 2: Set sources
            Log.i(TAG, "set sources");
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            Log.i(TAG, "set camcorder profile");
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

            // Step 4: Set output file
            Log.i(TAG, "set output file");
            outputPath = getOutputVideoFile().toString();
            mMediaRecorder.setOutputFile(outputPath);

            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Step 5: Set the preview output
                        Log.i(TAG, "set preview");
                        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

                        Log.i(TAG, "finished configuration.");

                        // Step 6: Prepare configured MediaRecorder
                        mMediaRecorder.prepare();
                    }
                    catch (IOException e) {
                        Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
                        releaseMediaRecorder();
                        releaseCamera();
                        finish();
                    }

                    Log.i(TAG, "prepare successful");

                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mMediaRecorder.start();

                    Log.i(TAG, "is recording");

                    // Count down from 3 seconds, then take picture.
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            timerExecutions++;

                            Log.i(TAG, "" + timerExecutions);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setCountdownText("" + (10 - timerExecutions));
                                }
                            });

                            if (timerExecutions >= 10) {
                                cancel();

                                mMediaRecorder.stop();  // stop the recording
                                releaseMediaRecorder(); // release the MediaRecorder object
                                releaseCamera();

                                // Add path to file as result
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("path", outputPath);
                                setResult(RESULT_OK, returnIntent);

                                // Finish activity
                                finish();
                            }
                        }
                    }, 1000, 1000);
                }
            }, 1000);
        }
        catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder (" + e.getCause() + "): " + e.getMessage());
            releaseMediaRecorder();
            releaseCamera();
            finish();
        }
        catch (Exception e) {
            Log.d(TAG, "Exception preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            releaseCamera();
            finish();
        }
    }

    /**
     * Update the countdown text
     *
     * @param s the text
     */
    public void setCountdownText(String s) {
        countdownText.setText(s);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;

        Log.i(TAG, "getting camera instance...");
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.e(TAG, "could not getCameraInstance");
            throw e;
            // Camera is not available (in use or does not exist)
            // @TODO: Throw Toast!
        }

        return c; // returns null if camera is unavailable
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mPreview.release();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputVideoFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), FILE_DIRECTORY);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        return mediaFile;
    }
}