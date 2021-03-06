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

package com.github.moko256.twicalico;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.moko256.twicalico.database.CachedIdListSQLiteOpenHelper;
import com.github.moko256.twicalico.text.TwitterStringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * Created by moko256 on 2016/03/27.
 *
 * @author moko256
 */
public abstract class BaseTweetListFragment extends BaseListFragment {

    StatusesAdapter adapter;
    ArrayList<Long> list;

    CompositeSubscription subscription;

    CachedIdListSQLiteOpenHelper statusIdsDatabase;

    final static int PARAM_PAGING_COUNT = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        list=new ArrayList<>();
        subscription = new CompositeSubscription();
        statusIdsDatabase = new CachedIdListSQLiteOpenHelper(getContext(), GlobalApplication.userId, getCachedIdsDatabaseName());
        if (savedInstanceState == null){
            ArrayList<Long> c = statusIdsDatabase.getIds();
            if (c.size() > 0) {
                list.addAll(c);
                setProgressCircleLoading(false);
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=super.onCreateView(inflater, container, savedInstanceState);

        getRecyclerView().addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                int span = ((StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
                float dens = getContext().getResources().getDisplayMetrics().density;

                outRect.left = Math.round(dens * (span == 0 ? 8f : 4f));
                outRect.right = Math.round(dens * (span == ((StaggeredGridLayoutManager) parent.getLayoutManager()).getSpanCount() - 1 ? 8f : 4f));
                outRect.top = Math.round(dens * 8f);
            }
        });

        adapter=new StatusesAdapter(getContext(), list);
        adapter.setOnLoadMoreClick(position -> subscription.add(
                getResponseSingle(
                        new Paging()
                                .maxId(list.get(position-1)-1L)
                                .sinceId(list.get(list.size() >= position + 2? position + 2: position + 1))
                                .count(PARAM_PAGING_COUNT))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    if (result.size() > 0) {
                                        list.remove(position);
                                        statusIdsDatabase.deleteIds(new long[]{-1L});
                                        adapter.notifyItemRemoved(position);
                                        List<Long>ids = Observable
                                                .from(result)
                                                .map(Status::getId)
                                                .toList().toSingle().toBlocking().value();
                                        if (ids.get(ids.size() - 1).equals(list.get(position))) {
                                            ids.remove(ids.size() - 1);
                                        } else {
                                            ids.add(-1L);
                                        }
                                        list.addAll(position, ids);
                                        statusIdsDatabase.insertIds(position, ids);
                                        adapter.notifyItemRangeInserted(position, ids.size());
                                    } else {
                                        list.remove(position);
                                        statusIdsDatabase.deleteIds(new long[]{-1L});
                                        adapter.notifyItemRemoved(position);
                                    }
                                },
                                e -> {
                                    e.printStackTrace();
                                    Snackbar.make(getSnackBarParentContainer(), TwitterStringUtils.convertErrorToText(e), Snackbar.LENGTH_INDEFINITE)
                                            .setAction(R.string.retry, v -> onLoadMoreList())
                                            .show();
                                }
                        )
        ));
        setAdapter(adapter);
        if (!isInitializedList()){
            adapter.notifyDataSetChanged();
        }

        getRecyclerView().getLayoutManager().scrollToPosition(statusIdsDatabase.getListViewPosition());

        return view;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null){
            ArrayList l=(ArrayList) savedInstanceState.getSerializable("list");
            if(l!=null){
                Long longs[] = new Long[l.size()];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = (Long)l.get(i);
                }
                list.addAll(Arrays.asList(longs));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putSerializable("list", list);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscription.unsubscribe();
        adapter=null;
    }

    @Override
    public void onStop() {
        super.onStop();
        StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getRecyclerView().getLayoutManager();
        int[] positions = layoutManager.findFirstVisibleItemPositions(null);
        statusIdsDatabase.setListViewPosition(positions[0]);
        ArrayList<Long> ids = statusIdsDatabase.getIds();
        if (ids.size() - positions[0] > 1000){
            List<Long> list = ids.subList(positions[0] + 1000, ids.size());
            statusIdsDatabase.deleteIds(list);
            boolean[] results = statusIdsDatabase.hasIdsOtherTable(list);
            List<Long> deletableIds = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                if (!results[i]) {
                    deletableIds.add(list.get(i));
                }
            }
            GlobalApplication.statusCache.delete(deletableIds);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subscription = null;
        list=null;
    }

    @Override
    protected void onInitializeList() {
        subscription.add(
                getResponseSingle(new Paging(1,20))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result-> {
                                    List<Long> ids = Observable
                                            .from(result)
                                            .map(Status::getId)
                                            .toList().toSingle().toBlocking().value();
                                    list.addAll(ids);
                                    statusIdsDatabase.addIds(ids);
                                    adapter.notifyDataSetChanged();
                                    getSwipeRefreshLayout().setRefreshing(false);
                                    setProgressCircleLoading(false);
                                },
                                e -> {
                                    e.printStackTrace();
                                    Snackbar.make(getSnackBarParentContainer(), TwitterStringUtils.convertErrorToText(e), Snackbar.LENGTH_INDEFINITE)
                                            .setAction(R.string.retry, v -> onInitializeList())
                                            .show();
                                }
                        )
        );
    }

    @Override
    protected void onUpdateList() {
        subscription.add(
                getResponseSingle(new Paging(list.get(list.size() >= 2? 1: 0)).count(PARAM_PAGING_COUNT))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    if (result.size() > 0) {
                                        List<Long> ids = Observable
                                                .from(result)
                                                .map(Status::getId)
                                                .toList().toSingle().toBlocking().value();
                                        if (ids.get(ids.size() - 1).equals(list.get(0))) {
                                            ids.remove(ids.size() - 1);
                                        } else {
                                            ids.add(-1L);
                                        }

                                        if (ids.size() > 0){
                                            list.addAll(0, ids);
                                            statusIdsDatabase.insertIds(0, ids);
                                            adapter.notifyItemRangeInserted(0, ids.size());
                                            TypedValue value=new TypedValue();
                                            Toast t=Toast.makeText(getContext(),"New Tweet",Toast.LENGTH_SHORT);
                                            t.setGravity(
                                                    Gravity.TOP|Gravity.CENTER,
                                                    0,
                                                    getContext().getTheme().resolveAttribute(R.attr.actionBarSize, value, true)?
                                                            TypedValue.complexToDimensionPixelOffset(value.data, getResources().getDisplayMetrics()):
                                                            0
                                            );
                                            t.show();
                                        }
                                    }
                                    getSwipeRefreshLayout().setRefreshing(false);
                                },
                                e -> {
                                    e.printStackTrace();
                                    getSwipeRefreshLayout().setRefreshing(false);
                                    Snackbar.make(getSnackBarParentContainer(), TwitterStringUtils.convertErrorToText(e), Snackbar.LENGTH_INDEFINITE)
                                            .setAction(R.string.retry, v -> onUpdateList())
                                            .show();
                                }
                        )
        );
    }

    @Override
    protected void onLoadMoreList() {
        subscription.add(
                getResponseSingle(new Paging().maxId(list.get(list.size()-1)-1L).count(PARAM_PAGING_COUNT))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    int size = result.size();
                                    if (size > 0) {
                                        List<Long> ids = Observable
                                                .from(result)
                                                .map(Status::getId)
                                                .toList().toSingle().toBlocking().value();
                                        list.addAll(ids);
                                        statusIdsDatabase.insertIds(list.size() - size, ids);
                                        adapter.notifyItemRangeInserted(list.size() - size, size);
                                    }
                                },
                                e -> {
                                    e.printStackTrace();
                                    Snackbar.make(getSnackBarParentContainer(), TwitterStringUtils.convertErrorToText(e), Snackbar.LENGTH_INDEFINITE)
                                            .setAction(R.string.retry, v -> onLoadMoreList())
                                            .show();
                                }
                        )
        );
    }

    @Override
    protected boolean isInitializedList() {
        return !list.isEmpty();
    }

    @Override
    protected RecyclerView.LayoutManager initializeRecyclerViewLayoutManager() {
            return new StaggeredGridLayoutManager(
                    (int) Math.ceil(getContext().getResources().getConfiguration().smallestScreenWidthDp/300),
                    StaggeredGridLayoutManager.VERTICAL
            );
    }

    protected abstract String getCachedIdsDatabaseName();

    public Single<ResponseList<Status>> getResponseSingle(Paging paging) {
        return Single.create(
                subscriber->{
                    try {
                        ResponseList<Status> statuses = getResponseList(paging);
                        if (statuses.size() > 0){
                            GlobalApplication.statusCache.addAll(statuses);
                        }
                        subscriber.onSuccess(statuses);
                    } catch (TwitterException e) {
                        subscriber.onError(e);
                    }
                }
        );
    }

    public abstract ResponseList<Status> getResponseList(Paging paging) throws TwitterException;

}