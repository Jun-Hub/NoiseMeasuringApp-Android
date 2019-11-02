package noisy.desibel;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends AppCompatActivity {

    private AudioReader audioReader;
    int sampleRate = 8000;
    int inputBlockSize = 256;
    int sampleDecimate = 1;
    private TextView desibelText, placeText, noisyText, statusText;
    private EditText editText;
    private int db;
    InputMethodManager imm;

    String inTime, updatingPlace;

    private DesibelThread desibelThread;
    private UpdateThread updateThread;
    private ListUpdateThread listUpdateThread;

    RecyclerAdapter recyclerAdapter;

    private static String TAG = "phptest_MainActivity";

    private static final String TAG_JSON = "decibel";
    private static final String TAG_PLACE = "place";
    private static final String TAG_NOISY = "noisy";
    private static final String TAG_TIME = "time";

    String mJsonString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/pencil_sketch.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.main_activity);

        checkPermission();
        NetworkUtil.setNetworkPolicy();

        Button startBtn = (Button) findViewById(R.id.startBtn);
        Button stopBtn = (Button) findViewById(R.id.stopBtn);
        desibelText = (TextView) findViewById(R.id.textview);
        placeText = (TextView) findViewById(R.id.textview3);
        noisyText = (TextView) findViewById(R.id.textview4);
        editText = (EditText) findViewById(R.id.placeEdittext);
        statusText = (TextView) findViewById(R.id.statusText);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        recyclerAdapter = new RecyclerAdapter(this, R.layout.main_activity);

        recyclerView.setAdapter(recyclerAdapter);

        statusText.setVisibility(View.INVISIBLE);

        audioReader = new AudioReader();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (editText.getText().toString().equals("")) {
                    Toast toast = Toast.makeText(MainActivity.this, "장소를 입력해주세요", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.setView(getLayoutInflater().inflate(R.layout.toast_view, null));
                    toast.show();
                } else if (statusText.getText().toString().equals("에서 소음 측정중입니다...")) {
                    Toast toast = Toast.makeText(MainActivity.this, "장소를 입력해주세요", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.setView(getLayoutInflater().inflate(R.layout.toast_view2, null));
                    toast.show();
                } else {
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    for (int i = 0; i < recyclerAdapter.getItemCount(); i++) {
                        if (editText.getText().toString().equals(recyclerAdapter.getPlace(i))) {
                            Toast toast = Toast.makeText(MainActivity.this, "장소를 입력해주세요", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.setView(getLayoutInflater().inflate(R.layout.toast_view3, null));
                            toast.show();

                            return;
                        }
                    }

                    doStart();
                    desibelThread = new DesibelThread();
                    desibelThread.start();
                    statusText.setVisibility(View.VISIBLE);
                    statusText.setText("에서 소음 측정중입니다...");

                    noisyInsert();

                    recyclerAdapter.add(editText.getText().toString(), noisyText.getText().toString(), inTime);
                    recyclerAdapter.notifyDataSetChanged();

                    updatingPlace = editText.getText().toString();

                    updateThread = new UpdateThread();
                    updateThread.start();
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doStop();
            }
        });

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                }
                return false;
            }
        });
    }

    public void doStart() {
        audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener() {
            @Override
            public final void onReadComplete(int dB) {
                if (dB >= 0)
                    db = dB;
                //receiveDecibel(dB);
            }

            @Override
            public void onReadError(int error) {

            }
        });
    }

    private void receiveDecibel(final int dB) {
        Log.e("###", dB + " dB");
    }

    public void doStop() {
        audioReader.stopReader();

        if (!(desibelThread == null)) {
            desibelThread.interrupt();
        }
        if (!(updateThread == null)) {
            updateThread.interrupt();
        }

        updatingPlace = null;

        statusText.setText("null");
        statusText.setVisibility(View.INVISIBLE);
        desibelText.setText("--");
        editText.setText(null);
    }

    public void noisyInsert() {
        inTime = new java.text.SimpleDateFormat("MM/dd HH:mm:ss").format(new java.util.Date());
        Log.e("555TIme", "" + inTime);

        try {
            PHPRequest request = new PHPRequest("http://jun3028.cafe24.com/user_signup/signup_user_information.php");
            String result = request.PhPtest(String.valueOf(editText.getText()), String.valueOf(noisyText.getText()),
                    inTime);
            if (result.equals("1 record added")) {
                Log.e("555TIme", "해당 정보가 DB에 등록되었습니다" + inTime);
            } else {
                Log.e("오류", "오류" + inTime);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void noisyUpdate() {
        inTime = new java.text.SimpleDateFormat("MM/dd HH:mm:ss").format(new java.util.Date());
        Log.e("999TIme", "" + inTime);

        try {
            PHPRequest request = new PHPRequest("http://jun3028.cafe24.com/user_signup/update_info.php");
            String result = request.PhPtest2(String.valueOf(noisyText.getText()), inTime, updatingPlace);
            if (result.equals("1 record added")) {
                Log.e("999TIme", "해당 정보가 DB에 등록되었습니다" + inTime);
            } else {
                Log.e("오류", "오류" + inTime);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void listUpdate() {
        GetData task = new GetData();
        task.execute("http://jun3028.cafe24.com/user_signup/signup_user_information2.php");
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    desibelText.setText(msg.obj.toString());
                    break;

                case 1:
                    String dBStr = msg.obj.toString();
                    int dB = Integer.parseInt(dBStr);

                    if (dB < 30) {
                        placeText.setText("나뭇잎 스치는 소리");
                    } else if (dB >= 30 && dB < 50) {
                        placeText.setText("조용한 도서관");
                    } else if (dB >= 50 && dB < 70) {
                        placeText.setText("대화 소리");
                    } else if (dB >= 70) {
                        placeText.setText("시끄러운 음악");
                    }
                    break;

                case 2:
                    String dBStr2 = msg.obj.toString();
                    int dB2 = Integer.parseInt(dBStr2);

                    if (dB2 < 50) {
                        noisyText.setText("(사람없음)");
                    } else if (dB2 >= 50 && dB2 < 80) {
                        noisyText.setText("(보통)");
                    } else if (dB2 >= 80) {
                        noisyText.setText("(붐빔)");
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private class DesibelThread extends Thread {


        private DesibelThread() {
        }

        public void run() {

            while (true) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 메시지 얻어오기
                Message message = mHandler.obtainMessage();
                // 메시지 ID 설정
                message.what = 0;
                // 메시지 내용 설정 (Object)
                message.obj = db;
                // 메시지 전달
                mHandler.sendMessage(message);

                // 메시지 얻어오기
                Message message2 = mHandler.obtainMessage();
                // 메시지 ID 설정
                message2.what = 1;
                // 메시지 내용 설정 (Object)
                message2.obj = db;
                // 메시지 전달
                mHandler.sendMessage(message2);

                // 메시지 얻어오기
                Message message3 = mHandler.obtainMessage();
                // 메시지 ID 설정
                message3.what = 2;
                // 메시지 내용 설정 (Object)
                message3.obj = db;
                // 메시지 전달
                mHandler.sendMessage(message3);
            }
        }
    }

    private class UpdateThread extends Thread {


        private UpdateThread() {
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                noisyUpdate();
            }
        }
    }

    private class ListUpdateThread extends Thread {


        private ListUpdateThread() {
        }

        public void run() {
            while (true) {
                listUpdate();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    @Override   //폰트 설정
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();

        listUpdateThread = new ListUpdateThread();
        listUpdateThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        doStop();

        if(!(listUpdateThread==null)) {
            listUpdateThread.interrupt();
        }
    }

    /**
     * 퍼미션 체크
     */
    private void checkPermission() {

            /* 사용자의 OS 버전이 마시멜로우 이상인지 체크한다. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    /* 사용자 단말기의 권한 중 "위치사용" 권한이 허용되어 있는지 체크한다.
                    *  int를 쓴 이유? 안드로이드는 C기반이기 때문에, Boolean 이 잘 안쓰인다.
                    */
            int permissionResult = checkSelfPermission(Manifest.permission.RECORD_AUDIO);

                    /* ACCESS_FINE_LOCATION의 권한이 없을 때 */
            // 패키지는 안드로이드 어플리케이션의 아이디다.( 어플리케이션 구분자 )
            if (permissionResult == PackageManager.PERMISSION_DENIED) {

                        /* 사용자가 CALL_PHONE 권한을 한번이라도 거부한 적이 있는 지 조사한다.
                        * 거부한 이력이 한번이라도 있다면, true를 리턴한다.
                        */
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한이 필요합니다.")
                            .setMessage("이 기능을 사용하기 위해서는 단말기의 \"위치정보\" 권한이 필요합니다. 계속하시겠습니까?")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1000);

                                    }

                                }
                            })
                            .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(MainActivity.this, "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .create()
                            .show();
                }

                //최초로 권한을 요청할 때
                else {
                    // ACCESS_FINE_LOCATION 권한을 Android OS 에 요청한다.
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
                }

            }
                    /* ACCESS_FINE_LOCATION의 권한이 있을 때 */
            else {

            }

        }
                /* 사용자의 OS 버전이 마시멜로우 이하일 떄 */
        else {

        }
    }

    /**
     * 사용자가 권한을 허용했는지 거부했는지 체크
     *
     * @param requestCode  1000번
     * @param permissions  개발자가 요청한 권한들
     * @param grantResults 권한에 대한 응답들
     *                     permissions와 grantResults는 인덱스 별로 매칭된다.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000) {

            /* 요청한 권한을 사용자가 "허용"했다면 인텐트를 띄워라
                내가 요청한 게 하나밖에 없기 때문에. 원래 같으면 for문을 돈다.*/
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                }
            } else {
                Toast.makeText(MainActivity.this, "앱 설정에서 권한을 허용해야 위치정보 사용이 가능합니다.", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private class GetData extends AsyncTask<String, Void, String> {

        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.e(TAG, "response  - " + result);

            if (result == null) {

            } else {

                mJsonString = result;
                showResult();
            }
        }


        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(10000);
                httpURLConnection.connect();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.e(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();

            } catch (Exception e) {

                Log.e(TAG, "InsertData: Error ", e);
                errorString = e.toString();

                return null;
            }

        }
    }

    private void showResult() {
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            recyclerAdapter.removeAll();

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                String place = item.getString(TAG_PLACE);
                String noisy = item.getString(TAG_NOISY);
                String time = item.getString(TAG_TIME);

                Log.e("TAG_JSON", ": " + place + noisy + time);

                recyclerAdapter.add(place, noisy, time);
            }

            recyclerAdapter.notifyDataSetChanged();
        } catch (JSONException e) {

            Log.e(TAG, "showResult : ", e);
        }
    }
}
