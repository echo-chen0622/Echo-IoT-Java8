package org.thingsboard.server.common.msg.queue;

public interface TbCallback {

    TbCallback EMPTY = new TbCallback() {

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(Throwable t) {

        }
    };

    void onSuccess();

    void onFailure(Throwable t);

}
