package net.pemiridosa.combind.config;

public class CombindConfigData {
    /** When {@code true} (default), all bindings sharing a combo fire simultaneously. */
    public boolean allowConflicts = true;

    /** Maximum ms between presses for a sequence to register in-game. */
    public long sequenceWindowMs = 400L;

    /** Maximum ms between presses during combo recording in the Controls screen. */
    public long sequenceRecordingWindowMs = 600L;
}
