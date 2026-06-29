package com.sentinel.aiops.service.triage;

/**
 * <b>Chain of Responsibility</b> + <b>Template Method.</b>
 *
 * <p>Chain of Responsibility: each handler does its piece of triage and forwards
 * to the next link. Template Method: the {@link #handle} algorithm is fixed here
 * (run this step, then delegate to the next), while subclasses fill in only the
 * {@link #doHandle} step.</p>
 */
public abstract class AbstractTriageHandler implements TriageHandler {

    private TriageHandler next;

    @Override
    public TriageHandler setNext(TriageHandler next) {
        this.next = next;
        return next; // enables fluent chaining
    }

    /** Template method — invariant skeleton, not overridden by subclasses. */
    @Override
    public final void handle(TriageContext ctx) {
        doHandle(ctx);
        if (next != null) {
            next.handle(ctx);
        }
    }

    /** The single varying step each concrete handler implements. */
    protected abstract void doHandle(TriageContext ctx);
}
