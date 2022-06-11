package org.example.registerlogin;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity{
    // 버킷 사용
    private Uri BfilePath;

    //wav 변환
    ExtAudioRecorder recorder;

    //녹음기능 media record
    public   MediaRecorder mRecorder = null;
    public int recordCount = 0;
    TextView test_tv;
    private BluetoothSPP bt;
    private long time = 0;

    NotificationManager notificationManager;
    PendingIntent intent;
    Button test;
    private final int PERMISSIONS_REQUEST_RESULT = 100;
    private static final int REQUEST_ENABLE_BT = 10;

    // FTP통신을 위한 변수 선언
    private ConnectFTP connectFTP;
    private final String TAGf = "0v0_FTP";
    boolean status, threadStop = false;
    private String FTPip = "172.30.1.57";

    private static final String TAG = "RecordThread";
    private static final int startAmpl = 33000;
    private final int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private final int outputFormat = MediaRecorder.OutputFormat.THREE_GPP;
    private final int audioEncoder = MediaRecorder.AudioEncoder.AMR_NB;

    // 데시벨 기능 media record
    private MediaRecorder mediaRecorder = null;
    private double decibel = 0;
    private boolean isRunning = false;
    private Thread recordThread;
    private static final double AMP_CONST = 1.9; // 기기간 데시벨 조절을 위한 보정값, 기본값 : 1.9, 공식값 : 2700
    private static final double EMA_FILTER = 0.6; // EMA 필터 계산에 사용되는 상수, 기본값 0.6
    private static double mEMA = 0.0;
    private NotificationManager notifManager;

    // socket통신을 위한 변수 선언
    private ConnectSocket connectSocket;

    //소켓을 받으면 1, 없으면 0이 저장됨
    int line2;
    //wav파일이 저장된 폴더의 경로
    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SoundSense/";

    int currnetFileNum = 0;
    File file;
    File[] files;
    List<String> fileNameList;
    Thread ftpThread;

    public List<String> fileNames() {
        file = new File(path);
        files = file.listFiles();
        fileNameList = new ArrayList<>();

        for (int k = 0; k < files.length; k++) {
            fileNameList.add(files[k].getName());
//            Log.i("파일 이름",files[k].getName());
        }
        return fileNameList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionCheck();
        start();

        connectFTP = new ConnectFTP();
        connectSocket = new ConnectSocket();

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 버전과 같거나 이상이라면
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "외부 저장소 사용을 위해 읽기/쓰기 필요", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        2);  //마지막 인자는 체크해야될 권한 갯수

            } else {
                //Toast.makeText(this, "권한 승인되었음", Toast.LENGTH_SHORT).show();
            }
        }*/
    }

