package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class ReportActivity extends Activity implements MediaHandlerListener {
    private static final String TAG = "ReportActivity";
    private static final String LINEEND = "\r\n";
    private static final String TWOHYPHENS = "--";
    private static final String BOUNDARY = "********";

    private TextView progress;
    private File[] mediaFiles;
    private String senderAddress;
    private String proxyUrl;
    private ArrayList<String> mediaFilePaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_report);

        Intent intent = getIntent();
        mediaFilePaths = intent.getStringArrayListExtra("media_files");

        // Read properties from assets/config.properties
        try {
            Properties properties = new Properties();
            AssetManager assetManager = getApplicationContext().getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
            senderAddress = properties.getProperty("SenderAddress");
            proxyUrl = properties.getProperty("ProxyUrl");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        progress = (TextView) findViewById(R.id.text_upload_progress);

        Log.i(TAG, "onCreate done.");

        finishReport();
    }

    class MediaHandler extends AsyncTask<Object, Object, Boolean> {
        private MediaHandlerListener listener;

        public MediaHandler(MediaHandlerListener listener) {
            this.listener = listener;
        }

        private DataOutputStream out;
        private BufferedReader in;

        private Boolean streamFiles(String serviceUrl, File[] files, Bundle extras) {
            try {
                URL url = new URL(serviceUrl);
                Socket socket = new Socket(url.getHost(), url.getPort());
                out = new DataOutputStream(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String recipientAddress = extras.getString("recipient_email");
                String subject = extras.getString("subject");
                String text = extras.getString("text");

                sendCommand("MAIL FROM:" + "<" + senderAddress + ">", 250);
                sendCommand("RCPT TO:" + "<" + recipientAddress + ">", 250);
                sendCommand("DATA", 354);

                sendLine("From: <" + senderAddress + ">");
                sendLine("To:   <" + recipientAddress + ">");
                sendLine("Subject: " + subject);

                sendLine("Content-Type: multipart/mixed;");
                sendLine("        boundary=" + BOUNDARY);
                sendLine("Content-Transfer-Encoding: 8bit");

                sendLine();
                sendLine("This is a multi-part message in MIME format.");
                sendLine();
                sendLine(TWOHYPHENS + BOUNDARY);
                sendLine("Content-Type: text/plain; charset=\"utf-8\"");
                sendLine("Content-Transfer-Encoding: 8bit");
                sendLine();
                sendLine(text);
                sendLine();
                sendLine("#files: " + files.length);
                sendLine();
                sendLine(TWOHYPHENS + BOUNDARY);

                writeFiles(out, files);

                sendLine();
                sendCommand(".", 250);
                sendCommand("QUIT", 221);

                socket.close();
                this.listener.onResult(true);
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "Hmm ...", t);
            }

            this.listener.onResult(false);
            return false;
        }

        private void sendCommand(String command, int expect) throws Exception {
            out.writeBytes(command + LINEEND);
            String response = readLine();
            if (!response.startsWith(expect + " ")) {
                throw new Exception("Expected: " + expect + "; got: " + response);
            }
        }

        private void sendLine(String line) throws Exception {
            System.err.println(line);
            out.writeBytes(line + LINEEND);
        }

        private void sendLine() throws Exception {
            sendLine("");
        }

        private String readLine() throws Exception {
            return in.readLine();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            String url = (String) params[0];
            File[] files = (File[]) params[1];
            Bundle extras = (Bundle) params[2];
            return streamFiles(url, files, extras);
        }

        private void writeFiles(DataOutputStream out, File[] files) throws Throwable {
            int totalBytes = 0;
            int bytesReadTotal = 0;
            double percent = 0.0;

            for (File file : files) {
                totalBytes += file.length();
            }

            for (File file : files) {

                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

                out.writeBytes("Content-Type: application/octet-stream; name=\"" + file.getName() + "\"" + LINEEND);
                out.writeBytes("Content-Transfer-Encoding: base64" + LINEEND);
                out.writeBytes("Content-Disposition: attachment; filename=" + file.getName() + LINEEND);
                out.writeBytes(LINEEND);

                int bufferSize = 3 * 1024; // http://stackoverflow.com/a/7920834
                byte[] buffer = new byte[bufferSize];
                int bytesRead = fileInputStream.read(buffer);
                bytesReadTotal += bytesRead;
                while (bytesRead > 0) {
                    out.write(Base64.encode(buffer, Base64.DEFAULT));
                    bytesRead = fileInputStream.read(buffer);
                    bytesReadTotal += bytesRead;

                    percent = ((double) bytesReadTotal / totalBytes) * 100;
                    updateProgress(percent);
                }

                out.writeBytes(LINEEND);
                out.writeBytes(LINEEND);
                out.writeBytes(TWOHYPHENS + BOUNDARY + LINEEND);
                fileInputStream.close();
            }
        }
    }


    private void updateProgress(final double percent) {
        final String p = String.valueOf((int) Math.round(percent));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.setText(p + "%");
            }
        });
    }

    public void finishReport() {
        if (mediaFilePaths != null) {
            int i = 0;
            mediaFiles = new File[mediaFilePaths.size()];
            for (String path : mediaFilePaths) {
                mediaFiles[i] = new File(path);
                i++;
            }
        } else {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), MainActivity.FILE_DIRECTORY);

            mediaFiles = mediaStorageDir.listFiles();
        }


        for (File file : mediaFiles) {
            Log.i(TAG, String.format("%s: %d", file.getName(), file.length()));
        }

        new MediaHandler(this).execute(proxyUrl, mediaFiles, getIntent().getExtras());
    }

    @Override
    public void onResult(boolean success) {
        Log.i(TAG, "onResult: " + success);
        if (success) {
            // Add path to file as result
            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);

            finish();
        } else {
            finish();
        }
    }
}

// http://stackoverflow.com/a/9963705
interface MediaHandlerListener {
    void onResult(boolean result);
}


