package org.thingsboard.server.dao.device.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.Customer;

@Data
@AllArgsConstructor
public class ReclaimResult {
    Customer unassignedCustomer;
}
