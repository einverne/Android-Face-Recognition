package com.echessa.facedetectiondemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Created by einverne on 16/8/3.
 */

public class FaceDetect {

    private static final String TAG = "EV_TAG";
    private Context context;
    private DetectListener listener;

    private SparseArray<Face> faces;
    private int facesCount;

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

    public void detectWithBitmap(Bitmap bitmap, DetectListener listener) {
        this.listener = listener;
        detectUsingGms(bitmap);
    }

    public void detectWithFile(String localpath) {

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

    }
}
