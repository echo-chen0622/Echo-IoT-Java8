package org.echoiot.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.send.SendListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

@Slf4j
public class LwM2mServerListener {

    private final LwM2mUplinkMsgHandler service;

    public LwM2mServerListener(LwM2mUplinkMsgHandler service) {
        this.service = service;
    }

    public final RegistrationListener registrationListener = new RegistrationListener() {
        /**
         * Register – query represented as POST /rd?…
         */
        @Override
        public void registered(@NotNull Registration registration, Registration previousReg,
                               Collection<Observation> previousObservations) {
            log.debug("Client: registered: [{}]", registration.getEndpoint());
            service.onRegistered(registration, previousObservations);
        }

        /**
         * Update – query represented as CoAP POST request for the URL received in response to Register.
         */
        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                            Registration previousRegistration) {
            service.updatedReg(updatedRegistration);
        }

        /**
         * De-register (CoAP DELETE) – Sent by the client when a shutdown procedure is initiated.
         */
        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            service.unReg(registration, observations);
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {
        @Override
        public void onSleeping(@NotNull Registration registration) {
            log.info("[{}] onSleeping", registration.getEndpoint());
            service.onSleepingDev(registration);
        }

        @Override
        public void onAwake(@NotNull Registration registration) {
            log.info("[{}] onAwake", registration.getEndpoint());
            service.onAwakeDev(registration);
        }
    };

    public final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(@NotNull Observation observation) {
            //TODO: should be able to use CompositeObservation
            log.trace("Canceled Observation {}.", ((SingleObservation)observation).getPath());
        }

        @Override
        public void onResponse(@NotNull SingleObservation observation, @Nullable Registration registration, ObserveResponse response) {
            if (registration != null) {
                service.onUpdateValueAfterReadResponse(registration, convertObjectIdToVersionedId(observation.getPath().toString(), registration), response);
            }
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration, ObserveCompositeResponse response) {
            throw new RuntimeException("Not implemented yet!");
        }

        @Override
        public void onError(@NotNull Observation observation, Registration registration, @Nullable Exception error) {
            if (error != null) {
                //TODO: should be able to use CompositeObservation
                log.debug("Unable to handle notification of [{}:{}] [{}]", observation.getRegistrationId(), ((SingleObservation)observation).getPath(), error.getMessage());
            }
        }

        @Override
        public void newObservation(@NotNull Observation observation, Registration registration) {
            //TODO: should be able to use CompositeObservation
            log.trace("Successful start newObservation {}.", ((SingleObservation)observation).getPath());
        }
    };

    public final SendListener sendListener = new SendListener() {

        @Override
        public void dataReceived(@Nullable Registration registration, Map<String, LwM2mNode> map, SendRequest sendRequest) {
            if (registration != null) {
                service.onUpdateValueWithSendRequest(registration, sendRequest);
            }
        }
    };
}
