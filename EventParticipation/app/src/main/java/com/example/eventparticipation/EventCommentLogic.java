package com.example.eventparticipation;

/**
 * Pure logic for event comment validation and ownership checks.
 *
 * <p>Implemented user stories:</p>
 * <ul>
 *     <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
 *     <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 * </ul>
 */
final class EventCommentLogic {

    private EventCommentLogic() {
    }

    /**
     * Returns a trimmed comment body.
     *
     * @param text raw comment text
     * @return trimmed comment text, or an empty string if null
     */
    static String normalizeCommentText(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * Checks whether a comment body is valid for posting.
     *
     * @param text raw comment text
     * @return true if the comment contains non-whitespace text
     */
    static boolean isCommentTextValid(String text) {
        return !normalizeCommentText(text).isEmpty();
    }

    /**
     * Resolves a safe author display name.
     *
     * @param entrantName raw entrant name
     * @return display name for the comment author
     */
    static String resolveAuthorName(String entrantName) {
        String normalizedName = entrantName == null ? "" : entrantName.trim();
        return normalizedName.isEmpty() ? "Anonymous entrant" : normalizedName;
    }

    /**
     * Checks whether the current entrant may delete the given comment.
     *
     * @param currentEntrantId current entrant id
     * @param comment comment to evaluate
     * @return true if the current entrant owns the comment
     */
    static boolean canDeleteComment(String currentEntrantId, EventComment comment) {
        if (currentEntrantId == null || currentEntrantId.trim().isEmpty() || comment == null) {
            return false;
        }

        String authorId = comment.getEntrantId();
        return authorId != null && authorId.equals(currentEntrantId);
    }
}
