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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.github.moko256.twicalico.database.TokenSQLiteOpenHelper;

import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

/**
 * Created by moko256 on 2016/04/29.
 *
 * @author moko256
 */
public class OAuthActivity extends AppCompatActivity {
    public RequestToken req;
    public boolean requirePin=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if(requirePin||(uri != null && uri.toString().startsWith(getString(R.string.app_name) + "://OAuthActivity"))){

            if (!requirePin){
                initToken(uri.getQueryParameter("oauth_verifier"));
            }
        }
    }

    private void startUrlAuth(){
        Configuration conf = ConfigurationContext.getInstance();
        final OAuthAuthorization oauth =new OAuthAuthorization(conf);

        oauth.setOAuthConsumer(GlobalApplication.consumerKey,GlobalApplication.consumerSecret);

        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                try {
                    req = oauth.getOAuthRequestToken(getString(R.string.app_name) + "://OAuthActivity");
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void onPostExecute(Void n) {
                startBrowser(req.getAuthorizationURL());
            }
        }.execute();
    }

    private void startPinAuth(){
        Configuration conf = ConfigurationContext.getInstance();
        final OAuthAuthorization oauth =new OAuthAuthorization(conf);
        requirePin=true;
        oauth.setOAuthConsumer(GlobalApplication.consumerKey,GlobalApplication.consumerSecret);

        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                try {
                    req = oauth.getOAuthRequestToken("oob");
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void onPostExecute(Void n) {
                startBrowser(req.getAuthorizationURL());
            }
        }.execute();

        EditText editText=new EditText(this);
        editText.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setView(editText)
                .setPositiveButton(android.R.string.ok,(dialog, which) -> initToken(editText.getText().toString()))
                .setCancelable(false)
                .show();
    }

    private void startBrowser(String url){
        new CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .build()
                .launchUrl(OAuthActivity.this, Uri.parse(url));
    }

    private void initToken(String verifier){
        OAuthAuthorization oauth;

        Configuration conf = ConfigurationContext.getInstance();
        oauth =new OAuthAuthorization(conf);

        Single.create(
                subscriber -> {
                    try {
                        subscriber.onSuccess(oauth.getOAuthAccessToken(req,verifier));
                    } catch (TwitterException e) {
                        subscriber.onError(e);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result-> storeAccessToken(((AccessToken) result)),
                        Throwable::printStackTrace
                );
    }

    private void storeAccessToken(AccessToken accessToken){
        SharedPreferences defaultSharedPreferences= PreferenceManager.getDefaultSharedPreferences(this);

        TokenSQLiteOpenHelper tokenOpenHelper = new TokenSQLiteOpenHelper(this);
        long nowAccountPoint=tokenOpenHelper.addAccessToken(accessToken)-1;
        tokenOpenHelper.close();

        defaultSharedPreferences
                .edit()
                .putString("AccountPoint",String.valueOf(nowAccountPoint))
                .apply();

        ((GlobalApplication) getApplication()).initTwitter(accessToken);

        startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    public void onStartUrlAuthClick(View view) {
        startUrlAuth();
    }

    public void onStartPinAuthClick(View view) {
        startPinAuth();
    }

    public void onSettingClick(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}