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
import java.net.Socket;
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
    public void setColor(HSBType color) throws IOException {
        changeState(targetState.withColor(color));
    }

    @Override
    public void setBrightness(PercentType brightness) throws IOException {
        changeState(targetState.withBrightness(brightness.doubleValue() / 100));
    }

    @Override
    public void incBrightness(int step) throws IOException {
        changeState(targetState.withBrightness(currentState.getBrightness() + ((double) step / 100)));
    }

    @Override
    public void decBrightness(int step) throws IOException {
        changeState(targetState.withBrightness(currentState.getBrightness() - ((double) step / 100)));
    }

    @Override
    public void setWhite(PercentType white) throws IOException {
        changeState(targetState.withWhite(white.doubleValue() / 100));
    }

    @Override
    public void incWhite(int step) throws IOException {
        changeState(targetState.withWhite(currentState.getWhite() + ((double) step / 100)));
    }

    @Override
    public void decWhite(int step) throws IOException {
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
        power = command == OnOffType.ON;
        fadeToState(power ? targetState : blackState);
    }

    @Override
    public LEDStateDTO getLEDStateDTO() throws IOException {
        InternalLedState s = targetState;

        return LEDStateDTO.valueOf(power ? 0xFF : 0x00, 0, 0, s.getR(), s.getR(), s.getB(), s.getW());
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

            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    if (currentState.equals(newState)) return;

                    keepFading = true;

                    try (Socket socket = new Socket(host, port)) {
                        logger.debug("Connected to '{}'", socket);

                        socket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);

                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                        InternalLedState fadeState = currentState;

                        for (int i = 1; i <= fadeSteps && keepFading; i++) {
                            try {
                                long lastTime = System.nanoTime();
                                fadeState = currentState.fade(newState, (double) i / fadeSteps);
                                logger.debug("fadeState: " + fadeState);

                                sendLEDData(fadeState, outputStream);

                                busySleep(fadeDurationInMs / fadeSteps, lastTime);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        currentState = fadeState;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        byte[] bytes = new byte[]{0x31, r, g, b, w, 0x00};
        sendRaw(bytes, out);
    }

    private static void busySleep(final long nanos, final long startTime) {
        //noinspection StatementWithEmptyBody
        while (System.nanoTime() - startTime < nanos * 1000000);
    }

}
