package com.hzw.nested.example;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * author: hzw
 * time: 2019/4/8 下午7:03
 * description:
 */
public class RvAdapter extends RecyclerView.Adapter {

    private List<Entity> list = new ArrayList<>();

    RvAdapter() {
        Entity image = new Entity();
        image.type = Entity.IMAGE;
        list.add(image);

        Entity title = new Entity();
        title.type = Entity.DIVIDER;
        title.title = "相关描述";
        list.add(title);

        for (int i = 0; i < 5; i++) {
            Entity entity = new Entity();
            entity.type = Entity.ABOUT;
            entity.title = String.format("%s. 相关描述：这是一个RecyclerView和WebView的嵌套控件", i + 1);
            list.add(entity);
        }

        Entity title2 = new Entity();
        title2.type = Entity.DIVIDER;
        title2.title = "评论区";
        list.add(title2);

        for (int i = 0; i < 8; i++) {
            Entity entity2 = new Entity();
            entity2.type = Entity.COMMENT;
            entity2.title = String.format("%s. NestedWebViewRecyclerViewGroup一个RecyclerView和WebView的嵌套控件，一般用于文章详情页的结构中，解决RecyclerView和WebView滑动时的无缝衔接！", i + 1);
            list.add(entity2);
        }
    }

    RvAdapter(boolean wrap) {
        for (int i = 0; i < 2; i++) {
            Entity entity2 = new Entity();
            entity2.type = Entity.COMMENT;
            entity2.title = String.format("%s. NestedWebViewRecyclerViewGroup一个RecyclerView和WebView的嵌套控件，一般用于文章详情页的结构中，解决RecyclerView和WebView滑动时的无缝衔接！", i + 1);
            list.add(entity2);
        }
    }

    public void addItem() {
        Entity entity2 = new Entity();
        entity2.type = Entity.COMMENT;
        entity2.title = "NestedWebViewRecyclerViewGroup一个RecyclerView和WebView的嵌套控件，一般用于文章详情页的结构中，解决RecyclerView和WebView滑动时的无缝衔接！xxxx";
        list.add(0, entity2);
        notifyItemInserted(0);
    }

    public void deleteItem() {
        if (list.size() > 0) {
            list.remove(0);
            notifyItemRemoved(0);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        switch (viewType) {
            case Entity.IMAGE:
                return new ImageHolder(inflater.inflate(R.layout.item_image, viewGroup, false));
            case Entity.ABOUT:
                return new AboutHolder(inflater.inflate(R.layout.item_about, viewGroup, false));
            case Entity.COMMENT:
                return new CommentHolder(inflater.inflate(R.layout.item_comment, viewGroup, false));
            case Entity.DIVIDER:
            default:
                return new DividerHolder(inflater.inflate(R.layout.item_divider, viewGroup, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {
        switch (getItemViewType(i)) {
            case Entity.ABOUT:
                AboutHolder aboutHolder = (AboutHolder) holder;
                aboutHolder.title.setText(list.get(i).title);
                break;
            case Entity.COMMENT:
                CommentHolder commentHolder = (CommentHolder) holder;
                commentHolder.comment.setText(list.get(i).title);
                break;
            case Entity.DIVIDER:
                DividerHolder dividerHolder = (DividerHolder) holder;
                dividerHolder.title.setText(list.get(i).title);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).type;
    }

    private static class AboutHolder extends RecyclerView.ViewHolder {

        private TextView title;

        AboutHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.about_title);
        }
    }

    private static class DividerHolder extends RecyclerView.ViewHolder {

        private TextView title;

        DividerHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_text);
        }
    }

    private static class CommentHolder extends RecyclerView.ViewHolder {

        private TextView comment;

        CommentHolder(@NonNull View itemView) {
            super(itemView);
            comment = itemView.findViewById(R.id.comment_text);
        }
    }

    private static class ImageHolder extends RecyclerView.ViewHolder {

        ImageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


}
