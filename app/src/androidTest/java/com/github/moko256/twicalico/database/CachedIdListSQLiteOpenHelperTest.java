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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by moko256 on 2017/06/08.
 *
 * @author moko256
 */
@RunWith(AndroidJUnit4.class)
public class CachedIdListSQLiteOpenHelperTest {

    private CachedIdListSQLiteOpenHelper helper = new CachedIdListSQLiteOpenHelper(InstrumentationRegistry.getTargetContext(), 0, "testIdsDatabase");

    private long[] addInput = new long[]{0,1,2};
    private long[] insertInput = new long[]{100, 101};

    @Test
    public void test() throws Exception{
        try {
            helper.getWritableDatabase().execSQL("delete from testIdsDatabase;");
        } catch (Exception e) {
            //Do nothing
        }

        addIdTest();
        insertIdTest();
        hasIdOtherTableTest();
        deleteIdTest();
        setListViewPositionTest();
        helper.close();
    }


    private void addIdTest(){
        helper.addIds(addInput);

        ArrayList<Long> result1 = helper.getIds();
        for (int i = 0; i < result1.size() ; i++) {
            assertEquals(result1.get(i), Long.valueOf(addInput[i]));
        }
    }

    private void insertIdTest(){
        helper.insertIds(1, insertInput);

        ArrayList<Long> result2 = helper.getIds();
        for (int i = 0; i < insertInput.length; i++) {
            assertEquals(Long.valueOf(insertInput[i]), result2.get(i + 1));
        }
    }

    private void hasIdOtherTableTest(){
        CachedIdListSQLiteOpenHelper helper2 = new CachedIdListSQLiteOpenHelper(InstrumentationRegistry.getTargetContext(), 0, "testIdDatabase2");
        boolean[] result = helper2.hasIdsOtherTable(new long[]{100, 105});
        assertTrue(result[0]);
        assertFalse(result[1]);
    }

    private void deleteIdTest(){
        helper.deleteIds(addInput);
        helper.deleteIds(insertInput);

        assertEquals(helper.getIds().size(), 0);
    }

    private void setListViewPositionTest(){
        helper.setListViewPosition(100);
        assertEquals(helper.getListViewPosition(), 100);

        helper.setListViewPosition(50);
        assertEquals(helper.getListViewPosition(), 50);
    }
}
