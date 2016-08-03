package com.echessa.facedetectiondemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.vision.face.Face;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final int SELECT_PHOTO = 100;
    private static final String TAG = "EV_TAG";
    private ArrayList<File> pictures;
    private int faceCount = 0;
    private Bitmap selectedBitmap;

    TextView tvFaceCount;
    TextView picIndex;
    CustomView overlay;
    private int failedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFaceCount = (TextView) findViewById(R.id.faceCount);
        picIndex = (TextView) findViewById(R.id.picIndex);
        overlay = (CustomView) findViewById(R.id.customView);

        InputStream stream = getResources().openRawResource(R.raw.image04);
        final Bitmap bitmap = BitmapFactory.decodeStream(stream);

        detectFaces(bitmap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_open:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                break;
            case R.id.action_settings:

                break;
            case R.id.action_opendir:
                openDir();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openDir() {
        pictures = getFileFromDir(new File("/sdcard/facedetect1"));

        detectNext();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    try {
                        selectedBitmap = decodeUri(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    detectFaces(selectedBitmap);
                }
        }
    }

    private void detectFaces(final Bitmap bitmap) {
        final FaceDetect faceDetect = new FaceDetect(getApplicationContext());
//        faceDetect.detectWithBitmap(bitmap, new FaceDetect.DetectListener() {
//            @Override
//            public void onSuccess() {
//                SparseArray<Face> faces = faceDetect.getDetectFaces();
//                faceCount = faces.size();
//                updateUI(bitmap, faces);
//                faceCount = faceDetect.getFacesCount();
//                Log.d(TAG, "detect success face count " + faceCount);
//                if (faceCount == 0) {
//                    saveToLocal("/sdcard/facedetectfailed", bitmap);
//                } else {
//                    saveToLocal("/sdcard/facedetectsuccess", bitmap);
//                }
//            }
//
//            @Override
//            public void onFail() {
//
//            }
//        });

    }

    private void detectNext() {
        if (pictures.size() > 0) {
            final File pic = pictures.remove(0);
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    final Bitmap bitmap = BitmapFactory.decodeFile(pic.getAbsolutePath(), options);

            detectFaces(pic);
        }
    }

    private void detectFaces(final File file) {
        final FaceDetect faceDetect = new FaceDetect(getApplicationContext());
        faceDetect.detectWithFile(file, new FaceDetect.DetectListener() {
            @Override
            public void onSuccess() {
                faceCount = faceDetect.getFacesCount();
                Log.d(TAG, "detect success face count " + faceCount);
                if (faceCount == 0) {
                    saveToLocal("/sdcard/facedetectfailed", file);
                } else {
                    saveToLocal("/sdcard/facedetectsuccess", file);
                }
                faceCount = 0;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        detectNext();
                    }
                }, 2000);
            }

            @Override
            public void onFail() {
                Log.d(TAG, "detect failed face count " + faceCount);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        detectNext();
                    }
                }, 2000);
            }
        });
    }

    private void updateUI(final Bitmap bitmap, SparseArray<Face> faces) {
        tvFaceCount.setText(faceCount + " faces detected");
//        overlay.setContent(bitmap, faces);
    }

    // http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }

    private ArrayList<File> getFileFromDir(File localDir) {
        ArrayList result = new ArrayList();
        File[] files = localDir.listFiles();
        List filesDirs = Arrays.asList(files);
        Iterator filesIter = filesDirs.iterator();
        File file = null;
        while (filesIter.hasNext()) {
            file = (File) filesIter.next();
            result.add(file);
        }
        return result;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * save fileIn to path with same name
     * @param path
     * @param fileIn
     */
    private void saveToLocal(String path, File fileIn) {
        File fileDst = new File(path + "/" + fileIn.getName());
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(fileIn);
            out = new FileOutputStream(fileDst);
            byte[] buf = new byte[2048];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToLocal(String path, Bitmap bitmap) {
        File fileFailedDir = new File(path);
        if (!fileFailedDir.exists()) {
            fileFailedDir.mkdir();
        }
        File filename = new File(fileFailedDir, failedCount + ".png");
        failedCount++;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bitmap.recycle();
    }
}
