package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.CustomerId;

public interface CustomerAwareMsg {

	CustomerId getCustomerId();

}