//        public void onStart() {
//        super.onStart();
//
//            ftpThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while (!threadStop){
//                        try {
//                                //파일 갯수가 달라지면 파일 업로드 실행
//                            if(currnetFileNum != fileNames().size()) {
//                                Log.i("파일 리스트","변화 감지");
//
//                                currnetFileNum = fileNames().size();
//                                for (int i = 0; i < fileNames().size(); i++) {
//                                    //버킷 업로드
//                                    uploadFile(fileNames().get(i));
//                                    Log.d("Upload File", fileNames().get(i) + " 업로드");
//                                }
//
//                                // 소켓 송신 & 수신
////                              connectSocket.socket_connect();
//                                }
//                            else {
//                                line2 = 0;
//                                Log.d(TAGf,"업로드할 파일 없음");
//                            }
//                        }
//
//                            catch (Exception e) {
//                                Log.d(TAGf, "업로드 실패");
//                            }
//
//                            try {
//                                Thread.sleep(2000);
//                            } catch (InterruptedException e) {
//                                Log.d(TAGf, "Thread dead");
//                            }
//                        }
//
//                    }
//            });
//            ftpThread.start();
//    }

    public void permissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            /**
             * Override function
             * Function to execute when the thread is started
             */
            recordThread = new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    while (isRunning) {
                        try {
                            //Sleeps to slow down the reading of the amplitude, such that the GUI can keep up with the readings.
                            Thread.sleep(500);

                            //decibel = getAmplitude() / 2000;
                            decibel = 20 * Math.log10(getAmplitudeEMA() / AMP_CONST);
                            Log.d(TAG, "decibel" + decibel);
                            if (decibel >= 60) {
                                recordAudio();
                                try {
                                    Thread.sleep(20000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                //파일을 저장한 폴더로 이동
                                try {
                                    Path filePath = Paths.get(Environment.getExternalStorageDirectory()
                                            .getAbsolutePath() + "/myrecording_"+ recordCount +".wav");
                                    Path filePathToMove = Paths.get(Environment.getExternalStorageDirectory()
                                            .getAbsolutePath() + "/SoundSense/myrecording_"+ recordCount +".wav");
                                    Files.move(filePath, filePathToMove);
                                }
                                catch (IOException e) {
                                    Log.d("MOVE record","옮길 오디오가 없음");
                                }
                                recordCount++;
                            }


                        } catch (InterruptedException e) {
                            Log.e(TAG, "Thread interrupted.", e.fillInStackTrace());
                        }

                    }
                }
            });
            if (mediaRecorder == null) {
                mediaRecorder = new MediaRecorder();
            }
            try {
                mediaRecorder.setAudioSource(audioSource);
                mediaRecorder.setOutputFormat(outputFormat);
                mediaRecorder.setAudioEncoder(audioEncoder);
                mediaRecorder.setOutputFile("/dev/null");
            }
            catch (IllegalStateException ex) {
                Log.e(TAG, "The order in the media recorder is not correct!", ex.fillInStackTrace());
            }
            recordThread.start();
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException ex) {
                Log.e(TAG, "Could not prepare or start the mediaRecorder", ex.fillInStackTrace());
            }
        }
    }

    @Override
    protected void onDestroy() {
        threadStop = true;
        ftpThread.interrupt();
        finish();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - time >= 2000) {
            time = System.currentTimeMillis();
            Toast.makeText(getApplicationContext(), "Press the back button again to exit...", Toast.LENGTH_SHORT).show();
        } else if (System.currentTimeMillis() - time < 2000) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            Toast.makeText(MainActivity.this, "App Start In Background...", Toast.LENGTH_SHORT).show();
        }
    }

    private void recordAudio() {
        mRecorder = new MediaRecorder();
        recorder = ExtAudioRecorder.getInstanse(false); //Uncompressed recording
//        try{
//            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 어디에서 음성 데이터를 받을 것인지
//            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 압축 형식 설정
//            mRecorder.setAudioSamplingRate(44100);
//            mRecorder.setAudioEncodingBitRate(96000);
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//            //mediaRecorder.setMaxDuration(10000);
//            mRecorder.setOutputFile(Environment.getExternalStorageDirectory()
//                    .getAbsolutePath() + "/myrecording_"+ recordCount+".wav");
//        } catch (Exception e){
//            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 어디에서 음성 데이터를 받을 것인지
//            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // 압축 형식 설정
//            mRecorder.setAudioSamplingRate(44100);
//            mRecorder.setAudioEncodingBitRate(96000);
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            //mediaRecorder.setMaxDuration(10000);
//            mRecorder.setOutputFile(Environment.getExternalStorageDirectory()
//                    .getAbsolutePath() + "/myrecording_"+ recordCount+".wav");
//        }
        try {
            //mediaRecorder.setMaxDuration(10000);
            recorder.setOutputFile(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/myrecording_"+ recordCount+".wav");
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

       new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recorder.stop();
                        recorder.reset();
                        recorder = null;
                        //Looper.loop();
                    }
                },20000);
            }
        }).start();

//        try {
//            //mediaRecorder.setMaxDuration(10000);
//            recorder.setOutputFile(Environment.getExternalStorageDirectory()
//                    .getAbsolutePath() + "/myrecording_"+ recordCount+".wav");
//            recorder.prepare();
//            recorder.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
    public void stopRecording(){
        if(recorder != null){
            try{
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private double getAmplitude() {
        if (mediaRecorder != null)
            return (mediaRecorder.getMaxAmplitude());
        else
            return 0;
    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }


    //upload the file
//    private void uploadFile(String filemp4) {
//
//        //storage
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//
//        //업로드하려는 파일 지정
//        String filename = filemp4;
//        BfilePath = Uri.fromFile(new File(path+filemp4));
//
//        //storage 주소와 폴더 파일명을 지정해 준다.
//        StorageReference storageRef = storage.getReferenceFromUrl("gs://e1i3-83897.appspot.com").child("/" + filename);
//        //올라가거라...
//        storageRef.putFile(BfilePath)
//                //성공시
//                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        Toast.makeText(getApplicationContext(), "업로드 완료!", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                //실패시
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(getApplicationContext(), "업로드 실패!", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                //진행중
//                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                        @SuppressWarnings("VisibleForTests") //이걸 넣어 줘야 아랫줄에 에러가 사라진다. 넌 누구냐?
//                                double progress = (100 * taskSnapshot.getBytesTransferred()) /  taskSnapshot.getTotalByteCount();
//                    }
//                });
//    }

}


