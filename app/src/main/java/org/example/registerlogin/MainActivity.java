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
    // ?????? ??????
    private Uri BfilePath;

    //wav ??????
    ExtAudioRecorder recorder;

    //???????????? media record
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

    // FTP????????? ?????? ?????? ??????
    private ConnectFTP connectFTP;
    private final String TAGf = "0v0_FTP";
    boolean status, threadStop = false;
    private String FTPip = "172.30.1.57";

    private static final String TAG = "RecordThread";
    private static final int startAmpl = 33000;
    private final int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private final int outputFormat = MediaRecorder.OutputFormat.THREE_GPP;
    private final int audioEncoder = MediaRecorder.AudioEncoder.AMR_NB;

    // ????????? ?????? media record
    private MediaRecorder mediaRecorder = null;
    private double decibel = 0;
    private boolean isRunning = false;
    private Thread recordThread;
    private static final double AMP_CONST = 1.9; // ????????? ????????? ????????? ?????? ?????????, ????????? : 1.9, ????????? : 2700
    private static final double EMA_FILTER = 0.6; // EMA ?????? ????????? ???????????? ??????, ????????? 0.6
    private static double mEMA = 0.0;
    private NotificationManager notifManager;

    // socket????????? ?????? ?????? ??????
    private ConnectSocket connectSocket;

    //????????? ????????? 1, ????????? 0??? ?????????
    int line2;
    //wav????????? ????????? ????????? ??????
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
//            Log.i("?????? ??????",files[k].getName());
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // ??????????????? ????????? ????????? ???????????????
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "?????? ????????? ????????? ?????? ??????/?????? ??????", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        2);  //????????? ????????? ??????????????? ?????? ??????

            } else {
                //Toast.makeText(this, "?????? ???????????????", Toast.LENGTH_SHORT).show();
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
//                                //?????? ????????? ???????????? ?????? ????????? ??????
//                            if(currnetFileNum != fileNames().size()) {
//                                Log.i("?????? ?????????","?????? ??????");
//
//                                currnetFileNum = fileNames().size();
//                                for (int i = 0; i < fileNames().size(); i++) {
//                                    //?????? ?????????
//                                    uploadFile(fileNames().get(i));
//                                    Log.d("Upload File", fileNames().get(i) + " ?????????");
//                                }
//
//                                // ?????? ?????? & ??????
////                              connectSocket.socket_connect();
//                                }
//                            else {
//                                line2 = 0;
//                                Log.d(TAGf,"???????????? ?????? ??????");
//                            }
//                        }
//
//                            catch (Exception e) {
//                                Log.d(TAGf, "????????? ??????");
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

                                //????????? ????????? ????????? ??????
                                try {
                                    Path filePath = Paths.get(Environment.getExternalStorageDirectory()
                                            .getAbsolutePath() + "/myrecording_"+ recordCount +".wav");
                                    Path filePathToMove = Paths.get(Environment.getExternalStorageDirectory()
                                            .getAbsolutePath() + "/SoundSense/myrecording_"+ recordCount +".wav");
                                    Files.move(filePath, filePathToMove);
                                }
                                catch (IOException e) {
                                    Log.d("MOVE record","?????? ???????????? ??????");
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
//            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // ???????????? ?????? ???????????? ?????? ?????????
//            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // ?????? ?????? ??????
//            mRecorder.setAudioSamplingRate(44100);
//            mRecorder.setAudioEncodingBitRate(96000);
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//            //mediaRecorder.setMaxDuration(10000);
//            mRecorder.setOutputFile(Environment.getExternalStorageDirectory()
//                    .getAbsolutePath() + "/myrecording_"+ recordCount+".wav");
//        } catch (Exception e){
//            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // ???????????? ?????? ???????????? ?????? ?????????
//            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // ?????? ?????? ??????
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
//        //?????????????????? ?????? ??????
//        String filename = filemp4;
//        BfilePath = Uri.fromFile(new File(path+filemp4));
//
//        //storage ????????? ?????? ???????????? ????????? ??????.
//        StorageReference storageRef = storage.getReferenceFromUrl("gs://e1i3-83897.appspot.com").child("/" + filename);
//        //???????????????...
//        storageRef.putFile(BfilePath)
//                //?????????
//                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        Toast.makeText(getApplicationContext(), "????????? ??????!", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                //?????????
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(getApplicationContext(), "????????? ??????!", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                //?????????
//                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                        @SuppressWarnings("VisibleForTests") //?????? ?????? ?????? ???????????? ????????? ????????????. ??? ??????????
//                                double progress = (100 * taskSnapshot.getBytesTransferred()) /  taskSnapshot.getTotalByteCount();
//                    }
//                });
//    }

}


