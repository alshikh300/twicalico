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

package com.github.moko256.twicalico.text;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import com.github.moko256.twicalico.R;
import com.github.moko256.twicalico.SearchResultActivity;
import com.github.moko256.twicalico.ShowUserActivity;

import java.text.Normalizer;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.SymbolEntity;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * Created by moko256 on 2016/08/06.
 *
 * @author moko256
 */

public class TwitterStringUtils {

    @NonNull
    public static String plusAtMark(String string){
        return "@" + string;
    }

    public static String removeHtmlTags(String html){
        return Normalizer.normalize(html, Normalizer.Form.NFC).replaceAll("<.+?>", "");
    }

    public static String convertToSIUnitString(int num){
        if (num == 0) return "0";
        boolean isNegative = (num < 0);
        if (isNegative) num *= -1;

        float k = num / 1000;
        if (k < 1) return (isNegative? "-": "") + String.valueOf(num);

        float m = k / 1000;
        if (m < 1) return (isNegative? "-": "") + String.valueOf(Math.round(k)) + "K";

        float g = m / 1000;
        if (g < 1) return (isNegative? "-": "") + String.valueOf(Math.round(m)) + "M";

        return (isNegative? "-": "") + String.valueOf(Math.round(g)) + "G";
    }


    public static CharSequence convertToReplyTopString(String userScreenName, UserMentionEntity[] users){
        StringBuilder userIdsStr = new StringBuilder();
        userIdsStr.append("@").append(userScreenName).append(" ");
        for (UserMentionEntity user : users) {
            userIdsStr.append("@").append(user.getScreenName()).append(" ");
        }
        return userIdsStr;
    }

    public static String convertErrorToText(Throwable e){
        if (e instanceof TwitterException && !TextUtils.isEmpty(((TwitterException) e).getErrorMessage())){
            return ((TwitterException) e).getErrorMessage();
        } else {
            return e.getMessage();
        }
    }

    public static CharSequence getStatusTextSequence(Status item){

        String tweet = item.getText();
        StringBuilder builder = new StringBuilder(tweet);

        URLEntity[] urlEntities = item.getURLEntities();

        int tweetLength = tweet.codePointCount(0, tweet.length());
        int sp = 0;

        for (URLEntity entity : urlEntities) {
            String url = entity.getURL();
            String displayUrl = entity.getDisplayURL();

            int urlLength = url.codePointCount(0, url.length());
            int displayUrlLength = displayUrl.codePointCount(0, displayUrl.length());
            if (entity.getStart() <= tweetLength && entity.getEnd() <= tweetLength) {
                int dusp = displayUrlLength - urlLength;
                builder.replace(tweet.offsetByCodePoints(0,entity.getStart()) + sp, tweet.offsetByCodePoints(0,entity.getEnd()) + sp, displayUrl);

                sp+=dusp;
            }
        }

        MediaEntity[] mediaEntities = item.getMediaEntities();
        if (mediaEntities.length > 0){
            MediaEntity mediaEntity = mediaEntities[0];
            int result = builder.indexOf(mediaEntity.getURL(), builder.offsetByCodePoints(0, mediaEntity.getStart()));
            if (result != -1){
                builder.replace(result, result + mediaEntity.getURL().length(), "");
            }
        }

        return builder;
    }

