package com.sentinel.aiops.integration.health;

/**
 * <b>Template Method pattern.</b> Fixes the probe algorithm — time it, run the
 * subclass check, normalize failures to DOWN — while subclasses supply only the
 * integration-specific check.
 */
public abstract class AbstractIntegrationProbe implements IntegrationProbe {

    @Override
    public final IntegrationStatus probe() {
        long start = System.currentTimeMillis();
        try {
            Result r = check();
            return new IntegrationStatus(name(), category(), r.state(), r.detail(),
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            return new IntegrationStatus(name(), category(), IntegrationState.DOWN,
                    e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    protected abstract String name();
    protected abstract String category();
    protected abstract Result check();

    protected record Result(IntegrationState state, String detail) {}
}
