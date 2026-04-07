package com.example.eventparticipation.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.eventparticipation.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Full-size image preview used by admin moderation. */
public class ImagePreviewDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_URL = "arg_url";

    public static ImagePreviewDialogFragment newInstance(String title, String imageUrl) {
        ImagePreviewDialogFragment fragment = new ImagePreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_image_preview, null);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        ImageView ivPreview = view.findViewById(R.id.ivPreview);

        Bundle args = getArguments();
        String title = args != null ? args.getString(ARG_TITLE, "Image preview") : "Image preview";
        String imageUrl = args != null ? args.getString(ARG_URL, "") : "";

        tvTitle.setText(title);
        Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(ivPreview);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setPositiveButton("Close", null)
                .create();
    }
}