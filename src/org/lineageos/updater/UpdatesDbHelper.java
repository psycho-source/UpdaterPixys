/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2019 The PixysOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.updater;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.lineageos.updater.model.Update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UpdatesDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "updates.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UpdateEntry.TABLE_NAME + " (" +
                    UpdateEntry._ID + " INTEGER PRIMARY KEY," +
                    UpdateEntry.COLUMN_NAME_STATUS + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_PATH + " TEXT," +
                    UpdateEntry.COLUMN_NAME_DOWNLOAD_ID + " TEXT NOT NULL UNIQUE," +
                    UpdateEntry.COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_TYPE + " TEXT," +
                    UpdateEntry.COLUMN_NAME_VERSION + " TEXT," +
                    UpdateEntry.COLUMN_NAME_SIZE + " INTEGER)";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + UpdateEntry.TABLE_NAME;

    public UpdatesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long addUpdate(Update update) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_STATUS, update.getPersistentStatus());
        values.put(UpdateEntry.COLUMN_NAME_PATH, update.getFile().getAbsolutePath());
        values.put(UpdateEntry.COLUMN_NAME_DOWNLOAD_ID, update.getDownloadId());
        values.put(UpdateEntry.COLUMN_NAME_TIMESTAMP, update.getTimestamp());
        values.put(UpdateEntry.COLUMN_NAME_TYPE, update.getType());
        values.put(UpdateEntry.COLUMN_NAME_VERSION, update.getVersion());
        values.put(UpdateEntry.COLUMN_NAME_SIZE, update.getFileSize());
        return db.insert(UpdateEntry.TABLE_NAME, null, values);
    }

    public void addUpdateWithOnConflict(Update update, int conflictAlgorithm) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_STATUS, update.getPersistentStatus());
        values.put(UpdateEntry.COLUMN_NAME_PATH, update.getFile().getAbsolutePath());
        values.put(UpdateEntry.COLUMN_NAME_DOWNLOAD_ID, update.getDownloadId());
        values.put(UpdateEntry.COLUMN_NAME_TIMESTAMP, update.getTimestamp());
        values.put(UpdateEntry.COLUMN_NAME_TYPE, update.getType());
        values.put(UpdateEntry.COLUMN_NAME_VERSION, update.getVersion());
        values.put(UpdateEntry.COLUMN_NAME_SIZE, update.getFileSize());
        db.insertWithOnConflict(UpdateEntry.TABLE_NAME, null, values, conflictAlgorithm);
    }

    public void removeUpdate(String downloadId) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = UpdateEntry.COLUMN_NAME_DOWNLOAD_ID + " = ?";
        String[] selectionArgs = {downloadId};
        db.delete(UpdateEntry.TABLE_NAME, selection, selectionArgs);
    }

    public boolean removeUpdate(long rowId) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = UpdateEntry._ID + " = " + rowId;
        return db.delete(UpdateEntry.TABLE_NAME, selection, null) != 0;
    }

    public void changeUpdateStatus(Update update) {
        String selection = UpdateEntry.COLUMN_NAME_DOWNLOAD_ID + " = ?";
        String[] selectionArgs = {update.getDownloadId()};
        changeUpdateStatus(selection, selectionArgs, update.getPersistentStatus());
    }

    public boolean changeUpdateStatus(long rowId, int status) {
        String selection = UpdateEntry._ID + " = " + rowId;
        return changeUpdateStatus(selection, null, status);
    }

    private boolean changeUpdateStatus(String selection, String[] selectionArgs,
                                       int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_STATUS, status);
        return db.update(UpdateEntry.TABLE_NAME, values, selection, selectionArgs) != 0;
    }

    public Update getUpdate(long rowId) {
        String selection = UpdateEntry._ID + " = " + rowId;
        return getUpdate(selection, null);
    }

    public Update getUpdate(String downloadId) {
        String selection = UpdateEntry.COLUMN_NAME_DOWNLOAD_ID + " = ?";
        String[] selectionArgs = {downloadId};
        return getUpdate(selection, selectionArgs);
    }

    private Update getUpdate(String selection, String[] selectionArgs) {
        List<Update> updates = getUpdates(selection, selectionArgs);
        return updates != null ? updates.get(0) : null;
    }

    public List<Update> getUpdates() {
        return getUpdates(null, null);
    }

    private List<Update> getUpdates(String selection, String[] selectionArgs) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                UpdateEntry.COLUMN_NAME_PATH,
                UpdateEntry.COLUMN_NAME_DOWNLOAD_ID,
                UpdateEntry.COLUMN_NAME_TIMESTAMP,
                UpdateEntry.COLUMN_NAME_TYPE,
                UpdateEntry.COLUMN_NAME_VERSION,
                UpdateEntry.COLUMN_NAME_STATUS,
                UpdateEntry.COLUMN_NAME_SIZE,
        };
        String sort = UpdateEntry.COLUMN_NAME_TIMESTAMP + " DESC";
        Cursor cursor = db.query(UpdateEntry.TABLE_NAME, projection, selection, selectionArgs,
                null, null, sort);
        List<Update> updates = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Update update = new Update();
                int index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_PATH);
                update.setFile(new File(cursor.getString(index)));
                update.setName(update.getFile().getName());
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_DOWNLOAD_ID);
                update.setDownloadId(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_TIMESTAMP);
                update.setTimestamp(cursor.getLong(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_TYPE);
                update.setType(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_VERSION);
                update.setVersion(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_STATUS);
                update.setPersistentStatus(cursor.getInt(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_SIZE);
                update.setFileSize(cursor.getLong(index));
                updates.add(update);
            }
            cursor.close();
        }
        return updates;
    }

    static class UpdateEntry implements BaseColumns {
        static final String TABLE_NAME = "updates";
        static final String COLUMN_NAME_STATUS = "status";
        static final String COLUMN_NAME_PATH = "path";
        static final String COLUMN_NAME_DOWNLOAD_ID = "download_id";
        static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        static final String COLUMN_NAME_TYPE = "type";
        static final String COLUMN_NAME_VERSION = "version";
        static final String COLUMN_NAME_SIZE = "size";
    }
}
