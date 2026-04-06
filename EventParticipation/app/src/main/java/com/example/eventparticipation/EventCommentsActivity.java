package com.example.eventparticipation;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class EventCommentsActivity extends AppCompatActivity {

    private String eventId;
    private String currentUserId;
    private boolean isOrganizer;
    private FirebaseFirestore db;
    private CommentAdapter adapter;
    private List<Comment> commentList;

    private EditText etCommentInput;
    private ImageButton btnSendComment;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_comments);

        eventId = getIntent().getStringExtra("EVENT_ID");
        isOrganizer = getIntent().getBooleanExtra("IS_ORGANIZER", false);

        SessionManager session = SessionManager.getInstance(this);
        currentUserId = session.getUserId();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);
        recyclerView = findViewById(R.id.recyclerViewComments);

        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList, isOrganizer, this::deleteComment);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Latest comments at the bottom
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSendComment.setOnClickListener(v -> postComment());

        loadComments();
    }

    private void loadComments() {
        db.collection("events").document(eventId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    commentList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            commentList.add(comment);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!commentList.isEmpty()) {
                        recyclerView.scrollToPosition(commentList.size() - 1);
                    }
                });
    }

    // US 02.08.02: As an organizer, I want to comment on my events.
    private void postComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // Fetch User Name for the comment
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.contains("name") ? doc.getString("name") : "Anonymous";
                    if (isOrganizer) name += " (Organizer)";

                    Comment newComment = new Comment(eventId, currentUserId, name, text);
                    db.collection("events").document(eventId).collection("comments").add(newComment)
                            .addOnSuccessListener(docRef -> etCommentInput.setText(""));
                });
    }

    // US 02.08.01: As an organizer, I want to view and delete entrant comments.
    private void deleteComment(Comment comment) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(eventId).collection("comments").document(comment.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}