    public static CharSequence getLinkedSequence(Status item, Context mContext){

        String tweet = item.getText();

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(tweet);

        for (SymbolEntity symbolEntity : item.getSymbolEntities()) {
            spannableStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    mContext.startActivity(SearchResultActivity.getIntent(mContext, symbolEntity.getText()));
                }
            }, tweet.offsetByCodePoints(0,symbolEntity.getStart()), tweet.offsetByCodePoints(0,symbolEntity.getEnd()), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        for (HashtagEntity hashtagEntity : item.getHashtagEntities()) {
            spannableStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    mContext.startActivity(new Intent(mContext,SearchResultActivity.class)
                            .setAction(Intent.ACTION_SEARCH)
                            .putExtra(SearchManager.QUERY,"#"+hashtagEntity.getText())
                    );
                }
            }, tweet.offsetByCodePoints(0,hashtagEntity.getStart()), tweet.offsetByCodePoints(0,hashtagEntity.getEnd()), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        for (UserMentionEntity userMentionEntity : item.getUserMentionEntities()) {
            spannableStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    mContext.startActivity(
                            ShowUserActivity.getIntent(mContext, userMentionEntity.getScreenName())
                    );
                }
            }, tweet.offsetByCodePoints(0,userMentionEntity.getStart()), tweet.offsetByCodePoints(0,userMentionEntity.getEnd()), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        URLEntity[] urlEntities = item.getURLEntities();

        int tweetLength = tweet.codePointCount(0, tweet.length());
        int sp = 0;

        for (URLEntity entity : urlEntities) {
            String url = entity.getURL();
            String expandedUrl = entity.getExpandedURL();

            int urlLength = url.codePointCount(0, url.length());
            int displayUrlLength = expandedUrl.codePointCount(0, expandedUrl.length());
            if (entity.getStart() <= tweetLength && entity.getEnd() <= tweetLength) {
                int dusp = displayUrlLength - urlLength;
                spannableStringBuilder.replace(tweet.offsetByCodePoints(0, entity.getStart()) + sp, tweet.offsetByCodePoints(0, entity.getEnd()) + sp, expandedUrl);
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        new CustomTabsIntent.Builder()
                                .setToolbarColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                                .setSecondaryToolbarColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark))
                                .build()
                                .launchUrl(mContext, Uri.parse(expandedUrl));
                    }
                }, tweet.offsetByCodePoints(0, entity.getStart()) + sp, tweet.offsetByCodePoints(0, entity.getEnd()) + sp + dusp, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                sp += dusp;
            }
        }

        MediaEntity[] mediaEntities = item.getMediaEntities();
        if (mediaEntities.length > 0){
            MediaEntity mediaEntity = mediaEntities[0];
            String text = spannableStringBuilder.toString();
            int result = text.indexOf(mediaEntity.getURL(), text.offsetByCodePoints(0, mediaEntity.getStart()));
            if (result != -1){
                spannableStringBuilder.replace(result, result + mediaEntity.getURL().length(), "");
            }
        }

        return spannableStringBuilder;

    }

    public static CharSequence getProfileLinkedSequence(Context mContext, User user){
        String description = user.getDescription();
        URLEntity[] urlEntities = user.getDescriptionURLEntities();

        if (urlEntities == null || urlEntities.length <= 0 || TextUtils.isEmpty(urlEntities[0].getURL())) return description;

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(description);
        int tweetLength = description.codePointCount(0, description.length());
        int sp = 0;

        for (URLEntity entity : urlEntities) {
            String url = entity.getURL();
            String expandedUrl = entity.getDisplayURL();

            int urlLength = url.codePointCount(0, url.length());
            int displayUrlLength = expandedUrl.codePointCount(0, expandedUrl.length());
            if (entity.getStart() <= tweetLength && entity.getEnd() <= tweetLength) {
                int dusp = displayUrlLength - urlLength;
                spannableStringBuilder.replace(description.offsetByCodePoints(0, entity.getStart()) + sp, description.offsetByCodePoints(0, entity.getEnd()) + sp, expandedUrl);
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        new CustomTabsIntent.Builder()
                                .setToolbarColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                                .setSecondaryToolbarColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark))
                                .build()
                                .launchUrl(mContext, Uri.parse(entity.getExpandedURL()));
                    }
                }, description.offsetByCodePoints(0, entity.getStart()) + sp, description.offsetByCodePoints(0, entity.getEnd()) + sp + dusp, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                sp += dusp;
            }
        }

        return spannableStringBuilder;
    }

}
