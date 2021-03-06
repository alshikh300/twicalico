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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.moko256.twicalico.model.base.PostTweetModel;
import com.github.moko256.twicalico.model.impl.PostTweetModelImpl;
import com.github.moko256.twicalico.text.TwitterStringUtils;

import java.text.DateFormat;

import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.UserMentionEntity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by moko256 on 2016/03/10.
 * This Activity is to show a tweet.
 *
 * @author moko256
 */
public class ShowTweetActivity extends AppCompatActivity {

    CompositeSubscription subscriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_tweet);

        subscriptions=new CompositeSubscription();

        ActionBar actionBar=getSupportActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white_24dp);
        }

        subscriptions.add(
                Single.create(
                        subscriber -> {
                            long statusId;
                            Status status;
                            statusId = getIntent().getLongExtra("statusId", -1);
                            if (statusId == -1) {
                                ShowTweetActivity.this.finish();
                                return;
                            }
                            status = GlobalApplication.statusCache.get(statusId);
                            if (status == null){
                                try {
                                    status = GlobalApplication.twitter.showStatus(statusId);
                                    GlobalApplication.statusCache.add(status);
                                } catch (TwitterException e) {
                                    subscriber.onError(e);
                                }
                            }
                            subscriber.onSuccess(status);
                        })
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result->{
                                    if (result == null) {
                                        finish();
                                        return;
                                    }
                                    Status item=(Status) result;

                                    TextView tweetIsReply = findViewById(R.id.tweet_show_is_reply_text);
                                    long replyTweetId = item.getInReplyToStatusId();
                                    if (replyTweetId != -1){
                                        tweetIsReply.setVisibility(VISIBLE);
                                        tweetIsReply.setOnClickListener(v -> startActivity(getIntent(this, replyTweetId)));
                                    } else {
                                        tweetIsReply.setVisibility(GONE);
                                    }

                                    StatusView statusView = new StatusView(this);
                                    statusView.setStatus(((Status) result));
                                    ViewGroup cview = (ViewGroup) statusView.getChildAt(0);
                                    ViewGroup sview = (ViewGroup) cview.getChildAt(0);
                                    cview.removeView(sview);
                                    ((FrameLayout) findViewById(R.id.tweet_show_tweet)).addView(sview);

                                    ((TextView)findViewById(R.id.tweet_show_timestamp)).setText(
                                            DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL)
                                                    .format(item.getCreatedAt())
                                    );
                                    TextView viaText= findViewById(R.id.tweet_show_via);
                                    viaText.setText(Html.fromHtml("via:"+item.getSource()));
                                    viaText.setMovementMethod(new LinkMovementMethod());

                                    AppCompatEditText replyText= findViewById(R.id.tweet_show_tweet_reply_text);
                                    AppCompatButton replyButton= findViewById(R.id.tweet_show_tweet_reply_button);
                                    UserMentionEntity[] users = item.getUserMentionEntities();
                                    replyText.setText(TwitterStringUtils.convertToReplyTopString(item.getUser().getScreenName(), users));
                                    replyButton.setOnClickListener(v -> {
                                        replyButton.setEnabled(false);
                                        PostTweetModel model = new PostTweetModelImpl(GlobalApplication.twitter, getContentResolver());
                                        model.setTweetText(replyText.getText().toString());
                                        model.setInReplyToStatusId(item.getId());
                                        subscriptions.add(
                                                model.postTweet()
                                                        .subscribe(
                                                                it -> {
                                                                    replyText.setText(TwitterStringUtils.convertToReplyTopString(item.getUser().getScreenName(), users));
                                                                    replyButton.setEnabled(true);
                                                                    Toast.makeText(ShowTweetActivity.this,R.string.succeeded,Toast.LENGTH_SHORT).show();
                                                                },
                                                                e->{
                                                                    e.printStackTrace();
                                                                    Toast.makeText(ShowTweetActivity.this,R.string.error_occurred,Toast.LENGTH_SHORT).show();
                                                                    replyButton.setEnabled(true);
                                                                }
                                                        )
                                        );
                                    });
                                },
                                e->{
                                    e.printStackTrace();
                                    finish();
                                })
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscriptions.unsubscribe();
        subscriptions=null;
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return false;
    }

    public static Intent getIntent(Context context, long statusId){
        return new Intent(context, ShowTweetActivity.class).putExtra("statusId", statusId);
    }

}
