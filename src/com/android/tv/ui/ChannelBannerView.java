// Copyright 2014 Google Inc. All Rights Reserved.

package com.android.tv.ui;

import com.android.tv.ChannelMap;
import com.android.tv.Program;
import com.android.tv.R;
import com.android.tv.Utils;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.TvContract;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.tv.TvInputInfo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A view to render channel banner.
 */
public class ChannelBannerView extends LinearLayout {

    private static final String TAG = "ChannelBannerView";

    private TextView mResolutionTextView;
    private TextView mAspectRatioTextView;
    private ProgressBar mRemainingTimeView;
    private LinearLayout mProgramInfoContainer;
    private TextView mProgrameDescriptionTextView;
    private TextView mChannelTextView;
    private TextView mProgramTextView;
    private TextView mProgramTimeTextView;
    private Uri mCurrentChannelUri;

    private final ContentObserver mProgramUpdateObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateProgramInfo();
        }
    };

    public ChannelBannerView(Context context) {
        super(context);
        mContext = context;
    }

    public ChannelBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public ChannelBannerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().getContentResolver().registerContentObserver(TvContract.Programs.CONTENT_URI,
                true, mProgramUpdateObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        getContext().getContentResolver().unregisterContentObserver(mProgramUpdateObserver);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mResolutionTextView = (TextView) findViewById(R.id.resolution);
        mAspectRatioTextView = (TextView) findViewById(R.id.aspect_ratio);
        mRemainingTimeView = (ProgressBar) findViewById(R.id.remaining_time);
        mChannelTextView = (TextView) findViewById(R.id.channel_text);
        mProgramTimeTextView = (TextView) findViewById(R.id.program_time_text);
        mProgramInfoContainer = (LinearLayout) findViewById(R.id.program_info);
        mProgrameDescriptionTextView = (TextView) findViewById(R.id.program_description);
        mProgramTextView = (TextView) findViewById(R.id.program_text);
    }

    public void updateViews(ChannelMap channelMap) {
        if (channelMap == null || !channelMap.isLoadFinished()) {
            return;
        }

        mResolutionTextView.setVisibility(View.GONE);
        mAspectRatioTextView.setVisibility(View.GONE);
        mProgrameDescriptionTextView.setVisibility(View.GONE);

        String displayNumber = channelMap.getCurrentDisplayNumber();
        String displayName = channelMap.getCurrentDisplayName();
        if (displayNumber == null) {
            displayNumber = "";
        }
        if (displayName == null) {
            displayName = "";
        }
        mChannelTextView.setText(Html.fromHtml(mContext.getString(
                R.string.channel_banner_channel_title, displayNumber, displayName)));
        TvInputInfo info = channelMap.getTvInputInfo();

        mCurrentChannelUri = channelMap.getCurrentChannelUri();
        updateProgramInfo();
    }

    private String getFormattedTimeString(long time) {
        return DateFormat.format(
                getContext().getString(R.string.channel_banner_time_format), time).toString();
    }

    public void updateProgramInfo() {
        if (mCurrentChannelUri == null) {
            hideProgramInformation();
            return;
        }

        Program program = Utils.getCurrentProgram(mContext, mCurrentChannelUri);
        if (program == null) {
            hideProgramInformation();
            return;
        }
        if (!TextUtils.isEmpty(program.getTitle())) {
            mProgramInfoContainer.setVisibility(View.VISIBLE);
            mProgramTextView.setText(program.getTitle());

            long startTime = program.getStartTimeUtcMillis();
            long endTime = program.getEndTimeUtcMillis();
            if (startTime > 0 && endTime > 0) {
                mProgramTimeTextView.setVisibility(View.VISIBLE);
                mRemainingTimeView.setVisibility(View.VISIBLE);

                String startTimeText = getFormattedTimeString(startTime);
                String endTimeText = getFormattedTimeString(endTime);

                mProgramTimeTextView.setText(mContext.getString(
                        R.string.channel_banner_program_time_format, startTimeText, endTimeText));

                long currTime = System.currentTimeMillis();
                if (currTime <= startTime) {
                    mRemainingTimeView.setProgress(0);
                } else if (currTime >= endTime) {
                    mRemainingTimeView.setProgress(100);
                } else {
                    mRemainingTimeView.setProgress(
                            (int) (100 *(currTime - startTime) / (endTime - startTime)));
                }
            } else {
                mProgramTimeTextView.setVisibility(View.INVISIBLE);
                mRemainingTimeView.setVisibility(View.INVISIBLE);
            }
        } else {
            hideProgramInformation();
        }
    }

    private void hideProgramInformation() {
        mProgramInfoContainer.setVisibility(View.INVISIBLE);
        mProgramTimeTextView.setVisibility(View.INVISIBLE);
        mRemainingTimeView.setVisibility(View.GONE);
    }
}
