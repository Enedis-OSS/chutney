/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.engine.domain.execution;


import com.chutneytesting.engine.domain.execution.event.Event;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton event bus that allows objects to subscribe to and post events.
 * It uses RxJava's `Subject` to allow multiple subscribers to listen to events and respond accordingly.
 * <p>
 * This implementation ensures thread-safety and provides a robust mechanism for error handling.
 *
 * <p>It includes:</p>
 * <ul>
 *     <li>Posting events to the bus.</li>
 *     <li>Subscribing to events of a specific type.</li>
 *     <li>Subscribing to events of a specific type with a filter on the execution ID.</li>
 *     <li>Exposing the bus as an Observable to allow further subscriptions.</li>
 * </ul>
 *
 * <p>Additionally, error handling is provided in the subscription process to log any issues.</p>
 */
public class RxBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(RxBus.class);
    private static final RxBus INSTANCE = new RxBus();

    public static RxBus getInstance() {
        return INSTANCE;
    }

    private final Subject<Object> bus = PublishSubject.create().toSerialized();

    /**
     * Posts an event to the bus, notifying all subscribers that a new event has occurred.
     *
     * <p>This method is the producer of the bus, and the event can be consumed by any
     * active subscriber that has registered for the event type.</p>
     *
     * @param event The event to post to the bus.
     */
    public void post(Object event) {
        try {
            bus.onNext(event);  // Publishes the event to the bus
        } catch (Exception e) {
            LOGGER.error("Error while posting event: {}", event.getClass().getSimpleName(), e);
        }
    }

    /**
     * Registers a subscriber for a specific event type. The subscriber will receive events
     * of the specified class type.
     *
     * <p>Subscribers are notified asynchronously when an event of the given class type
     * is posted to the bus. The method ensures that only events of the requested type
     * are passed to the subscriber.</p>
     *
     * @param <T>        The type of event to subscribe to.
     * @param eventClass The class type of the event.
     * @param onNext     The action to perform when the event is received.
     * @return A Disposable object that can be used to unsubscribe from the bus.
     */
    public <T> Disposable register(final Class<T> eventClass, Consumer<T> onNext) {
        return bus
            .filter(eventClass::isInstance)  // Filters events of the specified class type
            .map(eventClass::cast)  // Safely casts the event to the expected class
            .subscribe(
                onNext,  // Action to execute when the event is received
                throwable -> LOGGER.error("Error while processing event: {}", eventClass.getSimpleName(), throwable)  // Error handling
            );
    }

    /**
     * Registers a subscriber for a specific event type and execution ID. The subscriber
     * will only receive events of the specified class type that match the given execution ID.
     *
     * <p>This method is useful for filtering events by a specific execution context, ensuring
     * that subscribers only receive relevant events related to their execution ID.</p>
     *
     * @param <T>         The type of event to subscribe to.
     * @param eventClass  The class type of the event.
     * @param executionId The execution ID to filter the events.
     * @param onNext      The action to perform when the event is received.
     * @return A Disposable object that can be used to unsubscribe from the bus.
     */
    public <T extends Event> Disposable registerOnExecutionId(final Class<T> eventClass, long executionId, Consumer<? super Event> onNext) {
        return bus
            .filter(event -> event.getClass().equals(eventClass))  // Filters events by the exact class type
            .map(obj -> (T) obj)  // Safely casts the event to the expected class
            .filter(e -> e.executionId() == executionId)  // Filters by execution ID
            .subscribe(
                onNext,  // Action to execute when the event is received
                throwable -> LOGGER.error("Error while processing event for executionId {}: {}", executionId, throwable.getMessage(), throwable)  // Error handling
            );
    }

    /**
     * Exposes the event bus as an Observable, allowing subscribers to listen to all events.
     *
     * <p>This is useful if you want to observe all events on the bus without filtering by event type.</p>
     *
     * @return An Observable that emits all events posted to the bus.
     */
    public Observable<Object> toObservable() {
        return bus;
    }
}


