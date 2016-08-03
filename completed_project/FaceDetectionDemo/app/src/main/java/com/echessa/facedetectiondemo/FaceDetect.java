package com.echessa.facedetectiondemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
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
    private DetectProvider detectProvider;

    private SparseArray<Face> faces;
    private int facesCount;

    public enum DetectType {
        Face,
        Gender
    }

    public enum DetectProvider {
        AndroidMedia,
        PlayService,
        FacePlus
    }

    public interface DetectListener {
        void onSuccess();
        void onFail();
    }

    public FaceDetect(Context c) {
        context = c;
    }

    private void detectUsingGms(Bitmap bitmap) {
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        // Create a frame from the bitmap and run face detection on the frame.
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        faces = detector.detect(frame);
        if (listener != null ) {
            listener.onSuccess();
        }
        detector.release();
    }

    private void detectUsingNative(Bitmap bitmap) {
        if (null == bitmap) {
            Log.d(TAG, "detect local error no bitmap");
        }
        android.media.FaceDetector faceDetector = new android.media.FaceDetector(bitmap.getWidth(), bitmap.getHeight(), 3);
        android.media.FaceDetector.Face faces[] = new android.media.FaceDetector.Face[3];
        facesCount = faceDetector.findFaces(bitmap, faces);
        for (int i = 0; i < facesCount; i++){
            Log.d(TAG, faces[i].toString());
        }
        if (listener != null ) {
            listener.onSuccess();
        }
    }

    private Thread thread;
    private boolean isRunning = false;
    private void detectUsingFacePlus(File file) {
        final PostParameters parameters = new PostParameters();
        parameters.setImg(file);
        final Handler handler = new Handler();
        thread = new Thread() {
            @Override
            public void run() {
//                Log.d("FacePlusDetect", "Detect Request :" + parameters.toString());
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
     * 传入 Bitmap 检测
     * @param bitmap 图片
     * @param listener 回调
     */
    public void detectWithBitmap(Bitmap bitmap, DetectListener listener) {
        this.listener = listener;
        detectUsingGms(bitmap);
    }

    /**
     * give local path like /sdcard/facedetect/1.png
     * @param localpath local path
     */
    public void detectWithFile(File localpath, DetectListener l) {
        listener = l;
//        if (filepath.exists()) {
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            Bitmap bitmap = BitmapFactory.decodeFile(filepath.getAbsolutePath(), options);
//            detectUsingGms(bitmap);
//        }
        detectUsingFacePlus(localpath);
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

    }
}
