package org.thingsboard.server.common.transport;

/**
 * Created by ashvayka on 04.10.18.
 */
public interface TransportServiceCallback<T> {

    TransportServiceCallback<Void> EMPTY = new TransportServiceCallback<Void>() {
        @Override
        public void onSuccess(Void msg) {

        }

        @Override
        public void onError(Throwable e) {

        }
    };

    void onSuccess(T msg);

    void onError(Throwable e);

}
