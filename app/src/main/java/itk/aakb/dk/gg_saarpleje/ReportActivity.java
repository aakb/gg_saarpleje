package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
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
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;

public class ReportActivity extends Activity implements MediaHandlerListener {
    private static final String TAG = "ReportActivity";
    private static final String FILE_DIRECTORY = "saarpleje";

    private static final String LINEEND = "\r\n";
    private static final String TWOHYPHENS = "--";
    private static final String BOUNDARY = "********";

    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        info = (TextView) findViewById(R.id.text_info);
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

                String senderAddress = "googleglass@mikkelricky.dk";
                String senderName = "Google Glass";
                String recipientAddress = extras.getString("recipient_email");
                String recipientName = extras.getString("recipient_name");
                String subject = extras.getString("subject");

                sendCommand("MAIL FROM:" + "<" + senderAddress + ">", 250);
                sendCommand("RCPT TO:" + "<" + recipientAddress + ">", 250);
                sendCommand("DATA", 354);

                sendLine("From: " + senderName + " <" + senderAddress + ">");
                sendLine("To: " + recipientName + " <" + recipientAddress + ">");
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

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);

            final Object[] args = values;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appendInfo((String) args[0], Arrays.copyOfRange(args, 1, args.length));
                }
            });

            Log.e(TAG, String.format((String) args[0], Arrays.copyOfRange(args, 1, args.length)));
        }

        private void writeFiles(DataOutputStream out, File[] files) throws Throwable {
            int count = 0;
            for (File file : files) {
                count++;

                onProgressUpdate("Writing file %d of %d", count, files.length);

                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

                out.writeBytes("Content-Type: application/octet-stream; name=\"" + file.getName() + "\"" + LINEEND);
                out.writeBytes("Content-Transfer-Encoding: base64" + LINEEND);
                out.writeBytes("Content-Disposition: attachment; filename=" + file.getName() + LINEEND);
                out.writeBytes(LINEEND);

                int bufferSize = 3 * 1024; // http://stackoverflow.com/a/7920834
                byte[] buffer = new byte[bufferSize];
                int bytesRead = fileInputStream.read(buffer);
                while (bytesRead > 0) {
                    out.write(Base64.encode(buffer, Base64.DEFAULT));
                    bytesRead = fileInputStream.read(buffer);
                }

                out.writeBytes(LINEEND);
                out.writeBytes(LINEEND);
                out.writeBytes(TWOHYPHENS + BOUNDARY + LINEEND);
                fileInputStream.close();
            }
        }
    }

    private void setInfo(final String format, final Object... args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                info.setText(String.format(format, args));
            }
        });
    }

    private void setInfo(int id) {
        setInfo(getString(id));
    }

    private void appendInfo(final String format, final Object... args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                info.append("\n" + String.format(format, args));
            }
        });
    }

    public void finishReport() {
        // http://developer.android.com/tools/devices/emulator.html#networkaddresses
        String proxyUrl = "http://10.0.2.2:10000";

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FILE_DIRECTORY);

        mediaFiles = mediaStorageDir.listFiles();

        if (mediaFiles.length == 0) {
            setInfo(R.string.no_media_files);
        } else {
            for (File file : mediaFiles) {
                Log.e(TAG, String.format("%s: %d", file.getName(), file.length()));
            }

            setInfo(getResources().getQuantityString(R.plurals.uploading_n_files, mediaFiles.length, mediaFiles.length));

            new MediaHandler(this).execute(proxyUrl, mediaFiles, getIntent().getExtras());
        }
    }

    private File[] mediaFiles;

    private void cleanUp() {
        // Remove all files.
        for (File file : mediaFiles) {
            file.delete();
        }
    }

    @Override
    public void onResult(boolean success) {
        Log.e(TAG, "onResult: " + success);
        if (success) {
            cleanUp();
        }
    }
}

// http://stackoverflow.com/a/9963705
interface MediaHandlerListener {
    void onResult(boolean result);
}


