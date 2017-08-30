package com.lingju.assistant.activity.index.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.BottomBoxStateEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.entity.Lyric;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.IBatchPlayer.PlayListType;
import com.lingju.assistant.player.audio.model.AudioRepository;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.model.PlayMusic;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2016/11/23.
 */
public class PlayerListPagerFragment extends Fragment implements IBatchPlayer.BodyView {

    private final static String TAG = "PlayerListPagerFragment";

    @BindView(R.id.player_list_viewPager)
    ViewPager viewPager;
    @BindView(R.id.player_list_dots)
    ImageView dotBt;

    private PlayerListFragment[] fragments = new PlayerListFragment[4];
    private PlayerListLrcFragment lrcFragment;
    private IBatchPlayer.Presenter presenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frag_player_body, container, false);
        ButterKnife.bind(this, root);
        fragments[PlayListType.PUSH] = new PlayerListFragment()
                .setPlayMusicList(presenter.getPlayList(PlayListType.PUSH))
                .setTitle("推荐列表")
                .setTips("哎呀，这里暂时是空的，快去听歌吧！这样小灵才能知道你的喜好，为你推荐歌曲")
                .setPlayerListItemClickListener(new LingjuPlayerListItemListener(PlayListType.PUSH));
        fragments[PlayListType.REQUEST] = (lrcFragment = (PlayerListLrcFragment) new PlayerListLrcFragment()
                .setPlayMusicList(presenter.getPlayList(PlayListType.REQUEST))
                .setTitle("播放列表")
                .setTips("当前没有播放的歌曲")
                .setPlayerListItemClickListener(new LingjuPlayerListItemListener(PlayListType.REQUEST)));
        fragments[PlayListType.FAVORITE] = new PlayerListFragment()
                .setPlayMusicList(presenter.getPlayList(PlayListType.FAVORITE))
                .setTitle("收藏列表")
                .setTips("哎呀，这里暂时是空的，快去收藏你喜欢的歌曲吧！")
                .setPlayerListItemClickListener(new LingjuPlayerListItemListener(PlayListType.FAVORITE));
        fragments[PlayListType.LOCAL] = new PlayerListFragment()
                .setPlayMusicList(presenter.getPlayList(PlayListType.LOCAL))
                .setTitle("本地歌曲")
                .setTips("哎呀，这里暂时是空的，快去把你喜欢的歌曲下载到手机吧！只要对我说播放手机里的歌，小灵就可以播放这里的歌曲啦！")
                .setPlayerListItemClickListener(new LingjuPlayerListItemListener(PlayListType.LOCAL));
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new PlayerListPagerAdapter(getActivity().getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int p) {
                int musicPagersSelected = p % 4;
                fragments[musicPagersSelected].refresh();
                if (musicPagersSelected == 1 && lrcFragment != null) {
                    lrcFragment.setFavoriteState(presenter.currentPlayMusic());
                }
                switch (musicPagersSelected) {
                    case 0:
                        dotBt.setImageResource(R.drawable.slide_bg1);
                        break;
                    case 1:
                        dotBt.setImageResource(R.drawable.slide_bg2);
                        break;
                    case 2:
                        dotBt.setImageResource(R.drawable.slide_bg3);
                        break;
                    case 3:
                        dotBt.setImageResource(R.drawable.slide_bg4);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setCurrentItem(1001);
        if (this.presenter != null) {
            this.presenter.resetLyric();
        }
        return root;
    }

    @Override
    public void setPresenter(IBatchPlayer.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fragments[PlayListType.REQUEST].setMusicPlayMode(presenter.getPlayMode());
        updatePlayerList(presenter.getPlayListType(), AudioRepository.get().findByListType(presenter.getPlayListType()).getCurrentOrderIndex());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @OnClick(R.id.player_list_back)
    @Override
    public void disappear() {
        //话筒波纹效果展示
        AppConfig.dPreferences.edit().putBoolean("wave_show", true).commit();
        //键盘输入界面切换为话筒界面如果正在识别时打开波纹效果
        if (IflyRecognizer.getInstance().isRecognizing()) {
            EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
        }
        getActivity().getSupportFragmentManager().beginTransaction().hide(this).commit();
        ((MainActivity) getActivity()).updateBottomBoxState(new BottomBoxStateEvent(true));
    }

    @Override
    public void updateLrc(int currentDuration, int duration) {
        //Log.i(TAG,"updateLrc>>>>>>"+currentDuration+"--"+duration);
        if (lrcFragment != null)
            lrcFragment.updateProgress(currentDuration, duration);
    }

    @Override
    public void updatePlayerList(int listType, int playingPosition) {
        // Log.i(TAG, "updatePlayerList>>>>>>" + listType + "--" + playingPosition);
        for (int i = 0; i < fragments.length; i++) {
            if (i == listType) {
                fragments[i].setPlayingPosition(playingPosition);
                fragments[i].refresh();
            } else {
                fragments[i].setPlayingPosition(-1);
            }
        }
    }

    @Override
    public void showCurrentMusic(PlayMusic music) {
        if (lrcFragment != null)
            lrcFragment.showCurrentMusic(music);
    }

    @Override
    public void updatePlayMode(int playMode) {
        if (lrcFragment != null)
            lrcFragment.setPlayMode(playMode);
    }

    @Override
    public void setLyric(Lyric lyric) {
        if (lrcFragment != null)
            lrcFragment.setLyric(lyric);
    }

    @Override
    public void showLyric() {
        //展开歌词列表
        if (lrcFragment != null && !lrcFragment.isLrcOpen())
            lrcFragment.switchLyricBox();
    }

    @Override
    public void updateFavorte() {
        if (lrcFragment != null)
            lrcFragment.updateFavorte();
    }

    class PlayerListPagerAdapter extends FragmentPagerAdapter {

        public PlayerListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container, position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position % 4];
        }

        @Override
        public long getItemId(int position) {
            return position % 4;
        }

        @Override
        public int getCount() {
            return Integer.MAX_VALUE;
        }

    }

    class LingjuPlayerListItemListener implements PlayerListFragment.PlayerListItemClickListener {

        private int listType;

        public LingjuPlayerListItemListener(int listType) {
            this.listType = listType;
        }

        @Override
        public void onItemClick(int position, View view) {
            Log.i(TAG, "onItemClick>>" + listType + "-" + position);
            updatePlayerList(listType, position);
            presenter.setPlayListType(this.listType);
            presenter.play(position).subscribe();
            ((MainActivity) getActivity()).addPlayerHeader(presenter.currentPlayMusic());
        }

        @Override
        public void onItemFavoriteClick(int position, View view) {
            Log.i(TAG, "onItemFavoriteClick>>" + listType + "-" + position);
            if (position == -1)
                return;
            PlayMusic m = presenter.getPlayList(listType).size() > position ? presenter.getPlayList(listType).get(position) : null;
            if (m != null) {
                if (m.getFavorite()) {
                    presenter.removeFromFavoites(m);
                    presenter.removeFavoriteFromList(m);
                } else
                    presenter.addToFavoites(m);
            }
            fragments[listType].refresh();
        }
    }

}
