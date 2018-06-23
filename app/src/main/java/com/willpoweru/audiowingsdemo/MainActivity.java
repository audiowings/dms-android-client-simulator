/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.willpoweru.audiowingsdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** An Activity to browse and play media. */
public class MainActivity extends AppCompatActivity implements
        SelectPlaylistDialogFragment.OnPlaylistCreatedListener,
        DmsClient.OnVolleyResponse,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener, MenuItem.OnMenuItemClickListener {

    public static final String TAG_REQUEST_TRACK_URL = "TRACK_URL";
    private static final String LOG_TAG = "<Audiowings>";
    public String mAwDmsServerAddress;

//    private BrowseAdapter mBrowserAdapter;

    private MediaMetadataCompat mCurrentMetadata;
    private PlaybackStateCompat mCurrentState;

    private PlaybackManager mPlaybackManager;
    private TextView touchControl;

    private Playlist mPlaylist;
    private JSONObject mResponse;

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    private WearableActionDrawerView mWearableActionDrawer;

    private boolean mIsWatch;


    // TODO: [1] Uncomment the following block for playback in a Service
    /*
    private MediaBrowserCompat mMediaBrowser;

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mSubscriptionCallback);
                    try {
                        MediaControllerCompat mediaController =
                                new MediaControllerCompat(
                                        MainActivity.this, mMediaBrowser.getSessionToken());
                        updatePlaybackState(mediaController.getPlaybackState());
                        updateMetadata(mediaController.getMetadata());
                        mediaController.registerCallback(mMediaControllerCallback);
                        MediaControllerCompat.setMediaController(
                                MainActivity.this, mediaController);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    updateMetadata(metadata);
                    mBrowserAdapter.notifyDataSetChanged();
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    updatePlaybackState(state);
                    mBrowserAdapter.notifyDataSetChanged();
                }

                @Override
                public void onSessionDestroyed() {
                    updatePlaybackState(null);
                    mBrowserAdapter.notifyDataSetChanged();
                }
            };

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(
                        String parentId, List<MediaBrowserCompat.MediaItem> children) {
                    onMediaLoaded(children);
                }
            };
    */

//    public void onMediaLoaded(List<MediaBrowserCompat.MediaItem> media) {
//        mBrowserAdapter.clear();
//        mBrowserAdapter.addAll(media);
//        mBrowserAdapter.notifyDataSetChanged();
//    }

    private void playTrack(MediaBrowserCompat.MediaItem item) {
        if (item.isPlayable()) {
            // TODO: [2] Remove the following lines for playback in a Service
            MediaMetadataCompat metadata = mPlaylist.getMetadata(/*this,*/ item.getMediaId());
            mPlaybackManager.play(metadata);
            updateMetadata(metadata);

            // TODO: [2] Uncomment the following block for playback in a Service
            /*
            MediaControllerCompat.getMediaController(this)
                    .getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
            */
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        String mAwDmsServerAddress = sharedPref.getString(getResources()
//                .getString(R.string.pref_key_audiowings_server_address), "");

        mIsWatch = getResources().getBoolean(R.bool.watch);
        setContentView(R.layout.activity_main);
        setTitle(getString(R.string.app_name));
//        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(this,this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);


        touchControl = findViewById(R.id.touch_control);
//        touchControl.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                promptPlaylist();
//                return false;
//            }
//        });
        touchControl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

//        mBrowserAdapter = new BrowseAdapter(this);


        if (mIsWatch) {
            // Bottom Action Drawer
            mWearableActionDrawer =
                    (WearableActionDrawerView) findViewById(R.id.bottom_action_drawer);
            // Peeks action drawer on the bottom.
            mWearableActionDrawer.getController().peekDrawer();
            mWearableActionDrawer.setOnMenuItemClickListener(this);

            // From Java: drawerView.setDrawerContent(drawerContentView); drawerView.setPeekContent(peekContentView);
            //
            // <!-- From XML: --> <androidx.wear.widget.drawer.WearableDrawerView android:layout_width="match_parent" android:layout_height="match_parent" android:layout_gravity="bottom" android:background="@color/red" app:drawerContent="@+id/drawer_content" app:peekView="@+id/peek_view"> <FrameLayout android:id="@id/drawer_content" android:layout_width="match_parent" android:layout_height="match_parent" /> <LinearLayout android:id="@id/peek_view" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_gravity="center_horizontal" android:orientation="horizontal"> <ImageView android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@android:drawable/ic_media_play" /> <ImageView android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@android:drawable/ic_media_pause" /> </LinearLayout> </androidx.wear.widget.drawer.WearableDrawerView>



            /* Action Drawer Tip: If you only have a single action for your Action Drawer, you can use a
             * (custom) View to peek on top of the content by calling
             * mWearableActionDrawer.setPeekContent(View). Make sure you set a click listener to handle
             * a user clicking on your View.
             */

        }
    }

    private void promptPlaylist() {
        SelectPlaylistDialogFragment.newInstance()
                .show(getSupportFragmentManager(), "dialog");
    }


    @Override
    public void onStart() {
        super.onStart();

        // TODO: [3] Remove the following lines for playback in a Service
        mPlaybackManager =
                new PlaybackManager(
                        this,
                        new PlaybackManager.Callback() {
                            @Override
                            public void onPlaybackStatusChanged(PlaybackStateCompat state) {
//                                mBrowserAdapter.notifyDataSetChanged();
                                updatePlaybackState(state);
                            }
                        });
//        onMediaLoaded(Playlist.getMediaItems());

        // TODO: [3] Uncomment the following block for playback in a Service
        /*
        mMediaBrowser =
                new MediaBrowserCompat(
                        this,
                        new ComponentName(this, MusicService.class),
                        mConnectionCallback,
                        null);
        mMediaBrowser.connect();
        */
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: [4] Remove the following line for playback in a Service
        mPlaybackManager.stop();

        // TODO: [4] Uncomment the following block for playback in a Service
        /*
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            controller.unregisterCallback(mMediaControllerCallback);
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            if (mCurrentMetadata != null) {
                mMediaBrowser.unsubscribe(mCurrentMetadata.getDescription().getMediaId());
            }
            mMediaBrowser.disconnect();
        }
        */
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {

        Log.d(DEBUG_TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {

        float E1xToE2x = event2.getX() - event1.getX();
        float E1yToE2y = event2.getY() - event1.getY();
        Log.d(DEBUG_TAG, "onFling: Flung x Distance = " + E1xToE2x);
        Log.d(DEBUG_TAG, "onFling: Flung y Distance = " + E1yToE2y);
        // Check if motion along x or y axis
        if (Math.abs(E1xToE2x) > Math.abs(E1yToE2y)){
            // Check if motion is left to right
            if(Math.signum(E1xToE2x) == 1.0 ){
                Log.d(DEBUG_TAG, "onFling: signum = " + Math.signum(E1xToE2x) + " Left to Right");
                mPlaybackManager.skipForwardTrack();
            }
            // Check if motion is right to left
            else if (Math.signum(E1xToE2x) == -1.0 ){
                Log.d(DEBUG_TAG, "onFling: signum = " + Math.signum(E1xToE2x) + " Right to Left");
                mPlaybackManager.skipBackTrack();
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
        promptPlaylist();
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());

        if(mPlaylist != null){
            playPause();
            return true;
        }
        else {
            return false;
        }
    }


    private void updatePlaybackState(PlaybackStateCompat state) {
        mCurrentState = state;
        if (state == null
                || state.getState() == PlaybackStateCompat.STATE_PAUSED
                || state.getState() == PlaybackStateCompat.STATE_STOPPED) {

        } else {


        }
//        mPlaybackControls.setVisibility(state == null ? View.GONE : View.VISIBLE);
    }

    private void updateMetadata(MediaMetadataCompat metadata) {
        mCurrentMetadata = metadata;


//        mBrowserAdapter.notifyDataSetChanged();
    }


    // Event to signal that a playlist is loaded and ready to play
    @Override
    public void onPlaylistCreated(JSONArray jsonArrayPlaylist) {

//        mPlaylist = null;
        mPlaylist = getPlaylist(jsonArrayPlaylist);
        int plSize = mPlaylist.getSize();
        setTrackUrls();

        mPlaybackManager.setPlaylist(mPlaylist);
//        onMediaLoaded(mPlaylist.getMediaItems());



        MediaBrowserCompat.MediaItem mediaItem = mPlaylist.getMediaItems().get(mPlaybackManager.getCurrentIndex());

        playTrack(mediaItem);

    }

    private void setTrackUrls() {
        List<MediaBrowserCompat.MediaItem> mediaItems = Playlist.getMediaItems();
        for (MediaBrowserCompat.MediaItem item: mediaItems){
            Playlist.putMediaUrl(item.getMediaId(), item.getDescription().getMediaUri().toString());
        }
    }

    private Playlist getPlaylist(JSONArray jsonArrayPlaylist)  {
        Playlist playlist = new Playlist();
        for (int i = 0; i < jsonArrayPlaylist.length(); i++) {
            JSONObject item;
            try {
                item = jsonArrayPlaylist.getJSONObject(i);

                playlist.createMediaMetadataCompat(
                        String.valueOf(item.getLong("id")),
                        item.getString("title"),
                        item.getJSONObject("artist").getString("name"),
                        item.getJSONObject("album").getString("title"),
                        item.isNull("genre")? "?": item.getString("genre"),
                        item.getInt("duration"),
                        -1,
                        item.getJSONObject("urlInfo").getJSONArray("urls").getString(0),
                        item.getJSONObject("album").getString("title")
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return playlist;
    }

    @Override
    public void onSuccess(String tag, JSONObject response) {
        if(response != null) {
            switch (tag) {
                case TAG_REQUEST_TRACK_URL :
                    String title = "";
                    mResponse = response;

                    try {
                        title = response.getJSONArray("items")
                                .getJSONObject(0).getString("title");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
        Log.d(LOG_TAG, "Response... " + response);
    }

    // An adapter for showing the list of browsed MediaItem's
//    private class BrowseAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {
//
//        public BrowseAdapter(Activity context) {
//            super(context, R.layout.media_list_item, new ArrayList<MediaBrowserCompat.MediaItem>());
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            MediaBrowserCompat.MediaItem item = getItem(position);
//            int itemState = MediaItemViewHolder.STATE_NONE;
//            if (item.isPlayable()) {
//                String itemMediaId = item.getDescription().getMediaId();
//                int playbackState = PlaybackStateCompat.STATE_NONE;
//                if (mCurrentState != null) {
//                    playbackState = mCurrentState.getState();
//                }
//                if (mCurrentMetadata != null
//                        && itemMediaId.equals(mCurrentMetadata.getDescription().getMediaId())) {
//                    if (playbackState == PlaybackStateCompat.STATE_PLAYING
//                            || playbackState == PlaybackStateCompat.STATE_BUFFERING) {
//                        itemState = MediaItemViewHolder.STATE_PLAYING;
//                    } else if (playbackState != PlaybackStateCompat.STATE_ERROR) {
//                        itemState = MediaItemViewHolder.STATE_PAUSED;
//                    }
//                }
//            }
//            return MediaItemViewHolder.setupView(
//                    (Activity) getContext(), convertView, parent, item.getDescription(), itemState);
//        }
//    }

    // Invoked by single tap on main screen
    private final View.OnClickListener mPlaybackButtonListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playPause();
                }
            };

    private void playPause() {
        final int state =
                mCurrentState == null
                        ? PlaybackStateCompat.STATE_NONE
                        : mCurrentState.getState();
        if (state == PlaybackStateCompat.STATE_PAUSED
                || state == PlaybackStateCompat.STATE_STOPPED
                || state == PlaybackStateCompat.STATE_NONE) {

            if (mCurrentMetadata == null) {
                mCurrentMetadata =
                        mPlaylist.getMetadata(
                                /*MainActivity.this,*/
                                mPlaylist.getMediaItems().get(0).getMediaId());
                updateMetadata(mCurrentMetadata);
            }

            // TODO: [5] Remove the following line for playback in a Service
            mPlaybackManager.play(mCurrentMetadata);

            // TODO: [5] Uncomment the following block for playback in a Service
            /*
            MediaControllerCompat.getMediaController(MainActivity.this)
                    .getTransportControls()
                    .playFromMediaId(
                            mCurrentMetadata.getDescription().getMediaId(), null);
            */
        } else {
            // TODO: [6] Remove the following line for playback in a Service
            mPlaybackManager.pause();

            // TODO: [6] Uncomment the following block for playback in a Service
            /*
            MediaControllerCompat.getMediaController(MainActivity.this)
                    .getTransportControls()
                    .pause();
            */
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
