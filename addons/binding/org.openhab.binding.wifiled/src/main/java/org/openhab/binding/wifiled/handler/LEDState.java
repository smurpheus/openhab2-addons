package org.openhab.binding.wifiled.handler;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class LEDState {

    public final int state, program, programSpeed;
    public final int red, green, blue, white;

    public LEDState(int state, int program, int programSpeed, int red, int green, int blue, int white) {
        this.state = state;
        this.program = program;
        this.programSpeed = programSpeed;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.white = white;
    }

}
