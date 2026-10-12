package com.territorypaint.server.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    public void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    public void testRegisterAndRetrieveSession() {
        Session session = new Session(100L, "alice", 1500L, null, null);

        assertTrue(sessionManager.registerSession(session));
        assertEquals(session, sessionManager.getSessionBySessionId(session.getSessionId()));
        assertEquals(session, sessionManager.getSessionByUserId(100L));
    }

    @Test
    public void testDuplicateLoginRejected() {
        Session s1 = new Session(100L, "alice", 1500L, null, null);
        Session s2 = new Session(100L, "alice", 1500L, null, null);

        assertTrue(sessionManager.registerSession(s1));
        assertFalse(sessionManager.registerSession(s2)); // Duplicate userId rejected
    }

    @Test
    public void testRemoveSession() {
        Session session = new Session(100L, "alice", 1500L, null, null);
        sessionManager.registerSession(session);

        assertTrue(sessionManager.removeSession(session));
        assertNull(sessionManager.getSessionBySessionId(session.getSessionId()));
        assertNull(sessionManager.getSessionByUserId(100L));
    }

    @Test
    public void testRemoveNonExistentOrMismatchedSession() {
        Session s1 = new Session(100L, "alice", 1500L, null, null);
        Session s2 = new Session(100L, "alice", 1500L, null, null);

        sessionManager.registerSession(s1);

        assertFalse(sessionManager.removeSession(s2)); // Mismatched sessionId
        assertNotNull(sessionManager.getSessionByUserId(100L));
    }
}
