package com.willpoweru.audiowingsdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.wear.widget.BoxInsetLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.HashMap;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     SelectPlaylistDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 * <p>You activity (or fragment) needs to implement {@link SelectPlaylistDialogFragment.Listener}.</p>
 */
public class SelectPlaylistDialogFragment extends DialogFragment implements DmsClient.OnVolleyResponse
{
    private static final String LOG_TAG = "<Audiowings>";


    private static final HashMap<String, String> HEADER_DEVICE_ID = new HashMap<String, String>(){{
        put("X-Audiowings-DeviceId", DmsClient.MAC_ADDRESS);
    }};


    public static final String TAG_REQUEST_PLAYLISTS = "PLAYLISTS";
    public static final String TAG_REQUEST_PLAYLIST = "PLAYLIST";



    TextView textPlaylistPrompt;
    private ImageButton buttonPlaylistYes;
    private ImageButton buttonPlaylistNo;
    private TextView textvResponse;

    private Listener mListener;
    private JSONObject mResponse;
    private int mPlaylistItemsCount;
    private int mCurrentItem;

//    private BoxInsetLayout promptParent;



    private String mBaseUrl;
    private String mPlaylistsUrl;
    private String mPlaylistUrl;

    OnPlaylistCreatedListener mCallback;
    private DmsClient mDmsClient;

    // Container Activity must implement this interface
    public interface OnPlaylistCreatedListener {
        void onPlaylistCreated(JSONArray jsonArrayPlaylist);
    }

    // TODO: Customize parameters
    public static SelectPlaylistDialogFragment newInstance() {
        final SelectPlaylistDialogFragment fragment = new SelectPlaylistDialogFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_playlist_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        Boolean watch = getResources().getBoolean(R.bool.watch);
        if (watch){
        BoxInsetLayout promptParent = (BoxInsetLayout) view.getRootView();
            promptParent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        } else {
            FrameLayout promptParent = (FrameLayout) view.getRootView();
            promptParent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        textPlaylistPrompt = view.findViewById(R.id.text_playlist_prompt);

        buttonPlaylistYes = view.findViewById(R.id.button_pl_prompt_yes);
        buttonPlaylistNo = view.findViewById(R.id.button_pl_prompt_no);

        buttonPlaylistYes.setEnabled(false);
        buttonPlaylistNo.setEnabled(false);

        buttonPlaylistYes.setOnClickListener(new View.OnClickListener() {
            /*

         */
            @Override
            public void onClick(View v) {
                try {
                    JSONArray items = mResponse.getJSONArray("items");
                    String title = items.getJSONObject(mCurrentItem).getString("title");
                    getPlaylist(title);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                dismiss();
            }
        });

        buttonPlaylistNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mPlaylistItemsCount = mResponse.getJSONArray("items").length();
                    mCurrentItem++;
                    if (mCurrentItem == mPlaylistItemsCount){
                        mCurrentItem = 0;
                    }
                    String title = mResponse.getJSONArray("items")
                            .getJSONObject(mCurrentItem).getString("title");
                    textPlaylistPrompt.setText("Select playlist entitled " + title);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mDmsClient.requestDmsData(TAG_REQUEST_PLAYLISTS, mPlaylistsUrl, HEADER_DEVICE_ID, null);
    }



    @Override
    public void onSuccess(String tag, JSONObject response) {
        if(response != null) {
            switch (tag) {
                case TAG_REQUEST_PLAYLISTS :
                    String title = "";
                    mResponse = response;
                    buttonPlaylistYes.setEnabled(true);
                    buttonPlaylistNo.setEnabled(true);
                    try {
                        title = response.getJSONArray("items")
                                .getJSONObject(0).getString("title");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    textPlaylistPrompt.setText("Select playlist entitled " + title);

                    break;

                case TAG_REQUEST_PLAYLIST :
                    try {
                        JSONArray jsonArrayPlaylist = response.getJSONArray("items");
                        mCallback.onPlaylistCreated(jsonArrayPlaylist);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        Log.d(LOG_TAG, "Response... " + response);
    }

    private void getPlaylist(final String title) {
        String playlistId = null;

        try {
            playlistId = mResponse.getJSONArray("items")
                    .getJSONObject(mCurrentItem).getString("playlistId");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String params = "?playlistId=" + playlistId + "&playlistTitle=" + title;
        mDmsClient.requestDmsData(TAG_REQUEST_PLAYLIST, mPlaylistUrl + params, HEADER_DEVICE_ID, null);
    }

    private void saveFile(String filename, String fileContents) {

        FileOutputStream outputStream;

        try {
            outputStream = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

//        mSingleton = MySingleton.getInstance(context);

        mDmsClient = new DmsClient(this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String mProxyAddress = sharedPref.getString(getResources()
                .getString(R.string.pref_key_audiowings_server_address), "");
        mBaseUrl = "http://" + mProxyAddress;
        mPlaylistsUrl = mBaseUrl + "/playlists/";
        mPlaylistUrl = mBaseUrl + "/playlist/"; // ?playlistId=";

        Log.d("LOG", "PlaylistsUrl = " + mPlaylistsUrl);

        final Fragment parent = getParentFragment();
//        if (parent != null) {
//            mListener = (Listener) parent;
//        } else {
//            mListener = (Listener) context;
//        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnPlaylistCreatedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPlaylistCreatedListener");
        }
    }


    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    public interface Listener {
        void onItemClicked(int position);
    }


}
