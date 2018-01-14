package com.weixin.location.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.amap.api.services.core.PoiItem;
import com.weixin.location.R;
import com.weixin.location.utils.OnItemClickLisenter;

import java.util.List;

/**
 * 搜索地址的适配器
 * Created by XiaoFu on 2018-01-11 15:00.
 * 注释：
 */

public class SearchAddressAdapter extends RecyclerView.Adapter<SearchAddressAdapter.MyHolder> {
    private Context mContext;
    private List<PoiItem> mList;
    private String userInput = "";
    private int selectPosition = -1;
    private OnItemClickLisenter mOnItemClickLisenter;

    public SearchAddressAdapter(Context context, List<PoiItem> list) {
        this.mContext = context;
        this.mList = list;
    }

    public void setList(List<PoiItem> list,String userInput) {
        this.selectPosition = 0;
        this.userInput = userInput;
        this.mList = list;
        notifyDataSetChanged();
    }

    public void setSelectPosition(int position) {
        this.selectPosition = position;
        notifyDataSetChanged();
    }

    public void setOnItemClickLisenter(OnItemClickLisenter onItemClickLisenter) {
        this.mOnItemClickLisenter = onItemClickLisenter;
    }

    @Override
    public SearchAddressAdapter.MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyHolder myHolder = new MyHolder(LayoutInflater.from(mContext).inflate(R.layout.item_address_info, parent, false));
        return myHolder;
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position) {
        holder.itemView.setTag(position);
        PoiItem poiItem = mList.get(position);
        if (position == selectPosition) {
            holder.mCheckBox.setChecked(true);
        } else {
            holder.mCheckBox.setChecked(false);
        }

        String name = poiItem.getTitle();

        if (TextUtils.isEmpty(userInput)) {
            holder.mTvTitle.setText(name);
        } else {
            try {
                if (name != null && name.contains(userInput)) {//如果搜索出来的文字跟输入文字有重叠，则改变重叠文字的颜色
                    int index = name.indexOf(userInput);
                    int len = userInput.length();
                    Spanned temp = Html.fromHtml(name.substring(0, index)
                            + "<font color=#19ad19>"
                            + name.substring(index, index + len) + "</font>"
                            + name.substring(index + len, name.length()));
                    holder.mTvTitle.setText(temp);
                } else {
                    holder.mTvTitle.setText(name);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                holder.mTvTitle.setText(name);
            }
        }



        holder.mTvMessage.setText(poiItem.getProvinceName() + poiItem.getCityName() + poiItem.getAdName() + poiItem.getSnippet());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (Integer) view.getTag();
                setSelectPosition(position);
                if (null != mOnItemClickLisenter) {
                    mOnItemClickLisenter.onItemClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        TextView mTvTitle;
        TextView mTvMessage;
        CheckBox mCheckBox;


        public MyHolder(View itemView) {
            super(itemView);
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mTvMessage = (TextView) itemView.findViewById(R.id.tv_message);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        }
    }

}
