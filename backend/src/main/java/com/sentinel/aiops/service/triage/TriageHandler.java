package com.sentinel.aiops.service.triage;

/** A single link in the triage chain. */
public interface TriageHandler {
    /** Set the next handler; returns the argument to allow fluent chaining. */
    TriageHandler setNext(TriageHandler next);

    /** Process the context and (optionally) forward down the chain. */
    void handle(TriageContext ctx);
}
