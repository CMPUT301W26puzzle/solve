package com.example.eventparticipation.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.Comment;
import com.example.eventparticipation.universal.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified discussion board for entrants, organizers, and administrators.
 *
 * <p><b>Purpose & Role:</b> Acts as the central discussion hub. The UI
 * dynamically adjusts permissions based on roles:</p>
 * <ul>
 * <li><b>Entrants:</b> Can view and post. Can only delete their own comments.</li>
 * <li><b>Organizers:</b> Can view, post with a tag, and delete any comment for their event.</li>
 * <li><b>Admins:</b> Can view and delete any comment, but the input field is hidden.</li>
 * </ul>
 *
 * <p>Implemented user stories:</p>
 * <ul>
 * <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
 * <li>US 02.08.02 As an organizer, I want to comment on my events.</li>
 * <li>US 03.10.01 As an administrator, I want to remove event comments.</li>
 * </ul>
 */
public class EventCommentsActivity extends AppCompatActivity {

    private String eventId;
    private String currentUserId;
    private boolean isOrganizer;
    private boolean isAdmin;
    private FirebaseFirestore db;
    private CommentAdapter adapter;
    private List<Comment> commentList;

    private LinearLayout layoutCommentInput;
    private EditText etCommentInput;
    private ImageButton btnSendComment;
    private RecyclerView recyclerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_comments);

        // Permissions logic
        eventId = getIntent().getStringExtra("EVENT_ID");
        isOrganizer = getIntent().getBooleanExtra("IS_ORGANIZER", false);
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        SessionManager session = SessionManager.getInstance(this);
        currentUserId = session.getUserId();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutCommentInput = findViewById(R.id.layoutCommentInput);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);
        recyclerView = findViewById(R.id.recyclerViewComments);

        // Admin-specific UI rule: moderation only
        if (isAdmin && layoutCommentInput != null) {
            layoutCommentInput.setVisibility(View.GONE);
        }

        commentList = new ArrayList<>();
        // CommentAdapter handles role-based delete button visibility
        adapter = new CommentAdapter(commentList, currentUserId, isOrganizer, isAdmin, this::confirmDeleteComment);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSendComment.setOnClickListener(v -> postComment());

        loadComments();
    }

    /**
     * Listens for real-time Firestore updates in the comments sub-collection.
     */
    private void loadComments() {
        if (eventId == null) return;

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

    /**
     * Validates input and pushes a new Comment object to Firestore.
     * Appends an "(Organizer)" suffix if the user has organizer status.
     */
    private void postComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        db.collection("entrants").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = "Anonymous";
                    if (doc.exists() && doc.contains("name")) {
                        name = doc.getString("name");
                    }
                    if (isOrganizer) name += " (Organizer)";

                    Comment newComment = new Comment(eventId, currentUserId, name, text);
                    db.collection("events").document(eventId).collection("comments").add(newComment)
                            .addOnSuccessListener(docRef -> etCommentInput.setText(""));
                });
    }

    /**
     * Helper: Displays a confirmation dialog before deleting a comment.
     * @param comment The comment instance to delete.
     */
    private void confirmDeleteComment(Comment comment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(eventId).collection("comments").document(comment.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}