package org.echoiot.server.transport.snmp;

import org.jetbrains.annotations.NotNull;
import org.snmp4j.*;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class SnmpDeviceSimulatorV2 extends BaseAgent {

    public static class RequestProcessor extends CommandProcessor {
        private final Consumer<CommandResponderEvent> processor;

        public RequestProcessor(Consumer<CommandResponderEvent> processor) {
            super(new OctetString(MPv3.createLocalEngineID()));
            this.processor = processor;
        }

        @Override
        public void processPdu(CommandResponderEvent event) {
            processor.accept(event);
        }
    }


    @NotNull
    private final Target target;
    private final Address address;
    private Snmp snmp;

    @NotNull
    private final String password;

    public SnmpDeviceSimulatorV2(int port, @NotNull String password) throws IOException {
        super(new File("conf.agent"), new File("bootCounter.agent"), new RequestProcessor(event -> {
            System.out.println("aboba");
            ((Snmp) event.getSource()).cancel(event.getPDU(), event1 -> System.out.println("canceled"));
        }));
        @NotNull CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(password));
        this.address = GenericAddress.parse("udp:0.0.0.0/" + port);
        target.setAddress(address);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        this.target = target;
        this.password = password;
    }

    public void start() throws IOException {
        init();
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
        snmp = new Snmp(transportMappings[0]);
    }

    public void setUpMappings(@NotNull Map<String, String> oidToResponseMappings) {
        unregisterManagedObject(getSnmpv2MIB());
        oidToResponseMappings.forEach((oid, response) -> {
            registerManagedObject(new MOScalar<>(new OID(oid), MOAccessImpl.ACCESS_READ_WRITE, new OctetString(response)));
        });
    }

    public void sendTrap(String host, int port, @NotNull Map<String, String> values) throws IOException {
        @NotNull PDU pdu = new PDU();
        pdu.addAll(values.entrySet().stream()
                .map(entry -> new VariableBinding(new OID(entry.getKey()), new OctetString(entry.getValue())))
                .collect(Collectors.toList()));
        pdu.setType(PDU.TRAP);

        CommunityTarget remoteTarget = (CommunityTarget) getTarget().clone();
        remoteTarget.setAddress(new UdpAddress(host + "/" + port));

        snmp.send(pdu, remoteTarget);
    }

    @Override
    protected void registerManagedObjects() {
    }

    protected void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void unregisterManagedObject(@NotNull MOGroup moGroup) {
        moGroup.unregisterMOs(server, getContext(moGroup));
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB,
                                          SnmpNotificationMIB notificationMIB) {
    }

    @Override
    protected void addViews(@NotNull VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(
                        "cpublic"), new OctetString("v1v2group"),
                StorageType.nonVolatile);

        vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
                new OctetString("fullWriteView"), new OctetString(
                        "fullNotifyView"), StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
    }

    protected void addUsmUser(USM usm) {
    }

    @SuppressWarnings({"unchecked"})
    protected void initTransportMappings() {
        transportMappings = new TransportMapping[]{TransportMappings.getInstance().createTransportMapping(address)};
    }

    protected void unregisterManagedObjects() {
    }

    protected void addCommunities(@NotNull SnmpCommunityMIB communityMIB) {
        @NotNull Variable[] com2sec = new Variable[]{
                new OctetString("public"),
                new OctetString("cpublic"),
                getAgent().getContextEngineID(),
                new OctetString("public"),
                new OctetString(),
                new Integer32(StorageType.nonVolatile),
                new Integer32(RowStatus.active)
        };
        SnmpCommunityMIB.SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry().createRow(
                new OctetString("public2public").toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }

    public Target getTarget() {
        return target;
    }

}
