package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

        finishReport();
    }

    class MediaHandler extends AsyncTask<Object, Object, JSONObject> {
        private MediaHandlerListener listener;

        public MediaHandler(MediaHandlerListener listener) {
            this.listener = listener;
        }

        private Socket socket;
        private DataOutputStream out;
        private BufferedReader in;

        private JSONObject streamFiles(String serviceUrl, File[] files, Bundle extras) {
            JSONObject result = null;
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

                // cleanUp();

//                writeExtras(dos, extras);
//                dos.close();
                socket.close();
            } catch (Throwable t) {
                Log.e(TAG, "Hmm ...", t);
                String s = t.getMessage();
            }

            this.listener.onResult(result);
            return result;
        }

        private void sendCommand(String command, int expect) throws Exception {
            System.err.println(command);
            out.writeBytes(command + LINEEND);
            String response = readLine();
            if (!response.startsWith(expect + " ")) {
                throw new Exception("Expected: " + expect + "; got: " + response);
            }
            System.err.println(response);
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
        protected JSONObject doInBackground(Object... params) {
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

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
//            publishProgress();
        }

        private void writeExtras(DataOutputStream dos, Bundle extras) throws Exception {
            dos.writeBytes(TWOHYPHENS + BOUNDARY + LINEEND);
            dos.writeBytes(LINEEND);
            dos.writeBytes("Content-Type: text/plain;" + LINEEND);
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                dos.writeBytes(key + ": " + (value != null ? value.toString() : "") + LINEEND);
            }
            dos.writeBytes(LINEEND);
            dos.writeBytes(TWOHYPHENS + BOUNDARY + TWOHYPHENS + LINEEND);
        }

        private void writeFiles(DataOutputStream out, File[] files) throws Throwable {
            int maxBufferSize = 1024 * 1024;

            int count = 0;
            for (File file : files) {
                count++;

                onProgressUpdate("Writing file %d of %d", count, files.length);

                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

//                out.writeBytes(TWOHYPHENS + BOUNDARY + LINEEND);
                out.writeBytes("Content-Type: application/octet-stream; name=\"" + file.getName() + "\"" + LINEEND);
                out.writeBytes("Content-Transfer-Encoding: base64" + LINEEND);
                out.writeBytes("Content-Disposition: attachment; filename=" + file.getName() + LINEEND);
                out.writeBytes(LINEEND);

// @TODO Chunk files?
                 FileInputStream fileInputStreamReader = new FileInputStream(file);
                 byte[] buffer = new byte[(int)file.length()];
                 fileInputStreamReader.read(buffer);
                 out.write(Base64.encode(buffer, Base64.DEFAULT));

/*
                // create a buffer of maximum size
                int bytesAvailable = fileInputStream.available();
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[] buffer = new byte[bufferSize];

                // read file and write it into form...
                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    out.write(Base64.encode(buffer, Base64.DEFAULT), 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    // break; // We may run out of memory ...
                }

  */
                out.writeBytes(LINEEND);
                out.writeBytes(LINEEND);
                out.writeBytes(TWOHYPHENS + BOUNDARY + LINEEND);
                fileInputStream.close();
            }
        }

        private JSONObject getReponse(HttpURLConnection connection) {
            try {
                InputStream responseStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                Log.i(TAG, sb.toString());


                return new JSONObject(sb.toString());
            } catch (IOException ex) {
            } catch (JSONException ex) {
            }

            return null;
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
    public void onResult(JSONObject result) {
        try {
            int code = result.getInt("code");
            if (code == 200) {
                cleanUp();
            }
        } catch (Exception ex) {
            Log.e(TAG, "onResult", ex);
        }
    }
}

// http://stackoverflow.com/a/9963705
interface MediaHandlerListener {
    void onResult(JSONObject result);
}
