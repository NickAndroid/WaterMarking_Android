package dev.nick.watermarking;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PickerActivity extends AppCompatActivity {

    private static final String TAG = "PickerActivity";

    private static final int REQUEST_CODE_CAMERA_WITH_DATA = 1001;
    private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 1002;
    private static final int REQUEST_CROP_PHOTO = 1003;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1004;

    private static final String PHOTO_DATE_FORMAT = "'IMG'_yyyyMMdd_HHmmss";

    private Uri mCroppedPhotoUri;
    private Uri mTempPhotoUri;
    private Uri mCurrentPhotoUri;

    public static Uri generateTempImageUri(Context context) {
        final String fileProviderAuthority = context.getResources().getString(
                R.string.photo_file_provider_authority);
        return FileProvider.getUriForFile(context, fileProviderAuthority,
                new File(pathForTempPhoto(context, generateTempPhotoFileName())));
    }

    public static Uri generateTempCroppedImageUri(Context context) {
        final String fileProviderAuthority = context.getResources().getString(
                R.string.photo_file_provider_authority);
        return FileProvider.getUriForFile(context, fileProviderAuthority,
                new File(pathForTempPhoto(context, generateTempCroppedPhotoFileName())));
    }

    private static String pathForTempPhoto(Context context, String fileName) {
        final File dir = context.getCacheDir();
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    private static String generateTempPhotoFileName() {
        final Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(PHOTO_DATE_FORMAT, Locale.US);
        return "Themed_ic-" + dateFormat.format(date) + ".jpg";
    }

    private static String generateTempCroppedPhotoFileName() {
        final Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(PHOTO_DATE_FORMAT, Locale.US);
        return "Themed_ic-" + dateFormat.format(date) + "-cropped.jpg";
    }

    public static void addCropExtras(Intent intent, int photoSize) {
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", photoSize);
        intent.putExtra("outputY", photoSize);
    }

    /**
     * Given a uri pointing to a bitmap, reads it into a bitmap and returns it.
     *
     * @throws FileNotFoundException
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws FileNotFoundException {
        final InputStream imageStream = context.getContentResolver().openInputStream(uri);
        try {
            return BitmapFactory.decodeStream(imageStream);
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {

                }
            }
        }
    }

    /**
     * Creates a byte[] containing the PNG-compressed bitmap, or null if
     * something goes wrong.
     */
    public static byte[] compressBitmap(Bitmap bitmap) {
        final int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Unable to serialize photo: " + e.toString());
            return null;
        }
    }

    /**
     * Given an input photo stored in a uri, save it to a destination uri
     */
    public static boolean savePhotoFromUriToUri(Context context, Uri inputUri, Uri outputUri,
                                                boolean deleteAfterSave) {
        if (inputUri == null || outputUri == null) {
            return false;
        }
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = context.getContentResolver()
                    .openAssetFileDescriptor(outputUri, "rw").createOutputStream();
            inputStream = context.getContentResolver().openInputStream(
                    inputUri);

            final byte[] buffer = new byte[16 * 1024];
            int length;
            int totalLength = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalLength += length;
            }
            Log.v(TAG, "Wrote " + totalLength + " bytes for photo " + inputUri.toString());
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Failed to write photo: " + inputUri.toString() + " because: " + e);
            return false;
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (IOException e) {

            }
            if (outputStream != null) try {
                outputStream.close();
            } catch (IOException e) {

            }
            if (deleteAfterSave) {
                context.getContentResolver().delete(inputUri, null, null);
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTempPhotoUri = generateTempImageUri(this);
        mCroppedPhotoUri = generateTempCroppedImageUri(this);
    }

    protected void startTakePhotoActivity() {
        startTakePhotoActivity(mTempPhotoUri);
    }

    protected void startPickFromGalleryActivity() {
        startPickFromGalleryActivity(mTempPhotoUri);
    }

    /**
     * Should initiate an activity to take a photo using the camera.
     *
     * @param photoUri The file path that will be used to store the photo.  This is generally
     *                 what should be returned by.
     */
    private void startTakePhotoActivity(Uri photoUri) {
        final Intent intent = getTakePhotoIntent(photoUri);
        startPhotoActivity(intent, REQUEST_CODE_CAMERA_WITH_DATA, photoUri);
    }

    /**
     * Should initiate an activity pick a photo from the gallery.
     *
     * @param photoUri The temporary file that the cropped image is written to before being
     *                 stored by the content-provider.
     */
    private void startPickFromGalleryActivity(Uri photoUri) {
        final Intent intent = getPhotoPickIntent(photoUri);
        startPhotoActivity(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA, photoUri);
    }

    /**
     * Constructs an intent for picking a photo from Gallery, and returning the bitmap.
     */
    private Intent getPhotoPickIntent(Uri outputUri) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        addPhotoPickerExtras(intent, outputUri);
        return intent;
    }

    /**
     * Constructs an intent for capturing a photo and storing it in a temporary output uri.
     */
    private Intent getTakePhotoIntent(Uri outputUri) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        addPhotoPickerExtras(intent, outputUri);
        return intent;
    }

    void startPhotoActivity(Intent intent, int code, Uri uri) {
        mCurrentPhotoUri = uri;
        startActivityForResult(intent, code);
    }

    /**
     * Adds common extras to gallery intents.
     *
     * @param intent   The intent to add extras to.
     * @param photoUri The uri of the file to save the image to.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void addPhotoPickerExtras(Intent intent, Uri photoUri) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, photoUri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handlePhotoActivityResult(requestCode, resultCode, data);
    }

    /**
     * Attempts to handle the given activity result.  Returns whether this handler was able to
     * process the result successfully.
     *
     * @param requestCode The request code.
     * @param resultCode  The result code.
     * @param data        The intent that was returned.
     * @return Whether the handler was able to process the result.
     */
    public boolean handlePhotoActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // Cropped photo was returned
                case REQUEST_CROP_PHOTO: {
                    final Uri uri;
                    if (data != null && data.getData() != null) {
                        uri = data.getData();
                    } else {
                        uri = mCroppedPhotoUri;
                    }

                    // delete the original temporary photo if it exists
                    getContentResolver().delete(mTempPhotoUri, null, null);
                    onPhotoSelected(uri);
                    return true;
                }


                case REQUEST_CODE_PHOTO_PICKED_WITH_DATA:
                    final Uri pickedUri;
                    if (data != null && data.getData() != null) {
                        pickedUri = data.getData();
                    } else {
                        pickedUri = getCurrentPhotoUri();
                    }
                    onPhotoSelected(pickedUri);
                    break;
                // Photo was successfully taken, now crop it.
                case REQUEST_CODE_CAMERA_WITH_DATA:
                    final Uri takenUri;
                    boolean isWritable = false;
                    if (data != null && data.getData() != null) {
                        takenUri = data.getData();
                    } else {
                        takenUri = getCurrentPhotoUri();
                        isWritable = true;
                    }
                    final Uri toCrop;
                    if (isWritable) {
                        // Since this uri belongs to our file provider, we know that it is writable
                        // by us. This means that we don't have to save it into another temporary
                        // location just to be able to crop it.
                        toCrop = takenUri;
                    } else {
                        toCrop = mTempPhotoUri;
                        try {
                            if (!savePhotoFromUriToUri(this, takenUri,
                                    toCrop, false)) {
                                return false;
                            }
                        } catch (SecurityException e) {
                            Log.d(TAG, "Did not have read-access to uri : " + takenUri);
                            return false;
                        }
                    }

                    doCropPhoto(toCrop, mCroppedPhotoUri);
                    return true;
            }
        }
        return false;
    }

    protected void onPhotoSelected(Uri uri) {
        Log.d(TAG, "onPhotoSelected, Uri=" + uri.toString());
    }

    Uri getCurrentPhotoUri() {
        return mCurrentPhotoUri;
    }

    /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    private void doCropPhoto(Uri inputUri, Uri outputUri) {
        final Intent intent = getCropImageIntent(inputUri, outputUri);
        if (!hasIntentHandler(intent)) {
            onPhotoSelected(inputUri);
            return;
        }
        try {
            // Launch gallery to crop the photo
            startPhotoActivity(intent, REQUEST_CROP_PHOTO, inputUri);
        } catch (Exception e) {
            Log.e(TAG, "Cannot crop image", e);
        }
    }

    private boolean hasIntentHandler(Intent intent) {
        final List<ResolveInfo> resolveInfo = getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.size() > 0;
    }

    /**
     * Constructs an intent for image cropping.
     */
    private Intent getCropImageIntent(Uri inputUri, Uri outputUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputUri, "image/*");
        addPhotoPickerExtras(intent, outputUri);
        addCropExtras(intent, 240);// FIXME: 16-8-11 Real size.
        return intent;
    }

}
