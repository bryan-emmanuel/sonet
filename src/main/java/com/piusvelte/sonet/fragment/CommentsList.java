package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.loader.CommentsLoader;
import com.piusvelte.sonet.loader.LikeCommentLoader;
import com.piusvelte.sonet.loader.SendCommentLoader;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.social.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static com.piusvelte.sonet.Sonet.MYSPACE;

/**
 * Created by bemmanuel on 4/21/15.
 */
public class CommentsList extends ListFragment
        implements TextWatcher, View.OnKeyListener, View.OnClickListener {

    private static final int LOADER_COMMENTS = 0;
    private static final int LOADER_SEND = 1;
    private static final int LOADER_LIKE = 2;

    private static final String DIALOG_OPTIONS = "dialog:options";

    private static final int REQUEST_OPTIONS = 0;

    private static final String ARG_DATA = "data";
    private static final String ARG_ESID = "esid";
    private static final String ARG_SID = "sid";
    private static final String ARG_DO_LIKE = "do_like";
    private static final String ARG_MESSAGE = "message";

    private static final String STATE_PENDING_LOADERS = "state:pending_loaders";
    private static final String STATE_MESSAGE = "state:message";

    private int mService = Sonet.INVALID_SERVICE;
    private String mSid = null;
    private String mEsid = null;
    private List<HashMap<String, String>> mComments = new ArrayList<>();
    private String mServiceName = null;
    @Nullable
    private Client mClient;

    private EditText mMessage;
    private ImageButton mSend;
    private TextView mCount;

    private SimpleAdapter mAdapter;

    private View mLoadingView;

    private CommentsLoaderCallbacks mCommentsLoaderCallbacks = new CommentsLoaderCallbacks(this);
    private SendLoaderCallbacks mSendLoaderCallbacks = new SendLoaderCallbacks(this);
    private LikeLoaderCallbacks mLikeLoaderCallbacks = new LikeLoaderCallbacks(this);

    // TODO stop this
    @Deprecated
    @NonNull
    private Set<Integer> mPendingLoaders = new HashSet<>();

    public static CommentsList newInstance(@NonNull Uri data) {
        CommentsList commentsList = new CommentsList();
        Bundle args = new Bundle();
        args.putString(ARG_DATA, data.toString());
        commentsList.setArguments(args);
        return commentsList;
    }

    public void setData(@NonNull Uri data) {
        getArguments().putString(ARG_DATA, data.toString());

        if (isResumed()) {
            refresh();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.comments_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessage = (EditText) view.findViewById(R.id.message);
        mSend = (ImageButton) view.findViewById(R.id.send);
        mCount = (TextView) view.findViewById(R.id.count);
        mLoadingView = view.findViewById(R.id.loading);

        mMessage.addTextChangedListener(this);
        mMessage.setOnKeyListener(this);
        mSend.setOnClickListener(this);

        mAdapter = new SimpleAdapter(getActivity(),
                mComments,
                R.layout.comment,
                new String[] { Entity.FRIEND,
                        Statuses.MESSAGE,
                        Statuses.CREATEDTEXT,
                        getString(R.string.like) },
                new int[] { R.id.friend,
                        R.id.message,
                        R.id.created,
                        R.id.like });

        setListAdapter(mAdapter);

        if (savedInstanceState != null) {
            mMessage.setText(savedInstanceState.getString(STATE_MESSAGE));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        mLoadingView.setVisibility(View.VISIBLE);

        LoaderManager loaderManager = getLoaderManager();

        Bundle args = new Bundle();
        args.putString(ARG_DATA, getArguments().getString(ARG_DATA));
        getLoaderManager().initLoader(LOADER_COMMENTS, args, new CommentsLoaderCallbacks(this));

        if (loaderManager.getLoader(LOADER_SEND) != null) {
            loaderManager.initLoader(LOADER_SEND, null, mSendLoaderCallbacks);
        }

        if (loaderManager.getLoader(LOADER_LIKE) != null) {
            loaderManager.initLoader(LOADER_LIKE, null, mLikeLoaderCallbacks);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_comments, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int loaderIndex = 0;
        int[] loaders = new int[mPendingLoaders.size()];

        for (Integer loader : mPendingLoaders) {
            loaders[loaderIndex] = loader;
            loaderIndex++;
        }

        outState.putIntArray(STATE_PENDING_LOADERS, loaders);
        outState.putString(STATE_MESSAGE, mMessage.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_comments_refresh) {
            refresh();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        mMessage = null;
        mSend = null;
        mCount = null;
        mLoadingView = null;
        super.onDestroyView();
    }

    private void setCommentStatus(int position, String status) {
        if (mComments.size() > position) {
            HashMap<String, String> comment = mComments.get(position);
            comment.put(getString(R.string.like), status);
            mComments.set(position, comment);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void refresh() {
        mMessage.setEnabled(false);
        mLoadingView.setVisibility(View.VISIBLE);
        Bundle args = new Bundle();
        args.putString(ARG_DATA, getArguments().getString(ARG_DATA));
        getLoaderManager().restartLoader(LOADER_COMMENTS, args, mCommentsLoaderCallbacks);
    }

    @Override
    public void onListItemClick(ListView list, View view, final int position, long id) {
        super.onListItemClick(list, view, position, id);
        HashMap<String, String> comment = mComments.get(position);
        final String sid = comment.get(Statuses.SID);
        final String liked = comment.get(getString(R.string.like));
        final boolean doLike = !getString(R.string.like).equals(liked);

        // wait for previous attempts to finish
        if (!TextUtils.isEmpty(liked) && !liked.equals(getString(R.string.loading))) {
            // parse comment body, as in StatusDialog.java
            Matcher m = Sonet.getLinksMatcher(mComments.get(position).get(Statuses.MESSAGE));
            int count = 0;

            while (m.find()) {
                count++;
            }

            String[] items = new String[count + 1];
            items[0] = mClient.getLikeText(doLike);
            count = 1;
            m.reset();

            while (m.find()) {
                items[count++] = m.group();
            }

            CommentOptionsDialogFragment.newInstance(REQUEST_OPTIONS, items, sid, doLike)
                    .show(getChildFragmentManager(), DIALOG_OPTIONS);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // NO-OP
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // NO-OP
    }

    @Override
    public void afterTextChanged(Editable s) {
        mCount.setText(Integer.toString(s.toString().length()));
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        mCount.setText(Integer.toString(mMessage.getText().toString().length()));
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v == mSend) {
            if (!TextUtils.isEmpty(mMessage.getText().toString()) && mClient != null) {
                mMessage.setEnabled(false);
                mSend.setEnabled(false);

                mLoadingView.setVisibility(View.VISIBLE);
                Bundle args = new Bundle();
                args.putString(ARG_SID, mSid);
                args.putString(ARG_MESSAGE, mMessage.getText().toString());
                getLoaderManager().restartLoader(LOADER_SEND, null, mSendLoaderCallbacks);
            } else {
                (Toast.makeText(getActivity(), "error parsing message body", Toast.LENGTH_LONG)).show();
                mMessage.setEnabled(true);
                mSend.setEnabled(true);
            }
        }
    }

    private void setComments(CommentsLoader.Result result) {
        mLoadingView.setVisibility(View.GONE);
        mComments.clear();

        if (result != null) {
            mClient = result.client;
            mService = result.service;
            mSid = result.sid;
            mEsid = result.esid;
            mServiceName = result.serviceName;

            mMessage.setText("");

            if (result.isCommentable) {
                if (!TextUtils.isEmpty(result.messagePretext)) {
                    mMessage.append(result.messagePretext);
                }
            } else {
                mSend.setEnabled(false);
                mMessage.setEnabled(false);
                mMessage.setText(R.string.uncommentable);
            }

            if (result.isLikeable) {
                setCommentStatus(0, result.likeText);
            } else {
                setCommentStatus(0, getString(R.string.unlikable));
            }

            mMessage.setEnabled(true);

            if (result.socialClientComments != null && result.socialClientComments.size() > 0) {
                mComments.addAll(result.socialClientComments);
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private void onSendResult(Boolean result) {
        mLoadingView.setVisibility(View.GONE);

        if (Boolean.TRUE.equals(result)) {
            Toast.makeText(getActivity(), mServiceName + " " + getString(R.string.success), Toast.LENGTH_LONG).show();
        } else if (mService == MYSPACE) {
            // myspace permissions
            Toast.makeText(getActivity(), getActivity().getResources().getStringArray(R.array.service_entries)[MYSPACE] + getString(
                    R.string.failure) + " " + getString(R.string.myspace_permissions_message), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG).show();
        }
    }

    private void onLikeResult(LikeCommentLoader.Result result) {
        mLoadingView.setVisibility(View.GONE);

        if (result != null) {
            Toast.makeText(getActivity(), mServiceName + " " + getString(result.wasSuccessful ? R.string.success : R.string.failure),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_OPTIONS:
                if (resultCode == Activity.RESULT_OK) {
                    int which = ItemsDialogFragment.getWhich(data, 0);

                    if (which == 0) {
                        // like/unlike
                        String sid = CommentOptionsDialogFragment.getSid(data);
                        boolean doLike = CommentOptionsDialogFragment.getDoLiked(data, false);

                        mLoadingView.setVisibility(View.VISIBLE);

                        Bundle args = new Bundle();
                        args.putString(ARG_ESID, mEsid);
                        args.putString(ARG_SID, sid);
                        args.putBoolean(ARG_DO_LIKE, doLike);

                        getLoaderManager().restartLoader(LOADER_LIKE, args, mLikeLoaderCallbacks);
                    } else {
                        String[] items = ItemsDialogFragment.getItems(data);

                        if (items != null && which < items.length && items[which] != null) {
                            // open link
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(items[which])));
                        } else {
                            (Toast.makeText(getActivity(), getString(R.string.error_status), Toast.LENGTH_LONG)).show();
                        }
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Nullable
    private Client getClient() {
        return mClient;
    }

    private static class CommentsLoaderCallbacks implements LoaderManager.LoaderCallbacks<CommentsLoader.Result> {

        private CommentsList mCommentsList;

        public CommentsLoaderCallbacks(CommentsList commentsList) {
            mCommentsList = commentsList;
        }

        @Override
        public Loader<CommentsLoader.Result> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_COMMENTS:
                    return new CommentsLoader(mCommentsList.getActivity(),
                            Uri.parse(mCommentsList.getArguments().getString(ARG_DATA)));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<CommentsLoader.Result> loader, CommentsLoader.Result data) {
            switch (loader.getId()) {
                case LOADER_COMMENTS:
                    mCommentsList.setComments(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<CommentsLoader.Result> loader) {
            // NO-OP
        }
    }

    private static class SendLoaderCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {

        private CommentsList mCommentsList;

        public SendLoaderCallbacks(CommentsList commentsList) {
            mCommentsList = commentsList;
        }

        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_SEND:
                    return new SendCommentLoader(mCommentsList.getActivity(),
                            mCommentsList.getClient(),
                            args.getString(ARG_SID),
                            args.getString(ARG_MESSAGE));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Boolean> loader, Boolean data) {
            switch (loader.getId()) {
                case LOADER_SEND:
                    mCommentsList.onSendResult(data);
                    loader.abandon();
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Boolean> loader) {
            // NO-OP
        }
    }

    private static class LikeLoaderCallbacks implements LoaderManager.LoaderCallbacks<LikeCommentLoader.Result> {

        private CommentsList mCommentsList;

        public LikeLoaderCallbacks(CommentsList commentsList) {
            mCommentsList = commentsList;
        }

        @Override
        public Loader<LikeCommentLoader.Result> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_LIKE:
                    return new LikeCommentLoader(mCommentsList.getActivity(),
                            mCommentsList.getClient(),
                            args.getString(ARG_SID),
                            args.getString(ARG_ESID),
                            args.getBoolean(ARG_DO_LIKE));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<LikeCommentLoader.Result> loader, LikeCommentLoader.Result data) {
            switch (loader.getId()) {
                case LOADER_LIKE:
                    mCommentsList.onLikeResult(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<LikeCommentLoader.Result> loader) {
            // NO-OP
        }
    }
}
