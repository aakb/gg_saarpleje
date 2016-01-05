package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class QRActivity extends Activity {
    private static final String TAG = "QRActivity";

    private Camera camera;
    private CameraPreview cameraPreview;
    private TextView countdownText;

    private Timer timer;
    private int timerExecutions = 0;

    private MultiFormatReader mMultiFormatReader;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMultiFormatReader = new MultiFormatReader();

        Log.i(TAG, "Launching activity");

        setContentView(R.layout.activity_camera);

        countdownText = (TextView) findViewById(R.id.text_camera_countdown);

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
        }

        // Create an instance of Camera
        camera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

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
                        countdownText.setText("" + (3 - timerExecutions));
                    }
                });

                if (timerExecutions >= 3) {
                    Log.i(TAG, "timer cancel, take picture");
                    cancel();
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

    /**
     * Picture callback.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, size.width, size.height, 0, 0,
                    size.width, size.height, false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result;

            releaseCamera();

            try {
                result = mMultiFormatReader.decode(bitmap, null);
                if (result != null) {
                    Log.i(TAG, result.getText());
                }

                // Add path to file as result
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", result.getText());
                setResult(RESULT_OK, returnIntent);

                // Finish activity
                finish();
            } catch (NotFoundException e) {
                Log.e(TAG, e.toString());

                // Finish activity
                finish();
            }
        }
    };

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
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
}
