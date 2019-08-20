package com.gmail.katsaros.s.dimitris.e_ktima;

import android.content.ContentProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

public class LogFileProvider extends ContentProvider {

    private static final String CLASS_NAME = "LogFile";

    // The authority is the symbolic name for the provider class
    public static final String AUTHORITY = "com.gmail.katsaros.s.dimitris.e_ktima.LogFileProvider";

    // UriMatcher used to match against incoming requests
    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a URI to the matcher which will match against the form
        // 'content://it.my.app.LogFileProvider/*'
        // and return 1 in the case that the incoming Uri matches this pattern
        uriMatcher.addURI(AUTHORITY, "*", 1);

        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {

        String LOG_TAG = CLASS_NAME + " - openFile";

        Log.v(LOG_TAG,
                "Called with uri: '" + uri + "'." + uri.getLastPathSegment());

        // Check incoming Uri against the matcher
        switch (uriMatcher.match(uri)) {

            // If it returns 1 - then it matches the Uri defined in onCreate
            case 1:

                // The desired file name is specified by the last segment of the
                // path
                // E.g.
                // 'content://it.my.app.LogFileProvider/Test.txt'
                // Take this and build the path to the file
                String fileLocation = getContext().getCacheDir() + File.separator
                        + uri.getLastPathSegment();

                // Create & return a ParcelFileDescriptor pointing to the file
                // Note: I don't care what mode they ask for - they're only getting
                // read only
                return ParcelFileDescriptor.open(new File(
                        fileLocation), ParcelFileDescriptor.MODE_READ_ONLY);

            // Otherwise unrecognised Uri
            default:
                Log.v(LOG_TAG, "Unsupported uri: '" + uri + "'.");
                throw new FileNotFoundException("Unsupported uri: "
                        + uri.toString());
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentvalues, String s,
                      String[] as) {
        return 0;
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] as) {
        return 0;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentvalues) {
        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String s, String[] as1,
                        String s1) {
        return null;
    }

    public static void createCachedFile(Context context, String fileName,
                                        String content) throws IOException {
        File cacheFile = new File(context.getCacheDir() + File.separator
                + fileName);
        Log.d("LogFileProvider", "createCachedFile: " + cacheFile + "  " + cacheFile.getAbsolutePath());
        cacheFile.createNewFile();

        FileOutputStream fos = new FileOutputStream(cacheFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
        PrintWriter pw = new PrintWriter(osw);

        pw.println(content);

        pw.flush();
        pw.close();
    }
}