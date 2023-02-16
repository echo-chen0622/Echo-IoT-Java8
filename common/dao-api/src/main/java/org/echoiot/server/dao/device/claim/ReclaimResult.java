package org.echoiot.server.dao.device.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.Customer;

@Data
@AllArgsConstructor
public class ReclaimResult {
    Customer unassignedCustomer;
}
