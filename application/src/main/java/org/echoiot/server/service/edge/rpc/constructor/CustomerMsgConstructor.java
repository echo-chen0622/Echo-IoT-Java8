package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.gen.edge.v1.CustomerUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@TbCoreComponent
public class CustomerMsgConstructor {

    @NotNull
    public CustomerUpdateMsg constructCustomerUpdatedMsg(UpdateMsgType msgType, @NotNull Customer customer) {
        CustomerUpdateMsg.Builder builder = CustomerUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(customer.getId().getId().getMostSignificantBits())
                .setIdLSB(customer.getId().getId().getLeastSignificantBits())
                .setTitle(customer.getTitle());
        if (customer.getCountry() != null) {
            builder.setCountry(customer.getCountry());
        }
        if (customer.getState() != null) {
            builder.setState(customer.getState());
        }
        if (customer.getCity() != null) {
            builder.setCity(customer.getCity());
        }
        if (customer.getAddress() != null) {
            builder.setAddress(customer.getAddress());
        }
        if (customer.getAddress2() != null) {
            builder.setAddress2(customer.getAddress2());
        }
        if (customer.getZip() != null) {
            builder.setZip(customer.getZip());
        }
        if (customer.getPhone() != null) {
            builder.setPhone(customer.getPhone());
        }
        if (customer.getEmail() != null) {
            builder.setEmail(customer.getEmail());
        }
        if (customer.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(customer.getAdditionalInfo()));
        }
        return builder.build();
    }

    @NotNull
    public CustomerUpdateMsg constructCustomerDeleteMsg(@NotNull CustomerId customerId) {
        return CustomerUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(customerId.getId().getMostSignificantBits())
                .setIdLSB(customerId.getId().getLeastSignificantBits()).build();
    }
}
