package org.thingsboard.rule.engine.transform;

public interface TbMsgCallbackWrapper {

    void onSuccess();

    void onFailure(Throwable t);
}
