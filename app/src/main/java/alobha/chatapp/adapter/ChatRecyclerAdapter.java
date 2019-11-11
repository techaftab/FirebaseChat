package alobha.chatapp.adapter;

import android.content.Intent;
import android.net.Uri;

import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import alobha.chatapp.R;
import alobha.chatapp.activity.FullScreenImageActivity;
import alobha.chatapp.model.Chat;


public class ChatRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private static final int VIEW_TYPE_ME_IMAGE = 3;
    private static final int VIEW_TYPE_OTHER_IMAGE = 4;

    private List<Chat> mChats;

    public ChatRecyclerAdapter(List<Chat> chats) {
        mChats = chats;
    }

    public void add(Chat chat) {
        mChats.add(chat);
        notifyItemInserted(mChats.size() - 1);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case VIEW_TYPE_ME:
                View viewChatMine = layoutInflater.inflate(R.layout.item_chat_mine, parent, false);
                viewHolder = new MyChatViewHolder(viewChatMine);
                break;
            case VIEW_TYPE_OTHER:
                View viewChatOther = layoutInflater.inflate(R.layout.item_chat_other, parent, false);
                viewHolder = new OtherChatViewHolder(viewChatOther);
                break;
            case VIEW_TYPE_ME_IMAGE:
                View viewMineImageChat = layoutInflater.inflate(R.layout.item_message_right_img, parent, false);
                viewHolder = new MyImageChatViewHolder(viewMineImageChat);
                break;
            case VIEW_TYPE_OTHER_IMAGE:
                View viewOtherImageChat = layoutInflater.inflate(R.layout.item_message_left_img, parent, false);
                viewHolder = new OtherImageChatViewHolder(viewOtherImageChat);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (TextUtils.equals(mChats.get(position).senderUid, FirebaseAuth.getInstance().getCurrentUser().getUid())
                && mChats.get(position).message.equalsIgnoreCase("")) {
            if (mChats.get(position).getMapModel() != null) {
                configureMyImageChatViewHolder((MyImageChatViewHolder) holder, position);
            }

            configureMyImageChatViewHolder((MyImageChatViewHolder) holder, position);

        } else if (!mChats.get(position).senderUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())
                && mChats.get(position).message.equalsIgnoreCase("")) {
            if (mChats.get(position).getMapModel() != null) {
                configureOtherImageChatViewHolder((OtherImageChatViewHolder) holder, position);
            }

            configureOtherImageChatViewHolder((OtherImageChatViewHolder) holder, position);
        } else if (TextUtils.equals(mChats.get(position).senderUid, FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            configureMyChatViewHolder((MyChatViewHolder) holder, position);
        } else {
            configureOtherChatViewHolder((OtherChatViewHolder) holder, position);
        }
    }

    private void configureMyChatViewHolder(MyChatViewHolder myChatViewHolder, int position) {
        Chat chat = mChats.get(position);

        String alphabet = chat.sender.substring(0, 1);

        myChatViewHolder.txtChatMessage.setText(chat.message);
        myChatViewHolder.txtUserAlphabet.setText(alphabet);
    }

    private void configureOtherChatViewHolder(OtherChatViewHolder otherChatViewHolder, int position) {
        Chat chat = mChats.get(position);

        String alphabet = chat.sender.substring(0, 1);

        otherChatViewHolder.txtChatMessage.setText(chat.message);
        otherChatViewHolder.txtUserAlphabet.setText(alphabet);
    }

    private void configureMyImageChatViewHolder(final MyImageChatViewHolder myImageChatViewHolder, int position) {
        final Chat chat = mChats.get(position);
        String alphabet = chat.sender.substring(0, 1);


        if (chat.getMapModel() != null) {
            myImageChatViewHolder.imgChatMessage.setVisibility(View.GONE);
            myImageChatViewHolder.place_chat_message.setVisibility(View.VISIBLE);
            myImageChatViewHolder.place_chat_message.setText(mChats.get(position).getMapModel().getPlace_name());
            myImageChatViewHolder.txtUserAlphabet.setText(alphabet);
            myImageChatViewHolder.place_chat_message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String latitude = chat.getMapModel().getLatitude();
                    String longitude = chat.getMapModel().getLongitude();
                    String uri = String.format("geo:%s,%s?z=17&q=%s,%s", latitude, longitude, latitude, longitude);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    myImageChatViewHolder.place_chat_message.getContext().startActivity(intent);
                }
            });
        } else {


            myImageChatViewHolder.imgChatMessage.setVisibility(View.VISIBLE);
            myImageChatViewHolder.place_chat_message.setVisibility(View.GONE);

            Log.e("Image 137","Adapter-"+chat.getFile().getUrl_file());




            Glide.with(myImageChatViewHolder.imgChatMessage.getContext()).load(chat.getFile().getUrl_file())
                    .override(100,100)
                    .into(myImageChatViewHolder.imgChatMessage);


            myImageChatViewHolder.txtUserAlphabet.setText(alphabet);
            myImageChatViewHolder.imgChatMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nameUser = chat.sender;
                    String urlPhotoUser = chat.getFile().getUrl_file();
                    String urlPhotoClick = chat.getFile().getUrl_file();
                    Intent intent = new Intent(myImageChatViewHolder.imgChatMessage.getContext(), FullScreenImageActivity.class);
                    intent.putExtra("nameUser", nameUser);
                    intent.putExtra("urlPhotoUser", urlPhotoUser);
                    intent.putExtra("urlPhotoClick", urlPhotoClick);
                    myImageChatViewHolder.imgChatMessage.getContext().startActivity(intent);
                }
            });
        }


    }

    private void configureOtherImageChatViewHolder(final OtherImageChatViewHolder otherImageChatViewHolder, int position) {
        final Chat chat = mChats.get(position);
        String alphabet = chat.sender.substring(0, 1);

        if (chat.getMapModel() != null) {
            otherImageChatViewHolder.imgChatMessage.setVisibility(View.GONE);
            otherImageChatViewHolder.place_chat_message.setVisibility(View.VISIBLE);
            otherImageChatViewHolder.place_chat_message.setText(mChats.get(position).getMapModel().getPlace_name());
            otherImageChatViewHolder.txtChatMessage.setText(alphabet);
            otherImageChatViewHolder.place_chat_message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String latitude = chat.getMapModel().getLatitude();
                    String longitude = chat.getMapModel().getLongitude();
                    String uri = String.format("geo:%s,%s?z=17&q=%s,%s", latitude, longitude, latitude, longitude);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    otherImageChatViewHolder.place_chat_message.getContext().startActivity(intent);
                }
            });
        } else {
            otherImageChatViewHolder.imgChatMessage.setVisibility(View.VISIBLE);
            otherImageChatViewHolder.place_chat_message.setVisibility(View.GONE);

            Log.e("Image 184","Adapter-"+chat.getFile().getUrl_file());

            Glide.with(otherImageChatViewHolder.imgChatMessage.getContext())
                    .load(chat.getFile().getUrl_file())
                    .override(100,100)
                    .into(otherImageChatViewHolder.imgChatMessage);



            otherImageChatViewHolder.txtChatMessage.setText(alphabet);
            otherImageChatViewHolder.imgChatMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nameUser = chat.sender;
                    String urlPhotoUser = chat.getFile().getUrl_file();
                    String urlPhotoClick = chat.getFile().getUrl_file();
                    Intent intent = new Intent(otherImageChatViewHolder.imgChatMessage.getContext(), FullScreenImageActivity.class);
                    intent.putExtra("nameUser", nameUser);
                    intent.putExtra("urlPhotoUser", urlPhotoUser);
                    intent.putExtra("urlPhotoClick", urlPhotoClick);
                    otherImageChatViewHolder.imgChatMessage.getContext().startActivity(intent);
                }
            });
        }

    }


    @Override
    public int getItemCount() {
        if (mChats != null) {
            return mChats.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {

        if (TextUtils.equals(mChats.get(position).senderUid, FirebaseAuth.getInstance().getCurrentUser().getUid())
                && mChats.get(position).message.equalsIgnoreCase("")) {
            return VIEW_TYPE_ME_IMAGE;
        } else if (!mChats.get(position).senderUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())
                && mChats.get(position).message.equalsIgnoreCase("")) {
            return VIEW_TYPE_OTHER_IMAGE;
        } else if (TextUtils.equals(mChats.get(position).senderUid, FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }

    }

    private static class MyChatViewHolder extends RecyclerView.ViewHolder {
        private TextView txtChatMessage, txtUserAlphabet;

        public MyChatViewHolder(View itemView) {
            super(itemView);
            txtChatMessage = (TextView) itemView.findViewById(R.id.text_view_chat_message);
            txtUserAlphabet = (TextView) itemView.findViewById(R.id.text_view_user_alphabet);
        }
    }

    private static class OtherChatViewHolder extends RecyclerView.ViewHolder {
        private TextView txtChatMessage, txtUserAlphabet;

        public OtherChatViewHolder(View itemView) {
            super(itemView);
            txtChatMessage = (TextView) itemView.findViewById(R.id.text_view_chat_message);
            txtUserAlphabet = (TextView) itemView.findViewById(R.id.text_view_user_alphabet);
        }
    }

    private static class MyImageChatViewHolder extends RecyclerView.ViewHolder {
        private TextView txtUserAlphabet, place_chat_message;
        private ImageView imgChatMessage;

        public MyImageChatViewHolder(View itemView) {
            super(itemView);
            imgChatMessage = (ImageView) itemView.findViewById(R.id.image_view_chat_message);
            txtUserAlphabet = (TextView) itemView.findViewById(R.id.text_view_user_alphabet);
            place_chat_message = (TextView) itemView.findViewById(R.id.place_chat_message);
        }
    }

    private static class OtherImageChatViewHolder extends RecyclerView.ViewHolder {
        private TextView txtChatMessage, place_chat_message;
        private ImageView imgChatMessage;

        public OtherImageChatViewHolder(View itemView) {
            super(itemView);
            txtChatMessage = (TextView) itemView.findViewById(R.id.text_view_user_alphabet);
            place_chat_message = (TextView) itemView.findViewById(R.id.place_chat_message);
            imgChatMessage = (ImageView) itemView.findViewById(R.id.iamge_view_chat_message);
        }
    }

}
