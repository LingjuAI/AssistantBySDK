package com.lingju.assistant.service;

import android.content.Context;

import com.lingju.common.adapter.NetworkAdapter;
import com.lingju.util.NetUtil;

/**
 * Created by Ken on 2017/5/9.
 */
public class NetWorkAccessAdapter extends NetworkAdapter {
    private Context mContext;

    public NetWorkAccessAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isOnline() {
        return NetUtil.getInstance(mContext).getCurrentNetType().isOnline();
    }

    @Override
    public NetType currentNetworkType() {
        NetUtil.NetType currentNetType = NetUtil.getInstance(mContext).getCurrentNetType();
        if (currentNetType == NetUtil.NetType.NETWORK_TYPE_WIFI) {
            return NetType.WIFI;
        } else if (currentNetType == NetUtil.NetType.NETWORK_TYPE_3G) {
            return NetType.MOBILE4G;
        } else if (currentNetType == NetUtil.NetType.NETWORK_TYPE_2G) {
            return NetType.MOBILE2G;
        } else {
            return NetType.OFFLINE;
        }
    }
}
