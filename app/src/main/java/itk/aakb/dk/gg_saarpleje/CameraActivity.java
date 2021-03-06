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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends Activity {
    private static final String TAG = "CameraActivity";

    private Camera camera;
    private CameraPreview cameraPreview;
    private TextView countdownText;
    private Timer timer;
    private int timerExecutions = 0;
    private String filePrefix;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching activity");

        // Get file prefix
        Intent intent = getIntent();
        filePrefix = intent.getStringExtra("FILE_PREFIX");

        setContentView(R.layout.activity_camera_countdown);

        countdownText = (TextView) findViewById(R.id.text_camera_duration);

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
        }

        // Create an instance of Camera
        camera = getCameraInstance();

        if (camera == null) {
            Toast.makeText(this, "Camera cannot be locked", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Reset timer executions.
        timerExecutions = 0;

        countdownText.setText("3");

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerExecutions++;

                Log.i(TAG, "" + timerExecutions);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countdownText.setText("" + (3 - timerExecutions));
                    }
                });

                if (timerExecutions >= 3) {
                    Log.i(TAG, "timer cancel, take picture");
                    releaseTimer();
                    // Take picture
                    camera.takePicture(null, null, mPicture);
                }
            }
        }, 2000, 1000);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Log.i(TAG, "getting camera instance...");
        try {
            return Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.e(TAG, "could not getCameraInstance");
            throw e;
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * Picture callback.
     */
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

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        releaseTimer();
        releaseCamera();

        super.onPause();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        releaseTimer();
        releaseCamera();

        super.onDestroy();
    }

    /**
     * Release the camera resources.
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            cameraPreview.release();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    private void releaseTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Create a File for saving an image
     */
    private File getOutputImageFile() {

        String timeStampFolderPrefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String folderPrefix = filePrefix + "_" + timeStampFolderPrefix;

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + MainActivity.FILE_DIRECTORY, folderPrefix);

        Log.i(TAG, mediaStorageDir.getAbsolutePath());

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                filePrefix + "_" + timeStamp + ".jpg");
        return mediaFile;
    }
}