package com.echessa.facedetectiondemo;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
 * FaceDetect using android.media, play service && face++
 */

public class FaceDetect {
    private static final String FACEPLUSPLUS_APIKEY = "c659ff0d3a08601d115cf34035e53d5e";
    private static final String FACEPLUSPLUS_APISECRET = "JzDjJ8QDjouiwDkDSTV4KYd8yrXcf01m";

    private static final String TAG = "EV_TAG";
    private Context context;                    // used by Play Service
    private DetectListener listener;

    private int MEDIA_MAX_DETECT_FACE_NUMBER = 5;
    private android.media.FaceDetector.Face androidNativeFacesResults[];
    private SparseArray<Face> faces;            // 保存GMS中返回数据
    private int facesCount;                     // 保存识别出的人脸数量

    private DetectProvider detectProvider = DetectProvider.AndroidMedia;              // 人脸识别提供商

    private FaceDetector detector;              // Play Service 人脸检测

    private Thread thread;
    private boolean isRunning = false;          // 是否在检测中

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
     * @param bitmap Bitmap
     */
    private void detectUsingGms(Bitmap bitmap) {
        if (null == bitmap) {
            if (listener != null) {
                listener.onFail();
            }
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
     * There are some limitation in this 用android.media 包中识别人脸package.
     * 使用使用使Face Detection API's input Bitmap must :
     * <p/>
     * 1. config with Config.RGB_565<br/>
     * 2. Bitmap width must be even<br/>
     * <p/>
     * more details can be checked
     * http://stackoverflow.com/q/17640206/1820217
     *
     * @param bitmap Bitmap
     */
    private void detectUsingNative(final Bitmap bitmap) {
        if (null == bitmap || isRunning) {
            if (listener != null) {
                listener.onFail();
            }
            return;
        }
        facesCount = 0;
        final android.media.FaceDetector faceDetector = new android.media.FaceDetector(bitmap.getWidth(), bitmap.getHeight(), MEDIA_MAX_DETECT_FACE_NUMBER);
        androidNativeFacesResults = new android.media.FaceDetector.Face[MEDIA_MAX_DETECT_FACE_NUMBER];
        final Handler handler = new Handler();
        thread = new Thread() {
            @Override
            public void run() {
                facesCount = faceDetector.findFaces(bitmap, androidNativeFacesResults);

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
     * @param file File
     */
    private void detectUsingFacePlus(File file) {
        if (!file.exists() || isRunning) {
            if (listener != null) {
                listener.onFail();
            }
            return;
        }
        final PostParameters parameters = new PostParameters();
        parameters.setImg(file);
        final Handler handler = new Handler();
        facesCount = 0;
        thread = new Thread() {
            @Override
            public void run() {
                boolean hasFace = false;
                boolean detectSucceed = false;
                Log.d("FacePlusDetect", "Detect Request :" + parameters.toString());
                HttpRequests httpRequests = new HttpRequests(FACEPLUSPLUS_APIKEY, FACEPLUSPLUS_APISECRET, false, true);
                JSONObject result;
                try {
                    result = httpRequests.detectionDetect(parameters);
                    if (result != null) {
                        detectSucceed = true;
                        JSONArray faces = result.getJSONArray("face");
                        if (faces != null && faces.length() > 0 && null != listener) {
                            // Has face!!
                            facesCount = faces.length();
                            hasFace = true;
//                            String genderStr = faces.getJSONObject(0).getJSONObject("attribute").getJSONObject("gender").getString("value");
//                            gender = Gender.getValueOf(genderStr);
                        } else {
                            hasFace = false;
//                            detectSucceed = true;
//                            gender = Gender.OTHER;
                        }
//                        Log.d("FacePlusDetect", "Detect Result : hasFace = " + hasFace + "; gender = " + gender.toString());
                    }
                } catch (FaceppParseException e) {
                    detectSucceed = false;
                    Log.d(TAG, "Detect FaceppParseException !");
                    e.printStackTrace();
                } catch (JSONException e) {
//                    if (hasFace) {
//                        gender = Gender.OTHER;
//                    }
                    Log.d(TAG, "Detect JSONException !");
                    e.printStackTrace();
                }

                if (detectSucceed) {
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

    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromFile(File filepath, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filepath.getAbsolutePath(), options);
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
    @TargetApi(Build.VERSION_CODES.KITKAT)
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
                mOptions.inMutable = true;          // available api level >= 11
                //要使用Android内置的人脸识别，需要将Bitmap对象转为RGB_565格式，否则无法识别
                Bitmap mBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath(), mOptions);
                // Bitmap to detect must has a even width
                if (mBitmap.getWidth() % 2 == 1) {
                    mBitmap = Bitmap.createScaledBitmap(mBitmap, mBitmap.getWidth() - 1, mBitmap.getHeight(), true);
                }
                detectUsingNative(mBitmap);
                break;
            case FacePlus:
                detectUsingFacePlus(localFile);

                break;
        }
    }

    public void detectWithUrl(String url) {

    }

    public void setDetectProvider(DetectProvider detectProvider) {
        this.detectProvider = detectProvider;
    }

    public SparseArray<Face> getDetectFaces() {
        return faces;
    }

    /**
     * Get the detect Face count
     *
     * @return count of faces detect
     */
    public int getFacesCount() {
        // deal the facesCount in each detect function
        return facesCount;
    }

    public android.media.FaceDetector.Face[] getAndroidMediaDetectResult() {
        if (detectProvider == DetectProvider.AndroidMedia && androidNativeFacesResults != null) {
            return androidNativeFacesResults;
        }
        return null;
    }

    public void getFacesArea() {
        RectF rectf[] = new RectF[facesCount];
        switch (detectProvider) {
            case AndroidMedia:
                for (int i = 0; i < androidNativeFacesResults.length; i++){
                    android.media.FaceDetector.Face face = androidNativeFacesResults[0];
                    if (face != null) {
                        float eyeDistance = face.eyesDistance();
                        PointF midEyesPoint = new PointF();
                        face.getMidPoint(midEyesPoint);
                        rectf[i].set(midEyesPoint.x - eyeDistance,
                                midEyesPoint.y - eyeDistance,
                                midEyesPoint.x + eyeDistance,
                                midEyesPoint.y + eyeDistance);
                    }
                }
                break;
            default:

        }
    }

    public boolean isGooglePlayServiceAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return status == ConnectionResult.SUCCESS;
    }

    public void cancel() {
        listener = null;
        detector.release();

    }
}
