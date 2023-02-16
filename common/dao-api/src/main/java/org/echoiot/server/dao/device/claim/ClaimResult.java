package org.echoiot.server.dao.device.claim;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.Device;

@AllArgsConstructor
@Data
public class ClaimResult {

    private Device device;
    private ClaimResponse response;

}
