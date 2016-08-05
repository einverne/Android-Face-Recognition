package com.echessa.facedetectiondemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by einverne on 16/8/3.
 */

public class FaceDetect {
    private static final String FACEPLUSPLUS_APIKEY = "c659ff0d3a08601d115cf34035e53d5e";
    private static final String FACEPLUSPLUS_APISECRET = "JzDjJ8QDjouiwDkDSTV4KYd8yrXcf01m";

    private static final String TAG = "EV_TAG";
    private Context context;
    private DetectListener listener;

    private int MEDIA_MAX_DETECT_FACE_NUMBER = 5;
    private SparseArray<Face> faces;            // 保存GMS中返回数据
    private int facesCount;                     // 保存识别出的人脸数量
    private DetectProvider detectProvider = DetectProvider.AndroidMedia;              // 人脸识别提供商

    private FaceDetector detector;

    private Thread thread;
    private boolean isRunning = false;

    public enum DetectProvider {
        AndroidMedia,
        PlayService,
        FacePlus
    }

    public enum DetectType {
        Face,
        Gender
    }

    public interface DetectListener {
        void onSuccess();
        void onFail();
    }

    public FaceDetect(Context c) {
        context = c;
    }

    /**
     * 使用 Play Service 中人脸检测
     *
     * @param bitmap
     */
    private void detectUsingGms(Bitmap bitmap) {
        if (null == bitmap) {
            return;
        }
        facesCount = 0;

        detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.
        Detector<Face> safeDetector = new SafeFaceDetector(detector);

        // Create a frame from the bitmap and run face detection on the frame.
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        faces = safeDetector.detect(frame);

        if (!safeDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.

            if (listener != null) {
                listener.onFail();
            }
            return;
        }
        if (listener != null) {
            listener.onSuccess();
        }
    }

    /**
     * 使用 android.media 包中识别人脸
     *
     * @param bitmap
     */
    private void detectUsingNative(final Bitmap bitmap) {
        if (null == bitmap) {
            Log.d(TAG, "detect local error no bitmap");
            return;
        }
        facesCount = 0;
        final android.media.FaceDetector faceDetector = new android.media.FaceDetector(bitmap.getWidth(), bitmap.getHeight(), MEDIA_MAX_DETECT_FACE_NUMBER);
        final android.media.FaceDetector.Face faces[] = new android.media.FaceDetector.Face[MEDIA_MAX_DETECT_FACE_NUMBER];
        final Handler handler = new Handler();
        thread = new Thread() {
            @Override
            public void run() {
                facesCount = faceDetector.findFaces(bitmap, faces);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    }
                });

                isRunning = false;
            }
        };
        thread.start();
        isRunning = true;
    }

    /**
     * 使用 Face++ 人脸检测
     *
     * @param file
     */
    private void detectUsingFacePlus(File file) {
        final PostParameters parameters = new PostParameters();
        parameters.setImg(file);
        final Handler handler = new Handler();
        thread = new Thread() {
            @Override
            public void run() {
                Log.d("FacePlusDetect", "Detect Request :" + parameters.toString());
                HttpRequests httpRequests = new HttpRequests(FACEPLUSPLUS_APIKEY, FACEPLUSPLUS_APISECRET, false, true);
                JSONObject result = null;
                boolean detectSucced = false;
                try {
                    result = httpRequests.detectionDetect(parameters);
                    if (result != null) {
                        JSONArray faces = result.getJSONArray("face");
                        if (faces != null && faces.length() > 0 && null != listener) {
                            // Has face!!
                            facesCount = faces.length();
//                            hasFace = true;
//                            String genderStr = faces.getJSONObject(0).getJSONObject("attribute").getJSONObject("gender").getString("value");
//                            gender = Gender.getValueOf(genderStr);
                            detectSucced = true;
                        } else {
//                            hasFace = false;
//                            detectSucced = true;
//                            gender = Gender.OTHER;
                        }
//                        Log.d("FacePlusDetect", "Detect Result : hasFace = " + hasFace + "; gender = " + gender.toString());
                    }
                } catch (FaceppParseException e) {
                    Log.d(TAG, "Detect FaceppParseException !");
                    e.printStackTrace();
                } catch (JSONException e) {
//                    if (hasFace) {
//                        gender = Gender.OTHER;
//                    }
                    Log.d(TAG, "Detect JSONException !");
                    e.printStackTrace();
                }

                if (detectSucced) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onSuccess();
                            }
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onFail();
                            }
                        }
                    });
                }

                isRunning = false;
            }
        };
        thread.start();
        isRunning = true;
    }

    /**
     * 提供外部使用, 传入 Bitmap 检测
     *
     * @param bitmap   图片
     * @param listener 回调
     */
    public void detectWithBitmap(Bitmap bitmap, DetectListener listener) {
        this.listener = listener;
        switch (detectProvider) {
            case PlayService:
                detectUsingGms(bitmap);
                break;
            case AndroidMedia:
                detectUsingNative(bitmap);
                break;
            case FacePlus:

                break;
        }
    }

    /**
     * give local path like /sdcard/facedetect/1.png
     *
     * @param localFile local file
     */
    public void detectWithFile(File localFile, DetectListener l) {
        if (!localFile.exists() || l == null) return;
        listener = l;
        switch (detectProvider) {
            case PlayService:
                BitmapFactory.Options psOptions = new BitmapFactory.Options();
                Bitmap psBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath(), psOptions);
                detectUsingGms(psBitmap);
                break;
            case AndroidMedia:
                BitmapFactory.Options mOptions = new BitmapFactory.Options();
                mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                //要使用Android内置的人脸识别，需要将Bitmap对象转为RGB_565格式，否则无法识别
                Bitmap mBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath(), mOptions);
                detectUsingNative(mBitmap);
                break;
            case FacePlus:
                detectUsingFacePlus(localFile);

                break;
        }
    }

    public void detectWithUrl(String url) {

    }

    public SparseArray<Face> getDetectFaces() {
        return faces;
    }

    public int getFacesCount() {
        if (faces != null) {
            return faces.size();
        }
        return facesCount;
    }

    public void cancel() {
        listener = null;
        detector.release();

    }
}
