package com.sentinel.aiops.service.command;

/**
 * <b>Command pattern.</b> Encapsulates an operation as an object, decoupling the
 * caller (controller) from the receiver (domain logic) and enabling cross-cutting
 * concerns — logging, timing, history — to be applied uniformly by the invoker.
 */
public interface IncidentCommand<T> {
    T execute();
    String describe();
}
