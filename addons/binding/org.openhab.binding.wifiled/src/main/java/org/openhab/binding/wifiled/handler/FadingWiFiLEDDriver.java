/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wifiled.handler;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link FadingWiFiLEDDriver} class is responsible for the communication with the WiFi LED controller.
 * It utilizes color fading when changing colors or turning the light on of off.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FadingWiFiLEDDriver extends AbstractWiFiLEDDriver {

    public static final int DEFAULT_FADE_DURATION_IN_MS = 1000;
    public static final int DEFAULT_FADE_STEPS = 100;

    private boolean power = false;
    private InternalLedState blackState = new InternalLedState();
    private InternalLedState currentState = new InternalLedState();
    private InternalLedState targetState = new InternalLedState();
    private InternalLedState realTargetState = new InternalLedState();
    private LEDStateDTO dtoState = LEDStateDTO.valueOf(0, 0, 0, 0, 0, 0, 0);
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final int fadeDurationInMs;
    private final int fadeSteps;
    private boolean keepFading = false;

    public FadingWiFiLEDDriver(String host, int port, AbstractWiFiLEDDriver.Protocol protocol, int fadeDurationInMs, int fadeSteps) {
        super(host, port, protocol);
        this.fadeDurationInMs = fadeDurationInMs;
        this.fadeSteps = fadeSteps;
    }

    @Override
    public void init() throws IOException {
        try {
            LEDState s = getLEDState();
            dtoState = LEDStateDTO.valueOf(s.state, s.program, s.programSpeed, s.red, s.green, s.blue, s.white);
            power = (s.state & 0x01) != 0;
            currentState = InternalLedState.fromRGBW(s.red, s.green, s.blue, s.white);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void setColor(HSBType color) throws IOException {
        dtoState = dtoState.withColor(color);
        changeState(targetState.withColor(color));
    }

    @Override
    public void setBrightness(PercentType brightness) throws IOException {
        dtoState = dtoState.withBrightness(brightness);
        changeState(targetState.withBrightness(brightness.doubleValue() / 100));
    }

    @Override
    public void incBrightness(int step) throws IOException {
        dtoState = dtoState.withIncrementedBrightness(step);
        changeState(targetState.withBrightness(currentState.getBrightness() + ((double) step / 100)));
    }

    @Override
    public void decBrightness(int step) throws IOException {
        dtoState = dtoState.withIncrementedBrightness(-step);
        changeState(targetState.withBrightness(currentState.getBrightness() - ((double) step / 100)));
    }

    @Override
    public void setWhite(PercentType white) throws IOException {
        dtoState = dtoState.withWhite(white);
        changeState(targetState.withWhite(white.doubleValue() / 100));
    }

    @Override
    public void incWhite(int step) throws IOException {
        dtoState = dtoState.withIncrementedWhite(step);
        changeState(targetState.withWhite(currentState.getWhite() + ((double) step / 100)));
    }

    @Override
    public void decWhite(int step) throws IOException {
        dtoState = dtoState.withIncrementedWhite(-step);
        changeState(targetState.withWhite(currentState.getWhite() - ((double) step / 100)));
    }

    @Override
    public void setProgram(StringType program) throws IOException {
    }

    @Override
    public void setProgramSpeed(PercentType speed) throws IOException {
    }

    @Override
    public void incProgramSpeed(int step) throws IOException {
    }

    @Override
    public void setPower(OnOffType command) throws IOException {
        dtoState = dtoState.withPower(command);
        power = command == OnOffType.ON;
        fadeToState(power ? targetState : blackState);
    }

    @Override
    public LEDStateDTO getLEDStateDTO() throws IOException {
        return dtoState;
    }

    private void changeState(final InternalLedState newState) throws IOException {
        targetState = newState;
        if (power) {
            fadeToState(targetState);
        }
    }

    private void fadeToState(final InternalLedState newState) throws IOException {
        if (!newState.equals(realTargetState)) {
            keepFading = false;
            realTargetState = newState;

            executorService.schedule(() -> {
                if (currentState.equals(newState)) return;

                keepFading = true;

                try (Socket socket = new Socket(host, port)) {
                    logger.debug("Connected to '{}'", socket);

                    socket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);

                    try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                        InternalLedState fadeState = currentState;

                        for (int i = 1; i <= fadeSteps && keepFading; i++) {
                            long lastTime = System.nanoTime();
                            fadeState = currentState.fade(newState, (double) i / fadeSteps);
                            logger.debug("fadeState: " + fadeState);

                            sendLEDData(fadeState, outputStream);

                            busySleep(fadeDurationInMs / fadeSteps, lastTime);
                        }

                        currentState = fadeState;
                    }
                } catch (NoRouteToHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                    logger.warn("SocketException", e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    private void sendLEDData(InternalLedState ledState, DataOutputStream out) throws IOException {
        logger.debug("Setting LED State to {}", ledState);

        // "normal" program: set color etc.
        byte r = (byte) (ledState.getR() & 0xFF);
        byte g = (byte) (ledState.getG() & 0xFF);
        byte b = (byte) (ledState.getB() & 0xFF);
        byte w = (byte) (ledState.getW() & 0xFF);

        logger.info("RGBW: {}, {}, {}, {}", r, g, b, w);

        byte[] bytes = getBytesForColor(r, g, b, w);
        sendRaw(bytes, out);
    }

    private static void busySleep(final long nanos, final long startTime) {
        //noinspection StatementWithEmptyBody
        while (System.nanoTime() - startTime < nanos * 1000000);
    }

}
