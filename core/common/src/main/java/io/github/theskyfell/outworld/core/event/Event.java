package io.github.theskyfell.outworld.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A list of listeners for one kind of event. Common code registers listeners; the loader layer
 * forwards the loader's own event to them.
 *
 * @param <L> the listener type
 */
public final class Event<L> {
    private final List<L> listeners = new CopyOnWriteArrayList<>();

    public void register(L listener) {
        listeners.add(listener);
    }

    /** Calls {@code action} for every listener, in registration order. Used by the loader layer. */
    public void forEach(Consumer<? super L> action) {
        for (L listener : listeners) {
            action.accept(listener);
        }
    }
}
