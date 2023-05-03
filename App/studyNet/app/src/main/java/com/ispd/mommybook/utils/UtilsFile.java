package com.ispd.mommybook.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UtilsFile {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    public static String GetAssetFilePath(Context in_context, String in_assetName)
            throws IOException {
        File file = new File(in_context.getFilesDir(), in_assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = in_context.getAssets().open(in_assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param in_bitmap The bitmap to save.
     * @param in_fileName The location to save the bitmap to.
     */
    public static void SaveBitmap(final Bitmap in_bitmap, final String in_fileName) {
        final String root = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                                                         File.separator +
                                                                         "tensorflow";
        LOGGER.d("Saving %dx%d bitmap to %s.", in_bitmap.getWidth(), in_bitmap.getHeight(), root);
        final File myDir = new File(root);

        if (!myDir.mkdirs()) {
            LOGGER.d("Make dir failed");
        }

        final String fileName = in_fileName;
        final File file = new File(myDir, fileName);
        if (file.exists()) {
            //file.delete();
        }
        LOGGER.d("file : "+file);
        try {
            final FileOutputStream out = new FileOutputStream(file);
            in_bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
        }
    }
}
