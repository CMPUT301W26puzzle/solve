package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for event comment logic.
 *
 * <p>Implemented user stories:</p>
 * <ul>
 *     <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
 *     <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 * </ul>
 */
public class EventCommentLogicTest {

    @Test
    public void normalizeCommentText_null_returnsEmptyString() {
        assertEquals("", EventCommentLogic.normalizeCommentText(null));
    }

    @Test
    public void normalizeCommentText_trimsWhitespace() {
        assertEquals("Hello world", EventCommentLogic.normalizeCommentText("  Hello world  "));
    }

    @Test
    public void isCommentTextValid_empty_returnsFalse() {
        assertFalse(EventCommentLogic.isCommentTextValid(""));
    }

    @Test
    public void isCommentTextValid_whitespaceOnly_returnsFalse() {
        assertFalse(EventCommentLogic.isCommentTextValid("   "));
    }

    @Test
    public void isCommentTextValid_normalText_returnsTrue() {
        assertTrue(EventCommentLogic.isCommentTextValid("Looking forward to this event"));
    }

    @Test
    public void resolveAuthorName_blank_returnsAnonymousEntrant() {
        assertEquals("Anonymous entrant", EventCommentLogic.resolveAuthorName("   "));
    }

    @Test
    public void resolveAuthorName_validName_returnsTrimmedName() {
        assertEquals("Maya", EventCommentLogic.resolveAuthorName("  Maya "));
    }

    @Test
    public void canDeleteComment_sameEntrantId_returnsTrue() {
        EventComment comment = new EventComment();
        comment.setEntrantId("entrant_001");

        assertTrue(EventCommentLogic.canDeleteComment("entrant_001", comment));
    }

    @Test
    public void canDeleteComment_differentEntrantId_returnsFalse() {
        EventComment comment = new EventComment();
        comment.setEntrantId("entrant_002");

        assertFalse(EventCommentLogic.canDeleteComment("entrant_001", comment));
    }

    @Test
    public void canDeleteComment_missingCommentOwner_returnsFalse() {
        EventComment comment = new EventComment();

        assertFalse(EventCommentLogic.canDeleteComment("entrant_001", comment));
    }
}
