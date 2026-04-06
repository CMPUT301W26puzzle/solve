package com.example.eventparticipation;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated activity for Organizers and Administrators to view, post, and moderate event comments.
 *
 * <p><b>Purpose & Role:</b> Acts as the primary moderation hub for an event's discussion board.
 * Organizers use it to post official replies and delete spam. Admins use it strictly for
 * platform moderation (post-creation is disabled for Admins).</p>
 *
 * <p>Implemented user stories:</p>
 * <ul>
 * <li>US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.</li>
 * <li>US 02.08.02 As an organizer, I want to comment on my events.</li>
 * <li>US 03.10.01 As an administrator, I want to remove event comments that violate app policy.</li>
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

        // Admins are there to moderate, hide the input box
        if (isAdmin && layoutCommentInput != null) {
            layoutCommentInput.setVisibility(View.GONE);
        } fix

        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList, currentUserId, isOrganizer, isAdmin, this::confirmDeleteComment);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSendComment.setOnClickListener(v -> postComment());

        loadComments();
    }

    /**
     * Attaches a real-time Firestore listener to the event's comments sub-collection.
     * Updates the RecyclerView automatically when comments are added or removed.
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
     * Fetches the current user's profile to resolve their name, appends an "(Organizer)"
     * tag if applicable, and pushes the new comment to Firestore.
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
     * Displays an alert dialog confirming the user's intent to delete a comment.
     *
     * @param comment The comment scheduled for deletion.
     */
    private void confirmDeleteComment(Comment comment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(eventId).collection("comments").document(comment.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}