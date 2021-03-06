/*
 * Copyright 2016 The twicalico authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.moko256.twicalico.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.github.moko256.twicalico.BuildConfig;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import twitter4j.Trend;

/**
 * Created by moko256 on 2017/07/05.
 *
 * @author moko256
 */

public class CachedTrendsSQLiteOpenHelper extends SQLiteOpenHelper {

    public CachedTrendsSQLiteOpenHelper(Context context, long userId){
        super(context, new File(context.getCacheDir(), String.valueOf(userId) + "/" + "Trends.db").getAbsolutePath(), null, BuildConfig.CACHE_DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table Trends(name);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public synchronized ArrayList<Trend> getTrends(){
        SQLiteDatabase database=getReadableDatabase();
        Cursor c=database.query("Trends", new String[]{"name"}, null, null, null,null,null);
        ArrayList<Trend> trends = new ArrayList<>(c.getCount());

        while (c.moveToNext()){
            trends.add(new CachedTrend(c.getString(0)));
        }

        c.close();
        database.close();

        return trends;
    }

    public synchronized void setTrends(List<Trend> trends){
        SQLiteDatabase database=getWritableDatabase();
        database.beginTransaction();
        database.delete("Trends", null, null);

        for (int i = 0; i < trends.size(); i++) {
            Trend item = trends.get(i);
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", item.getName());

            database.insert("Trends", "", contentValues);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    private static class CachedTrend implements Trend{
        private final String name;

        private CachedTrend(String name){
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getURL() {
            return "http://twitter.com/search?q=" + getQuery();
        }

        @Override
        public String getQuery() {
            try {
                return URLEncoder.encode(name, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj!=null&&obj instanceof Trend&&((Trend) obj).getName().equals(name);
        }
    }
}
