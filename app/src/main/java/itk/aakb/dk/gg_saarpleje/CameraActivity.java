package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private static final String TAG = "CameraActivity";
    private Timer timer;
    private int timerExecutions = 0;
    private TextView countdownText;
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

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Reset timer executions.
        timerExecutions = 0;

        countdownText.setText("3");

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerExecutions++;

                Log.i(TAG, "" + timerExecutions);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCountdownText("" + (3 - timerExecutions));
                    }
                });

                if (timerExecutions >= 3) {
                    Log.i(TAG, "timer cancel, take picture");
                    cancel();
                    takePicture();
                }
            }
        }, 2000, 1000);
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
     * Trigger take picture on camera.
     */
    public void takePicture() {
        Log.i(TAG, "take picture");
        // Take picture
        mCamera.takePicture(null, null, mPicture);
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

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            releaseCamera();

            File pictureFile = getOutputImageFile();
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                // Add path to file as result
                Intent returnIntent = new Intent();
                returnIntent.putExtra("path", pictureFile.getAbsolutePath());
                setResult(RESULT_OK, returnIntent);

                // Finish activity
                finish();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
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
     * Create a File for saving an image
     */
    private static File getOutputImageFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FILE_DIRECTORY);
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
                    "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }
}