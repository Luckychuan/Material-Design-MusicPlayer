package com.example.luckychuan.musicplayer.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.TabFragmentPagerAdapter;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.fragment.AlbumFragment;
import com.example.luckychuan.musicplayer.fragment.ArtistFragment;
import com.example.luckychuan.musicplayer.fragment.MusicBottomFragment;
import com.example.luckychuan.musicplayer.fragment.MusicFragment;
import com.example.luckychuan.musicplayer.fragment.PlaylistFragment;
import com.example.luckychuan.musicplayer.listener.OnFragmentDeleteModeEnterListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.ImageUtil;
import com.example.luckychuan.musicplayer.util.MenuUtil;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener, OnFragmentDeleteModeEnterListener {

    private static final String TAG = "MainActivity";
    //权限请求码
    private final static int PERMISSION_READ_EXTERNAL_STORAGE = 1;
    //创建新播放列表时选歌启动SelectActivity,并且返回选中的要添加的音乐
    private static final int SELECT_ACTIVITY_RESULT = 1;

    private ViewPager mViewPager;
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ImageView mAlbumNav;
    private static FloatingActionButton mAddButton;

    private static DeleteUtil mDeleteUtil;

    private UIUpdateReceiver mUIUpdateReceiver;
    private DeleteFinishReceiver mDeleteFinishReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExitApplication.getInstance().addActivity(this);
        setContentView(R.layout.activity_main);

        mDeleteUtil = new DeleteUtil(this);

        mViewPager = (ViewPager) findViewById(R.id.viewpager_main);
        mViewPager.addOnPageChangeListener(this);
        //使fragment的数据不重新加载
        mViewPager.setOffscreenPageLimit(3);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);

        //检查权限是否已经获得
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            afterPermissionGrant();
        } else {
            //请求读取手机存储的权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_READ_EXTERNAL_STORAGE);
        }

        //初始化Service对象
        startService(new Intent(MainActivity.this, PlayService.class));

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        invalidateOptionsMenu();

        //底部音乐栏初始化
        LinearLayout mMusicBottom = (LinearLayout) findViewById(R.id.bottom_fragment_main);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new MusicBottomFragment();
        transaction.add(R.id.bottom_fragment_main, fragment);
        transaction.commit();
        mMusicBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayService.getMusicInfo() != null) {
                    Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.translate_in, R.anim.fade_out);

                }
            }
        });

        //侧滑菜单初始化
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        mAlbumNav = (ImageView) headerView.findViewById(R.id.album_nav);
        if (PlayService.getMusicInfo() == null) {
            mAlbumNav.setImageResource(R.drawable.play_page_default_cover);
        } else {
            ImageUtil.displayImage(MainActivity.this, PlayService.getMusicInfo().getAlbumUri(), mAlbumNav);
        }

        mAddButton = (FloatingActionButton) findViewById(R.id.add_playlist_button);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建播放列表
                MenuUtil.editPlaylist(MainActivity.this, new MenuUtil.OnPlaylistDialogConfirmFinishListener() {
                    @Override
                    public void OnPlaylistDialogConfirmFinish(String editName) {
                        //从系统数据库中新建播放列表
                        MediaStoreManager.getInstance(MainActivity.this).addPlaylistData(editName);
                        //选择歌曲
                        startActivityForResult(new Intent(MainActivity.this, SelectMusicActivity.class), SELECT_ACTIVITY_RESULT);
                        //发送广播刷新播放列表
                        Intent intent = new Intent("UPDATE_PLAYLIST");
                        intent.putExtra("playlist_name",editName);
                        sendBroadcast(intent);
                    }
                });
            }
        });

        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        registerReceiver(mUIUpdateReceiver, filter);

        mDeleteFinishReceiver = new DeleteFinishReceiver();
        IntentFilter filter2 = new IntentFilter("DELETE_FINISH");
        registerReceiver(mDeleteFinishReceiver, filter2);

    }

    private void afterPermissionGrant() {
        //四个主页面初始化
        Fragment fragment1 = new PlaylistFragment();
        Fragment fragment2 = new MusicFragment();
        Fragment fragment3 = new AlbumFragment();
        Fragment fragment4 = new ArtistFragment();
        List<Fragment> list = new ArrayList<>();
        list.add(fragment1);
        list.add(fragment2);
        list.add(fragment3);
        list.add(fragment4);
        TabFragmentPagerAdapter adapter = new TabFragmentPagerAdapter(getSupportFragmentManager(), list);
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    /**
     * 选择完歌曲后添加到新创建的列表
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //刚创建的播放列表的id
        int id = MediaStoreManager.getInstance(this).queryNewPlaylistId();
        Log.d(TAG, "onActivityResult: id"+id);
        //获得传回来的音乐数据
        HashMap<Integer, MusicInfo> map = (HashMap<Integer, MusicInfo>) data.getSerializableExtra("result");
        if (map.size() != 0) {
            Set<Map.Entry<Integer, MusicInfo>> sets = map.entrySet();
            List<MusicInfo> list = new ArrayList<>();
            for (Map.Entry entry : sets) {
                list.add((MusicInfo) entry.getValue());
            }
            //将音乐添加到数据库中
            MediaStoreManager.getInstance(this).insertListMusicToPlaylist(MainActivity.this, id, list);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.system_exit);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUIUpdateReceiver);
        unregisterReceiver(mDeleteFinishReceiver);
        ExitApplication.getInstance().removeActivity(this);
        if (!PlayService.isPlaying()) {
            Log.d(TAG, "onDestroy:  play");
            stopService(new Intent(this, PlayService.class));
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mDeleteUtil.isOnDeleteMode()) {
            mDeleteUtil.cancelDeleteMode();
            invalidateOptionsMenu();
            sendBroadcast(new Intent("DELETE_CANCEL"));
            mToolbar.setTitle("音乐盒");
        } else {
            moveTaskToBack(true);
        }
    }

    /**
     * 进入和退出删除模式时改变Toolbar上的菜单
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mDeleteUtil.isOnDeleteMode()) {
            getMenuInflater().inflate(R.menu.menu_delete_main, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_main, menu);

        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.delete_menu:
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
                builder.setTitle("删除：");
                builder.setMessage("是否从设备中删除 " + mDeleteUtil.getDeleteListSize() + " 个选项");
                builder.setCancelable(false);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mDeleteUtil.isOnDeleteMode()) {
                            //删除文件
                            mDeleteUtil.confirmDelete();
                        } else {
                            Toast.makeText(MainActivity.this, "删除失败！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                });
                builder.create().show();
                break;

            case R.id.random_play:
                sendBroadcast(new Intent("RANDOM_PLAY"));
                break;

            case R.id.search:
                startActivity(new Intent(this,SearchActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_search:
                startActivity(new Intent(this,SearchActivity.class));
                break;
            case R.id.nav_system_exit:
                //退出程序
                sendBroadcast(new Intent("SYSTEM_EXIT"));
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * 得到询问权限的结果
     *
     * @param requestCode  权限请求码
     * @param permissions  需求的权限
     * @param grantResults 用户接受的权限
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_READ_EXTERNAL_STORAGE:
                //当权限获得时
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    afterPermissionGrant();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        //弹出对话框提示用户接收权限
                        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                        dialog.setMessage("音乐盒要获得读取存储权限才能扫描手机里的音乐信息哦！");
                        dialog.setCancelable(false);
                        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //请求读取手机存储的权限
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
                            }
                        });
                        dialog.create().show();
                    }
                }
        }
    }

    /**
     * 从FragmentPlaylist中获得FloatingActionButton,使FloatingActionButton和RecyclerView绑定
     */
    public static FloatingActionButton getAddButton() {
        return mAddButton;
    }


    //继承OnPageChangeListener的方法
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mViewPager.getCurrentItem() == 0) {
            mAddButton.show();
        } else {
            mAddButton.hide();
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (mViewPager.getCurrentItem() == 0) {
            mAddButton.show();
        } else {
            mAddButton.hide();
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            if (mDeleteUtil.isOnDeleteMode()) {
                invalidateOptionsMenu();
                mDeleteUtil.cancelDeleteMode();
                sendBroadcast(new Intent("DELETE_CANCEL"));
                mToolbar.setTitle("音乐盒");
            }
        }
    }


    /**
     * 实现OnFragmentDeleteModeEnterListener的方法，4个fragment的item长按时进入删除模式，切换菜单
     */
    @Override
    public void startDeleteMode(int deleteType) {
        mDeleteUtil.startDeleteMode(deleteType);
        invalidateOptionsMenu();
    }

    @Override
    public void onListItemClick(Integer id) {
        mDeleteUtil.selectItem(id);
        mToolbar.setTitle("已选择： " + mDeleteUtil.getDeleteListSize());
    }

    @Override
    public void cancelDeleteMode() {
        mDeleteUtil.cancelDeleteMode();
    }

    public static boolean isDeleteMode() {
        return mDeleteUtil.isOnDeleteMode();
    }


    /**
     * 接收Service上切换音乐时更新UI的广播
     */
    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                if (PlayService.getMusicInfo() == null) {
                    mAlbumNav.setImageResource(R.drawable.play_page_default_cover);
                } else {
                    ImageUtil.displayImage(MainActivity.this, PlayService.getMusicInfo().getAlbumUri(), mAlbumNav);
                }
            }
    }

    /**
     * 接收DeleteUtil上完成删除后的广播
     */
    class DeleteFinishReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //恢复原来的UI
            mToolbar.setTitle("音乐盒");
            invalidateOptionsMenu();
        }
    }

}
