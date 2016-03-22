# WiFi LED Binding

This Binding is used to control LED stripes connected by WiFi. These devices are sold with different names, i.e. Magic Home LED, UFO LED, LED NET controller, etc.  

The original add-on code can be found [here](https://github.com/monnimeter/openhab2/tree/wifiled/addons/binding/org.openhab.binding.wifiled).

## Supported Things

This Binding supports RGB(W) LED devices.

## Discovery

The LED WiFi Controllers are discovered by broadcast. The device has to be connected to the local network (i.e. by using the WiFi PBC connection method or the native App shipped with the device). For details please refer to the manual of the device. 

## Binding Configuration

No binding configuration required.

## Thing Configuration

Although this binding should usually work without manual configuration, it might be of interest if you want to enable color fading.

The color fading can be enabled by selecting the "FADING" driver.
If selected you can also set the number of fading steps and the fading duration.
Note that each fading step will at least take 10 ms for being process.
This natural limit is given by the speed of the LED controller.
Thus, a color fading with a configured fading duration of 0 might still take some time (count with more than 1 second for 100 steps).
IF the "FADING" driver is chosen the program channel and the programSpeed channel will not have any effect.
If you want to use the those functions you should use the "CLASSIC" driver.

If the automatic discovery does not work for some reason then the IP address and the port have to be set manually. Optionally, a refresh interval (in seconds) can be defined.
The binding supports newer controllers also known as v3 or LD382A (default) and the older generation also known as LD382. As the two generations differ in their protocol it might be necessary to set the configuration appropriately.

## Channels

- **power** Power state of the LEDs (on/off)
- **color** Color of the RGB LEDs expressed as values of hue, saturation and brightness
- **white** The brightness of the (warm) white LEDs
- **program** The program to be automatically run by the controller (i.e. color cross fade, strobe, etc.)
- **programSpeed** The speed of the programm

## Full example
N/A
