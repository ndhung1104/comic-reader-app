package com.group09.ComicReader.ui.share;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group09.ComicReader.R;
import com.group09.ComicReader.databinding.FragmentShareSheetBinding;
import com.group09.ComicReader.share.ShareActionExecutor;
import com.group09.ComicReader.share.ShareContent;
import com.group09.ComicReader.share.ShareUrlValidator;

public class ShareSheetBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ShareSheetBottomSheet";

    private static final String ARG_TYPE = "share_type";
    private static final String ARG_TITLE = "share_title";
    private static final String ARG_URL = "share_url";
    private static final String ARG_CHAPTER_NUMBER = "share_chapter_number";

    private FragmentShareSheetBinding binding;

    @NonNull
    public static ShareSheetBottomSheetFragment newComicInstance(@NonNull String title, @NonNull String url) {
        return newInstance(ShareContent.TYPE_COMIC, title, url, 0);
    }

    @NonNull
    public static ShareSheetBottomSheetFragment newChapterInstance(
            @NonNull String title,
            @NonNull String url,
            int chapterNumber) {
        return newInstance(ShareContent.TYPE_CHAPTER, title, url, chapterNumber);
    }

    @NonNull
    private static ShareSheetBottomSheetFragment newInstance(
            @NonNull String type,
            @NonNull String title,
            @NonNull String url,
            int chapterNumber) {
        ShareSheetBottomSheetFragment fragment = new ShareSheetBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_URL, url);
        args.putInt(ARG_CHAPTER_NUMBER, chapterNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentShareSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ShareContent shareContent = readShareContent();
        if (shareContent == null) {
            dismissAllowingStateLoss();
            return;
        }

        binding.btnShareSheetClose.setOnClickListener(v -> dismiss());
        bindPreview(shareContent);
        bindActions(shareContent);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void bindPreview(@NonNull ShareContent shareContent) {
        binding.tvShareSheetTitle.setText(
                shareContent.isChapter() ? R.string.share_sheet_title_chapter : R.string.share_sheet_title_comic);
        binding.tvShareSheetSubtitle.setText(R.string.share_sheet_subtitle);
        binding.tvShareSheetPreviewTitle.setText(shareContent.getTitle());
        binding.tvShareSheetPreviewCaption.setText(shareContent.buildCaption(
                getString(R.string.share_comic_caption_short),
                getString(R.string.share_chapter_caption_short),
                getString(R.string.share_chapter_caption_short_fallback)
        ));
        binding.tvShareSheetPreviewLink.setText(shareContent.getUrl());
        binding.tvShareSheetWarning.setVisibility(
                ShareUrlValidator.needsLocalWarning(shareContent.getUrl()) ? View.VISIBLE : View.GONE);
        binding.tvShareSheetPlatformHint.setText(R.string.share_sheet_platform_hint);
    }

    private void bindActions(@NonNull ShareContent shareContent) {
        binding.btnShareSheetCopyLink.setOnClickListener(v -> {
            ShareActionExecutor.copyLink(requireContext(), shareContent);
            dismiss();
        });
        binding.btnShareSheetMoreApps.setOnClickListener(v -> {
            ShareActionExecutor.openSystemShare(requireContext(), shareContent);
            dismiss();
        });
        binding.btnShareSheetFacebook.setOnClickListener(v -> {
            ShareActionExecutor.openFacebookShare(requireContext(), shareContent);
            dismiss();
        });
        binding.btnShareSheetZalo.setOnClickListener(v -> {
            ShareActionExecutor.openZaloShare(requireContext(), shareContent);
            dismiss();
        });
        binding.btnShareSheetDiscord.setOnClickListener(v -> {
            ShareActionExecutor.openDiscordShare(requireContext(), shareContent);
            dismiss();
        });
        binding.btnShareSheetTelegram.setOnClickListener(v -> {
            ShareActionExecutor.openTelegramShare(requireContext(), shareContent);
            dismiss();
        });
        binding.btnShareSheetPreviewPage.setOnClickListener(v -> {
            ShareActionExecutor.openPreviewPage(requireContext(), shareContent);
            dismiss();
        });
    }

    @Nullable
    private ShareContent readShareContent() {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }
        String type = args.getString(ARG_TYPE, ShareContent.TYPE_COMIC);
        String title = args.getString(ARG_TITLE, getString(R.string.app_name));
        String url = args.getString(ARG_URL, "");
        int chapterNumber = args.getInt(ARG_CHAPTER_NUMBER, 0);
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return new ShareContent(type, title, url, chapterNumber);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
