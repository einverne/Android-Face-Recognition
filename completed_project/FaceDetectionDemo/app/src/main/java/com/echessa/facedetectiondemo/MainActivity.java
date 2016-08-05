package com.echessa.facedetectiondemo;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.gms.vision.face.Face;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
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
    TextView tvPicIndex;
    CustomView overlay;
    private int failedCount = 0;

    private ArrayList<File> picturesToDetect = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFaceCount = (TextView) findViewById(R.id.faceCount);
        tvPicIndex = (TextView) findViewById(R.id.picIndex);
        overlay = (CustomView) findViewById(R.id.customView);

//        InputStream stream = getResources().openRawResource(R.raw.image04);
//        final Bitmap bitmap = BitmapFactory.decodeStream(stream);
//        detectFaces(bitmap);
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
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.DIR_SELECT;
                properties.root = new File("/sdcard/");
                properties.extensions = null;

                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        if (files.length <= 0) return;
                        addFileFromDir(new File(files[0]));
                        detectFaces();
                    }
                });
                dialog.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    picturesToDetect.add(new File(getPath(selectedImage)));
                    detectFaces();
                }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        startManagingCursor(cursor);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void detectFaces() {
        if (picturesToDetect.size() <= 0) {
            return;
        }
        final File pic = picturesToDetect.remove(0);
        tvPicIndex.setText("photo left: " + picturesToDetect.size());
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        final Bitmap bitmap = BitmapFactory.decodeFile(pic.getAbsolutePath(), options);

        final FaceDetect faceDetect = new FaceDetect(getApplicationContext());
        faceDetect.detectWithFile(pic, new FaceDetect.DetectListener() {
            @Override
            public void onSuccess() {
//                updateUI(bitmap, faces);
                faceCount = faceDetect.getFacesCount();
                tvFaceCount.setText("detect success face count: " + faceCount);
                Log.d(TAG, "detect success face count " + faceCount);
//                if (faceCount == 0) {
//                    saveToLocal("/sdcard/facedetectfailed", pic);
//                } else {
//                    saveToLocal("/sdcard/facedetectsuccess", pic);
//                }
            }

            @Override
            public void onFail() {

            }
        });

    }

    private void detectFaces(final File file) {
        final FaceDetect faceDetect = new FaceDetect(getApplicationContext());
        faceDetect.detectWithFile(file, new FaceDetect.DetectListener() {
            @Override
            public void onSuccess() {

                faceCount = faceDetect.getFacesCount();
//                updateUI(bitmap, faces);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        detectFaces();
                    }
                }, 2000);
//                if (faceCount == 0) {
//                    saveToLocal(bitmap);
//                }

            }

            @Override
            public void onFail() {
                detectFaces();
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

    private void addFileFromDir(File localDir) {
        File[] files = localDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.contains(".png") || filename.contains(".jpg");
            }
        });
        List filesDirs = Arrays.asList(files);
        Iterator filesIter = filesDirs.iterator();
        File file = null;
        while (filesIter.hasNext()) {
            file = (File) filesIter.next();
            picturesToDetect.add(file);
        }
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
     *
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
