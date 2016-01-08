package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class ReportActivity extends Activity {
    private static final String TAG = "ReportActivity";
    private static final String FILE_DIRECTORY = "saarpleje";

    private static final String POST_LINEEND = "\r\n";
    private static final String POST_TWOHYPHENS = "--";
    private static final String POST_BOUNDARY = "*****";

    private TextView info;

//    private String recipient_email;
//    private String recipient_name;
//    private String subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        info = (TextView) findViewById(R.id.text_info);

//        Intent intent = getIntent();
//        recipient_email = intent.getStringExtra("recipient_email");
//        recipient_name = intent.getStringExtra("recipient_name");
//        subject = intent.getStringExtra("subject");

        finishReport();
    }

    class MediaHandler extends AsyncTask<Object, Object, JSONObject> {
        @Override
        protected JSONObject doInBackground(Object... params) {
            try {
                // http://developer.android.com/tools/devices/emulator.html#emulatornetworking

                String reportServiceUrl = (String) params[0];
                File[] files = (File[]) params[1];
                Bundle extras = (Bundle) params[2];

// http://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83&aaid=106
                URL url = new URL(reportServiceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoInput(true); // Allow Inputs
                connection.setDoOutput(true); // Allow Outputs
                connection.setUseCaches(false); // Don't use a Cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + POST_BOUNDARY);

                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

                // Map<String, Object> data = new HashMap<>();
                // data.put("recipient_email", recipient_email);
                // data.put("recipient_name", recipient_name);
                // data.put("subject", subject);
                // writeData(dos, data);
                writeExtras(dos, extras);

                try {
                    writeFiles(dos, files);
                } catch (Throwable ex) {
                    Log.e(TAG, "Error in writeFiles", ex);
                }

                dos.flush();
                dos.close();

                publishProgress("Sending files to ...");

                // Responses from the server (code and message)
                int serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);

                JSONObject response = getReponse(connection);
                // Log.e(TAG, "reponse: " + response);
                if (serverResponseCode == 200) {
                    // Remove all files.
                    for (File file : files) {
                        file.delete();
                    }

                    publishProgress("Done.");
                    return response;
                }
                publishProgress("Something did not work ...");
            } catch (MalformedURLException ex) {
                Log.e(TAG, "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                Log.e(TAG, "Exception : " + e.getMessage(), e);
            }
            return null;
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

//        private void writeData(DataOutputStream dos, Map<String, Object> data) throws Exception {
//            for (Map.Entry<String, Object> entry : data.entrySet()) {
//                if (entry.getValue() != null) {
//                    dos.writeBytes(POST_TWOHYPHENS + POST_BOUNDARY + POST_LINEEND);
//                    dos.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + POST_LINEEND);
//                    dos.writeBytes(POST_LINEEND);
//                    dos.writeBytes(entry.getValue().toString());
//                    dos.writeBytes(POST_LINEEND);
//                    dos.writeBytes(POST_TWOHYPHENS + POST_BOUNDARY + POST_TWOHYPHENS + POST_LINEEND);
//                }
//            }
//        }

        private void writeExtras(DataOutputStream dos, Bundle extras) throws Exception {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                dos.writeBytes(POST_TWOHYPHENS + POST_BOUNDARY + POST_LINEEND);
                dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + POST_LINEEND);
                dos.writeBytes(POST_LINEEND);
                dos.writeBytes(value != null ? value.toString() : "");
                dos.writeBytes(POST_LINEEND);
                dos.writeBytes(POST_TWOHYPHENS + POST_BOUNDARY + POST_TWOHYPHENS + POST_LINEEND);
            }
        }

        private void writeFiles(DataOutputStream dos, File[] files) throws Throwable {
            int maxBufferSize = 1024 * 1024;

            int count = 0;
            for (File file : files) {
                count++;

                onProgressUpdate("Writing file %d of %d", count, files.length);

                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

                String formName = "file" + count;
                dos.writeBytes(POST_TWOHYPHENS + POST_BOUNDARY + POST_LINEEND);
                dos.writeBytes("Content-Disposition: form-data; name=\"" + formName + "\";filename=\"" + file.getName() + "\"" + POST_LINEEND);
                dos.writeBytes(POST_LINEEND);

                // create a buffer of maximum size
                int bytesAvailable = fileInputStream.available();
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[] buffer = new byte[bufferSize];

                // read file and write it into form...
                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    // break; // We may run out of memory ...
                }

                // send multipart form data necessary after file data...
                dos.writeBytes(POST_LINEEND);
                dos.writeBytes(POST_TWOHYPHENS + POST_BOUNDARY + POST_TWOHYPHENS + POST_LINEEND);
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
        String reportServiceUrl = "http://mikkelricky.dk/saarpleje/";

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FILE_DIRECTORY);

        File[] files = mediaStorageDir.listFiles();

        if (files.length == 0) {
            setInfo(R.string.no_media_files);
        } else {
            for (File file : files) {
                Log.e(TAG, String.format("%s: %d", file.getName(), file.length()));
            }

            setInfo(getResources().getQuantityString(R.plurals.uploading_n_files, files.length, files.length));

            new MediaHandler().execute(reportServiceUrl, files, getIntent().getExtras());
        }
    }
}
