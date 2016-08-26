package io.github.cluo29.videorecordertest;

import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private File myRecVideoFile;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TextView tvTime;
    private TextView tvSize;
    private Button btnStart;
    private Button btnStop;
    private Button btnCancel;
    private MediaRecorder recorder;
    private Handler handler;
    private Camera camera;
    private boolean recording; // 记录是否正在录像,fasle为未录像, true 为正在录像
    private int minute = 0;
    private int second = 0;
    private String time="";
    private String size="";
    private String fileName;
    private String name="";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mSurfaceView = (SurfaceView) findViewById(R.id.videoView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);
        handler = new Handler();
        tvTime = (TextView) findViewById(R.id.tv_video_time);
        tvSize=(TextView)findViewById(R.id.tv_video_size);
        btnStop = (Button) findViewById(R.id.button2);
        btnStart = (Button) findViewById(R.id.button);
        btnCancel = (Button) findViewById(R.id.button3);
        btnCancel.setOnClickListener(listener);
        btnStart.setOnClickListener(listener);
        btnStop.setOnClickListener(listener);
        // 设置sdcard的路径
        fileName =getApplicationContext().getExternalFilesDir(null)+"/Documents";
        File documents_folder = getApplicationContext().getExternalFilesDir(null); //get the root of OS handled app external folder
        File docs = new File( documents_folder, "Documents" ); //create a Documents folder if it doesn't exist
        if( ! docs.exists() )
            docs.mkdirs();
        //fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        name="video_" +System.currentTimeMillis() + ".mp4";
        fileName += File.separator+name;



    }

    /**
     * 录制过程中,时间变化,大小变化
     */

    private Runnable timeRun = new Runnable() {

        @Override
        public void run() {
            long fileLength=myRecVideoFile.length();
            if(fileLength<1024 && fileLength>0){
                size=String.format("%dB/10M", fileLength);
            }else if(fileLength>=1024 && fileLength<(1024*1024)){
                fileLength=fileLength/1024;
                size=String.format("%dK/10M", fileLength);
            }else if(fileLength>(1024*1024*1024)){
                fileLength=(fileLength/1024)/1024;
                size=String.format("%dM/10M", fileLength);
            }
            second++;
            if (second == 60) {
                minute++;
                second = 0;
            }
            time = String.format("%02d:%02d", minute, second);
            tvSize.setText(size);
            tvTime.setText(time);
            handler.postDelayed(timeRun, 1000);
        }
    };
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 开启相机
        if (camera == null) {
            int CammeraIndex=FindFrontCamera();
            if(CammeraIndex==-1){
                //ToastUtil.TextToast(getApplicationContext(), "您的手机不支持前置摄像头", ToastUtil.LENGTH_SHORT);
                CammeraIndex=FindBackCamera();
            }
            camera = Camera.open(CammeraIndex);
            try {
                camera.setPreviewDisplay(mSurfaceHolder);
                camera.setDisplayOrientation(90);
            } catch (IOException e) {
                e.printStackTrace();
                camera.release();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 开始预览
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 关闭预览并释放资源
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button2:
                    if(recorder!=null){
                        releaseMediaRecorder();
                        minute = 0;
                        second = 0;
                        handler.removeCallbacks(timeRun);
                        recording = false;
                    }
                    btnStart.setEnabled(true);
                    break;
                case R.id.button:
                    if(recorder!=null){
                        releaseMediaRecorder();
                        minute = 0;
                        second = 0;
                        handler.removeCallbacks(timeRun);
                        recording = false;
                    }
                    recorder();
                    btnStart.setEnabled(false);
                    break;
                case R.id.button3:
                    releaseMediaRecorder();
                    handler.removeCallbacks(timeRun);
                    minute=0;
                    second=0;
                    recording = false;
                    MainActivity.this.finish();
                    break;
            }
        }
    };

    //判断前置摄像头是否存在
    private int FindFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }
    //判断后置摄像头是否存在
    private int FindBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    //释放recorder资源
    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }
    //开始录像
    public void recorder() {
        if (!recording) {
            try {
                // 关闭预览并释放资源
                if(camera!=null){
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                recorder = new MediaRecorder();

                myRecVideoFile = new File(fileName);

                if(!myRecVideoFile.exists()){

                    myRecVideoFile.createNewFile();
                }

                recorder.reset();

                int CammeraIndex=FindFrontCamera();
                if(CammeraIndex==-1){

                    CammeraIndex=FindBackCamera();
                }

                camera = Camera.open(CammeraIndex);

                camera.setDisplayOrientation(90);

                camera.unlock();

                recorder.setCamera(camera);

                // Step 2: Set sources
                recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                recorder.setProfile(CamcorderProfile.get(CammeraIndex,CamcorderProfile.QUALITY_HIGH));

                recorder.setOutputFile(myRecVideoFile.getAbsolutePath());
                recorder.setPreviewDisplay(mSurfaceHolder.getSurface()); // 预览


                Log.d("Video","253");
                Log.d("Video",fileName);
                recorder.prepare(); // 准备录像
                Log.d("Video","255");


                recorder.start(); // 开始录像
                Log.d("Video","257");
                handler.post(timeRun); // 调用Runable
                recording = true; // 改变录制状态为正在录制
                Log.d("Video","260");
            } catch (IOException e1) {
                Log.d("Video","264");

                Log.d("Video", "IOException preparing MediaRecorder: " + e1.getMessage());
                releaseMediaRecorder();
                handler.removeCallbacks(timeRun);
                minute = 0;
                second = 0;
                recording = false;
                btnStart.setEnabled(true);
            } catch (IllegalStateException e) {
                Log.d("Video", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
                releaseMediaRecorder();
                handler.removeCallbacks(timeRun);
                minute = 0;
                second = 0;
                recording = false;
                btnStart.setEnabled(true);
            }
        } else
            Log.d("Video", "recording...");
    }
}
