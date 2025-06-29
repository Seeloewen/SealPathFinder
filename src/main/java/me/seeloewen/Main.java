package me.seeloewen;

import org.joml.Vector2f;

import java.time.LocalTime;

public class Main
{
    public static final int RADIUS = 100;
    public static final boolean ENABLEUI = true;
    public static final int STARTX = 70;
    public static final int STARTY = -50;

    public static MainWindow wnd;

    public static FastNoiseLite perlin = new FastNoiseLite(LocalTime.now().toSecondOfDay());
    public static float frequency = 0.1f;


    public static void main(String[] args)
    {
        //Setup noise generator
        perlin.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        perlin.SetFrequency(frequency);

        if (ENABLEUI)
        {
            wnd = new MainWindow(new Vector2f(STARTX, STARTY), RADIUS);
            wnd.setVisible(true);
        }

        PathFinder.findPath(new Vector2f(STARTX, STARTY), RADIUS, true);
    }
}