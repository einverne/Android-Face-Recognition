package com.echessa.facedetectiondemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;

/**
 * Created by einverne on 16/8/3.
 */

public class FaceDetect {

    private static final String TAG = "EV_TAG";
    private Context context;
    private DetectListener listener;

    private SparseArray<Face> faces;            // 保存GMS中返回数据
    private int facesCount;                     // 保存识别出的人脸数量
    DetectProvider detectProvider = DetectProvider.PlayService;              // 人脸识别提供商

    private FaceDetector detector;

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
        this.context = c;
    }

    private void detectUsingGms(Bitmap bitmap) {
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
        if (listener != null ) {
            listener.onSuccess();
        }
    }

    /**
     * 使用 android.media 包中识别人脸
     * @param bitmap
     */
    private void detectUsingNative(Bitmap bitmap) {
        if (null == bitmap) {
            Log.d(TAG, "detect local error no bitmap");
            return;
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
     * @param localpath local path
     */
    public void detectWithFile(String localpath) {
        File filepath = new File(localpath);
        if (filepath.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(filepath.getAbsolutePath(), options);
            detectUsingGms(bitmap);
        }
    }

    public void detectWithUrl(String url) {

    }

    public SparseArray<Face> getDetectFaces() {
        return faces;
    }

    public int getFacesCount() {
        return facesCount;
    }

    public void cancel() {
        listener = null;
        detector.release();

    }
}
