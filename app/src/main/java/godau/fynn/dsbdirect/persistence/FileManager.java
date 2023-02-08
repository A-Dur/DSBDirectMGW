/*
 * DSBDirect
 * Copyright (C) 2019 Fynn Godau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with heinekingmedia GmbH, the
 * developer of the DSB platform.
 */

package godau.fynn.dsbdirect.persistence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.util.Log;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.download.DownloadManager;
import godau.fynn.dsbdirect.model.Table;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import static godau.fynn.dsbdirect.table.reader.Reader.CONTENT_IMAGE;

public class FileManager {

    private final Context context;

    public FileManager(Context context) {
        this.context = context;

        deleteOldFiles();
    }

    /**
     * Write a String to a file
     *
     * @param table    Table whose contents are to be saved
     * @param response Contents (of table) to be saved
     */
    public void saveFile(final Table table, final String response) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String filename = getFilename(table.getUri(), table.getPublishedDate());

                File file = new File(context.getFilesDir(), filename);

                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(response.getBytes());
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     * Write bitmap to a file
     *
     * @param table  The table the file belongs to
     * @param bitmap The bitmap to be written into a file
     */
    public void saveFile(final Table table, final Bitmap bitmap) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String filename = getFilename(table.getUri(), table.getPublishedDate());

                File file = new File(context.getFilesDir(), filename);

                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    public @Nullable
    String readHtmlFile(File file) {
        // Read a String from a File

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);

            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public @Nullable
    Bitmap readBitmapFile(File file) {
        // Read a Bitmap from a File

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return BitmapFactory.decodeStream(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check whether this specific version of a file has already been downloaded
     *
     * @param filename The filename (use {@link #getFilename(String, Date)})
     * @return Whether the file is already downloaded or not
     * @see #exists(Table)
     */
    private boolean exists(String filename) {
        File file = new File(context.getFilesDir(), filename);
        return file.exists();
    }


    private File[] listFiles() {
        return context.getFilesDir().listFiles();
    }

    private void deleteOldFiles() {
        // Automatically delete old files

        new Thread(new Runnable() {
            @Override
            public void run() {
                String storeDurationString = new Utility(context).getSharedPreferences().getString("cache_duration", "604800000");
                try {
                    int storeDuration = Integer.parseInt(storeDurationString);

                    File[] files = context.getFilesDir().listFiles();

                    for (File file : files) {
                        long lastModified = file.lastModified();
                        long now = new Date().getTime();
                        long delta = now - lastModified;
                        if (delta > storeDuration) {
                            boolean deleted = file.delete();
                            Log.d("AUTODELETE", "could delete file " + file.getName() + "? " + deleted);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Normal behavior if files shouldn't be deleted
                    // Also prevents crash when someone plays around with the sharedPreferences file
                    Log.d("AUTODELETE", "deletion duration is not a valid int. files not deleted");
                }

            }
        }).start();
    }

    private String getFilename(String uri, Date date) {
        String path = Uri.parse(uri).getPath().replaceAll("[\\\\/]", "-");
        return date.getTime() + "-" + path;
    }

    // WELCOME TO WRAP(PER) ZONE

    /**
     * @param filename The filename
     * @return File belonging to corresponding filename
     */
    private File getFile(String filename) {
        return new File(context.getFilesDir(), filename);
    }

    private @Nullable
    String readHtmlFile(Table table) {
        return readHtmlFile(getFile(getFilename(table.getUri(), table.getPublishedDate())));
    }

    private Bitmap readBitmapFile(Table table) {
        return readBitmapFile(getFile(getFilename(table.getUri(), table.getPublishedDate())));
    }

    /**
     * Check whether this exact table file has already been downloaded, using table name and timestamp
     *
     * @param table Table to be checked
     * @return Whether the table has already been downloaded
     * @see #exists(String)
     */
    public boolean exists(Table table) {
        return exists(getFilename(table.getUri(), table.getPublishedDate()));
    }

    public int countFiles() {
        return listFiles().length;
    }

    public long occupiedStorage() {
        long sum = 0;

        for (File file :
                listFiles()) {
            sum += file.length();
        }

        return sum;
    }

    public void deleteAllFiles() {
        for (File file :
                listFiles()) {
            boolean deleted = file.delete();
            Log.d("MANUALDELETE", "could delete file " + file.getName() + "? " + deleted);
        }
    }

    /**
     * @return all files sorted by last order descending
     */
    public File[] getFilesSorted() {
        File[] files = listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.lastModified() < o2.lastModified()) {
                    return 1;
                } else if (o1.lastModified() == o2.lastModified()) {
                    return 0;
                } else if (o1.lastModified() > o2.lastModified()) {
                    return -1;
                }
                // We should never get here but whatever
                return 0;
            }
        });
        return files;
    }

    public String getHtmlTable(Table table, DownloadManager downloadManager) throws IOException {
        // Test whether it really is html
        if (!table.isHtml()) {
            throw new IllegalArgumentException();
        }

        if (exists(table)) {
            return readHtmlFile(table);
        } else {
            return downloadManager.downloadHtmlTable(table, this);
        }
    }

    public Bitmap getImageTable(Table table, DownloadManager downloadManager) throws IOException {
        // Test whether it really is an image
        if (table.getContentType() != CONTENT_IMAGE) {
            throw new IllegalArgumentException();
        }

        if (exists(table)) {
            return readBitmapFile(table);
        } else {
            return downloadManager.downloadImageTable(table, this);
        }
    }

    /**
     * Download table without returning it
     *
     * @param table Table to be downloaded
     * @return <b>Nothing!</b>
     */
    public void getTable(Table table, DownloadManager downloadManager) throws IOException {
        if (table.isHtml()) {
            getHtmlTable(table, downloadManager);
        } else {
            getImageTable(table, downloadManager);
        }
    }
}
