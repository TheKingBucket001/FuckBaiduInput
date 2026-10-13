package com.fuckbaiduinput;

/** Main-thread request identity for a preference page; no Android objects are retained. */
final class PreferenceCommitSession {
    private long nextToken;
    private long pendingToken;
    private boolean closed;

    long begin() {
        if (closed || pendingToken != 0L) {
            return 0L;
        }
        pendingToken = ++nextToken;
        return pendingToken;
    }

    boolean complete(long token, boolean ownerAlive) {
        if (!ownerAlive) {
            close();
            return false;
        }
        if (closed || token == 0L || token != pendingToken) {
            return false;
        }
        pendingToken = 0L;
        return true;
    }

    void close() {
        closed = true;
        pendingToken = 0L;
    }
}
