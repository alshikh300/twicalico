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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.moko256.twicalico.text.TwitterStringUtils;

import java.util.ArrayList;

import twitter4j.User;

/**
 * Created by moko256 on 2016/03/29.
 *
 * @author moko256
 */
class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private ArrayList<Long> data;
    private Context context;

    private GlideRequests glideRequests;

    UsersAdapter(Context context, ArrayList<Long> data) {
        this.context = context;
        this.data = data;
        glideRequests = GlideApp.with(context);

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position);
    }

    @Override
    public UsersAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_user, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        User item=GlobalApplication.userCache.get(data.get(i));

        glideRequests.asBitmap().load(item.getProfileImageURLHttps()).circleCrop().into(viewHolder.userUserImage);

        viewHolder.userUserName.setText(item.getName());
        viewHolder.userUserId.setText(TwitterStringUtils.plusAtMark(item.getScreenName()));
        viewHolder.itemView.setOnClickListener(v -> {
            context.startActivity(ShowUserActivity.getIntent(context, item.getId()));
        });

    }

    @Override
    public int getItemCount() {
        if (data != null) {
            return data.size();
        } else {
            return 0;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView userUserImage;
        TextView userUserName;
        TextView userUserId;

        ViewHolder(final View itemView) {
            super(itemView);
            userUserImage= itemView.findViewById(R.id.user_user_image);
            userUserId= itemView.findViewById(R.id.user_user_id);
            userUserName= itemView.findViewById(R.id.user_user_name);
        }
    }
}

