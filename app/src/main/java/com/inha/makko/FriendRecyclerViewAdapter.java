package com.inha.makko;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

public class FriendRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<User> friendsList;
    private int[] avatarResourceIdList = {R.drawable.ic_afro_man_male_avatar, R.drawable.ic_boy_avatar, R.drawable.ic_girl_avatar, R.drawable.ic_woman_avatar, R.drawable.ic_hipster_beard_man};

    public class FriendViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView friendAvatar;
        TextView friendName;
        TextView friendAddress;
        TextView lastUpdateAt;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);

            friendAvatar = itemView.findViewById(R.id.image_view_friend);
            friendName = itemView.findViewById(R.id.text_view_friend_name);
            friendAddress = itemView.findViewById(R.id.text_view_address);
            lastUpdateAt = itemView.findViewById(R.id.text_view_last_update_at);
        }
    }

    FriendRecyclerViewAdapter(ArrayList<User> friendsList) {
        this.friendsList = new ArrayList<>();
        this.friendsList.addAll(friendsList);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Random random = new Random();
        int randomIndex = random.nextInt(avatarResourceIdList.length);
        FriendViewHolder friendViewHolder = (FriendViewHolder)holder;
        friendViewHolder.friendAvatar.setImageResource(avatarResourceIdList[randomIndex]);
        friendViewHolder.friendName.setText(friendsList.get(position).name);
        if (friendsList.get(position).roadAddress != null) {
            friendViewHolder.friendAddress.setText(friendsList.get(position).roadAddress);
        } else if (friendsList.get(position).address != null) {
            friendViewHolder.friendAddress.setText(friendsList.get(position).address);
        } else {
            friendViewHolder.friendAddress.setText("친구 주소 얻기 실패");
        }
        if (friendsList.get(position).lastUpdateAt != null) {
            String lastUpdateAt = new DateTime(friendsList.get(position).lastUpdateAt).toString("yyyy-MM-dd hh:mm:ss");
            friendViewHolder.lastUpdateAt.setText(lastUpdateAt);
        }
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public void addItem(User friend) {
        friendsList.add(friend);
        notifyItemInserted(friendsList.size() -1);
    }

    public void refreshList(ArrayList<User> friendsList) {
        this.friendsList.clear();
        this.friendsList.addAll(friendsList);
        notifyDataSetChanged();
    }
}
