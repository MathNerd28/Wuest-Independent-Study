package com.mathnerd28.drawingpanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * {@code DrawingPanel} is a simple Java class that automatically handles the
 * backend and exposes an easy interface to the user.
 * <p>
 * Author: Alexander Bhalla (Victor Senior High School, NY)
 * <p>
 * Based on the work of Marty Stepp (Stanford University) and Stuart Reges
 * (University of Washington)
 * <p>
 * The {@code DrawingPanel} class provides a simple interface for drawing
 * persistent images using a {@code Graphics} object. An internal
 * {@code BufferedImage} object is used to keep track of what has been drawn. A
 * client of the class simply constructs a {@code DrawingPanel} of a particular
 * size and then draws on it with the {@code Graphics} object, setting the
 * background color if they so choose. To ensure that the image is always
 * displayed, a timer calls repaint at regular intervals.
 * <p>
 * The intention is that this custom library will mostly "stay out of the way"
 * so that the client mostly interacts with a standard Java
 * {@code java.awt.Graphics} object.
 *
 * @author Alexander Bhalla
 *
 * @version 1.0
 *
 * @implNote Requires Java 8 or later.
 *
 * @see <a target="_blank" href=
 *      "https://www.buildingjavaprograms.com/drawingpanel/DrawingPanel.java">
 *      www.buildingjavaprograms.com/drawingpanel/DrawingPanel.java</a>
 */
@SuppressWarnings({ "java:S1200", "java:S1845" })
public class DrawingPanel implements ImageObserver {
    /**
     * The title applied to each {@code DrawingPanel} instance upon creation.
     */
    private static final String DEFAULT_TITLE = "Drawing Panel";

    /**
     * The default width of the window in pixels when not specified.
     */
    private static final int DEFAULT_WIDTH = 1280;

    /**
     * The default height of the window in pixels when not specified.
     */
    private static final int DEFAULT_HEIGHT = 720;

    /**
     * The maximum permitted width and height of the window in pixels.
     */
    private static final int MAX_SIZE = 7680;

    /**
     * The default framerate of the window in frames per second.
     */
    private static final int DEFAULT_FRAMERATE = 30;

    /**
     * A mask used to obtain the alpha component of an argb pixel.
     */
    private static final int PIXEL_ALPHA_MASK = 0xFF000000;

    /**
     * A mask used to obtain the red component of an argb pixel.
     */
    private static final int PIXEL_RED_MASK = 0x00FF0000;

    /**
     * A mask used to obtain the green component of an argb pixel.
     */
    private static final int PIXEL_GREEN_MASK = 0x0000FF00;

    /**
     * A mask used to obtain the blue component of an argb pixel.
     */
    private static final int PIXEL_BLUE_MASK = 0x000000FF;

    /**
     * The id used for logging messages from the {@code DrawingPanel} class
     * rather than an instance.
     */
    private static final int NO_INSTANCE_NUMBER = 0;

    /**
     * Whether or not to display debug messages in std-out.
     */
    private static final boolean DEBUG = false;

    /**
     * An object used for synchronization on the class.
     */
    private static final Object LOCK = new Object();

    /**
     * The number of instances of the {@code DrawingPanel} class that currently
     * exist (including invisible ones).
     */
    private static int CURRENT_INSTANCE_COUNT = 0;

    /**
     * The total number of instances of the {@code DrawingPanel} class that have
     * been created.
     */
    private static int TOTAL_INSTANCE_COUNT = 0;

    /**
     * Whether or not the program's main(String[]) method is still running (used
     * for automatic shutdown).
     */
    private static boolean MAIN_RUNNING = true;

    /**
     * Whether or not to kill the program once all instances are closed.
     */
    private static boolean AUTO_SHUTDOWN_ENABLED = true;

    /**
     * The {@code Thread} that checks for automatic shutdown.
     */
    private static Thread SHUTDOWN_THREAD;

    /**
     * The internal image that is drawn to the screen
     */
    private BufferedImage image;

    /**
     * The {@code Graphics} object exposed to clients
     */
    private Graphics2D graphics;

    /**
     * The width of the window in pixels
     */
    private int width;

    /**
     * The height of the window in pixels
     */
    private int height;

    /**
     * The ID of the instance
     */
    private int instanceNumber;

    /**
     * Whether or not the window should render with anti-aliasing
     */
    private boolean isAntiAliased;

    /**
     * The background color of the window
     */
    private Color backgroundColor = Color.WHITE;

    /**
     * The custom component displaying the {@code BufferedImage}
     */
    private ImagePanel panel;

    /**
     * The color to use when clearing the screen
     */
    private int clearColor;

    /**
     * The {@code JFrame} that contains the {@code ImagePanel}
     */
    private JFrame window;

    /**
     * The timer that calls repaint at regular intervals
     */
    private DPTimer timer;

    /*
     * Static initializer to set up the shutdown thread
     */
    static {
        synchronized (LOCK) {
            SHUTDOWN_THREAD = new Thread(() -> {
                while (true) {
                    synchronized (LOCK) {
                        if (AUTO_SHUTDOWN_ENABLED && CURRENT_INSTANCE_COUNT == 0
                                && !mainMethodRunning()) {
                            debugPrint(
                                    "Shutdown thread: conditions met, exiting",
                                    NO_INSTANCE_NUMBER);
                            try {
                                System.exit(0);
                            } catch (SecurityException e) {
                                debugPrintException(e, NO_INSTANCE_NUMBER);
                            }
                        }
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        debugPrintException(e, NO_INSTANCE_NUMBER);
                    }
                }
            }, "DrawingPanel Shutdown");
            SHUTDOWN_THREAD.start();
            debugPrint("Shutdown thread started", NO_INSTANCE_NUMBER);
        }
    }

    /**
     * Constructs a visible {@code DrawingPanel} instance with the default size
     * of 1280x720 pixels.
     */
    public DrawingPanel() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Constructs a visible {@code DrawingPanel} instance with the specified
     * width and height.
     *
     * @param width
     *        the width of the window in pixels
     * @param height
     *        the height of the window in pixels
     *
     * @throws IllegalArgumentException
     *         if the width or height are negative or zero, or if they exceed
     *         the maximum of 7680 pixels.
     */
    public DrawingPanel(int width, int height) {
        this(width, height, true);
    }

    /**
     * Constructs a {@code DrawingPanel} instance with the specified width and
     * height.
     *
     * @param width
     *        the width of the window in pixels
     * @param height
     *        the height of the window in pixels
     * @param visible
     *        whether or not the window should be visible upon creation
     *
     * @throws IllegalArgumentException
     *         if the width or height are negative or zero, or if they exceed
     *         the maximum of 7680 pixels.
     */
    @SuppressWarnings({ "java:S2693", "java:S1147", "java:S1699" })
    public DrawingPanel(int width, int height, boolean isVisible) {
        this.debugPrint("New instance requested, params=[width=" + width
                + ", height=" + height + ", isVisible=" + isVisible + "]");

        checkSize(width, height);
        this.width = width;
        this.height = height;

        synchronized (LOCK) {
            CURRENT_INSTANCE_COUNT++;
            TOTAL_INSTANCE_COUNT++;
            this.instanceNumber = TOTAL_INSTANCE_COUNT;
        }
        this.debugPrint("Parameters valid, id assigned");

        this.image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        this.clearColor = image.getRGB(0, 0);
        this.graphics = image.createGraphics();
        this.setAntiAlias(true);
        this.panel = new ImagePanel(this.image);
        this.panel.setBackground(backgroundColor);
        this.panel.setSize(this.width, this.height);
        this.window = new JFrame(DEFAULT_TITLE);
        this.window.add(panel);
        this.window.setSize(this.width, this.height);
        this.window.pack();
        this.window.setVisible(isVisible);
        this.debugPrint("Framebuffer initialized");

        this.timer = new DPTimer(() -> this.panel.repaint(),
                calculateDelayNanos(DEFAULT_FRAMERATE));
        new Thread(this.timer,
                "DrawingPanel-" + this.instanceNumber + " Timer").start();
        this.debugPrint("Painting started");
    }

    /**
     * Changes the rate at which the window is repainted. Defaults to 30 fps.
     *
     * @param framerate
     *        the new framerate in frames per second
     *
     * @throws IllegalArgumentException
     *         if the framerate is negative or zero
     */
    public void setFrameRate(int framerate) {
        if (framerate < 1) {
            throw new IllegalArgumentException("Framerate must be positive");
        }
        this.timer.setDelay(calculateDelayNanos(framerate));
        this.debugPrint("Set framerate to " + framerate + "fps");
    }

    /**
     * Adds an event listener that responds to window, mouse, and/or keyboard
     * events. Currently accepts {@code MouseListener},
     * {@code MouseMotionListener}, {@code MouseWheelListener},
     * {@code KeyListener}, {@code WindowListener}, {@code WindowFocusListener},
     * {@code WindowStateListener}, and {@code FocusListener}.
     *
     * @param listener
     *        the listener to add. If null, nothing happens.
     */
    public void addListener(EventListener listener) {
        if (listener instanceof MouseListener) {
            this.panel.addMouseListener((MouseListener) listener);
            this.debugPrint("Added MouseListener");
        }
        if (listener instanceof MouseMotionListener) {
            this.panel.addMouseMotionListener((MouseMotionListener) listener);
            this.debugPrint("Added MouseMotionListener");
        }
        if (listener instanceof MouseWheelListener) {
            this.panel.addMouseWheelListener((MouseWheelListener) listener);
            this.debugPrint("Added MouseWheelListener");
        }
        if (listener instanceof KeyListener) {
            this.panel.addKeyListener((KeyListener) listener);
            this.debugPrint("Added KeyListener");
        }
        if (listener instanceof WindowListener) {
            this.window.addWindowListener((WindowListener) listener);
            this.debugPrint("Added WindowListener");
        }
        if (listener instanceof WindowFocusListener) {
            this.window.addWindowFocusListener((WindowFocusListener) listener);
            this.debugPrint("Added WindowFocusListener");
        }
        if (listener instanceof FocusListener) {
            this.panel.addFocusListener((FocusListener) listener);
            this.debugPrint("Added FocusListener");
        }
        if (listener instanceof WindowStateListener) {
            this.window.addWindowStateListener((WindowStateListener) listener);
            this.debugPrint("Added WindowStateListener");
        }
    }

    /**
     * Centers this {@code DrawingPanel} in the middle of the screen.
     *
     * @implNote This method is only guaranteed to work correctly on
     *           single-display systems.
     */
    public void center() {
        Dimension screenSize = Toolkit.getDefaultToolkit()
                                      .getScreenSize();
        int x = Math.max(0, (screenSize.width - this.window.getWidth()) / 2);
        int y = Math.max(0, (screenSize.height - this.window.getHeight()) / 2);
        this.setWindowPosition(x, y);
    }

    /**
     * Sets the position of the window.
     *
     * @param x
     *        the x-coordinate of the window's top-left corner
     * @param y
     *        the y-coordinate of the window's top-left corner
     *
     * @implNote This method is only guaranteed to work correctly on
     *           single-display systems.
     */
    public void setWindowPosition(int x, int y) {
        this.window.setLocation(x, y);
    }

    /**
     * Clears the screen to the background color
     */
    public void clear() {
        int[] pixels = new int[this.width * this.height];
        Arrays.fill(pixels, this.clearColor);
        this.image.setRGB(0, 0, this.width, this.height, pixels, 0, 1);
    }

    /**
     * Closes and disposes of the window, releasing any system resources
     * currently in use.
     */
    public void close() {
        this.window.setVisible(false);
        this.window.dispose();
        this.timer.stop();
    }

    /**
     * Changes the visibility of the window.
     *
     * @param visible
     *        whether or not the window should be shown on screen
     */
    public void setVisible(boolean visible) {
        this.window.setVisible(visible);
    }

    /**
     * Gets the {@code Graphics} object used to draw on the internal buffer.
     *
     * @return a {@code Graphics2D} object
     */
    public Graphics2D getGraphics() {
        return this.graphics;
    }

    /**
     * @return the height of this window
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the width of this window
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Gets the color of a single pixel on the window.
     *
     * @param x
     *        the x-coordinate of the pixel
     * @param y
     *        the y-coordinate of the pixel
     *
     * @return the color of the pixel
     *
     * @throws IndexOutOfBoundsException
     *         if the coordinates are out of bounds
     */
    public int getPixelRGB(int x, int y) {
        this.checkPixelLocation(x, y);
        return this.image.getRGB(x, y);
    }

    /**
     * Gets the colors of every pixel on the window.
     *
     * @return a 2D int array representing the pixel colors
     */
    public int[][] getPixelsRGB() {
        int[][] pixels = new int[this.height][];
        for (int y = 0; y < pixels.length; y++) {
            pixels[y] = this.image.getRGB(0, y, this.width, 1, null, 0, 1);
        }
        return pixels;
    }

    /**
     * Gets the position of this window on the screen.
     *
     * @return the screen position
     *
     * @implNote This method is only guaranteed to work correctly on
     *           single-display systems.
     */
    public Dimension getScreenPosition() {
        return new Dimension(this.window.getX(), this.window.getY());
    }

    /**
     * An internal method that logs a message to the console if in debug mode.
     *
     * @param message
     *        the message to log
     */
    private void debugPrint(String message) {
        debugPrint(message, this.instanceNumber);
    }

    /**
     * An internal method that logs an {@code Exception} to the console if in
     * debug mode.
     *
     * @param t
     *        the Exception to log
     */
    private void debugPrintException(Throwable t) {
        debugPrintException(t, this.instanceNumber);
    }

    /**
     * An internal method that ensures a pixel index is in-bounds.
     *
     * @param x
     *        the x-coordinate of the pixel
     * @param y
     *        the y-coordinate of the pixel
     *
     * @throws IndexOutOfBoundsException
     *         if the coordinates are out of bounds
     */
    private void checkPixelLocation(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Pixel location (" + x + ", "
                    + y + ") is out of bounds for DrawingPanel size (" + width
                    + ", " + height + ").");
        }
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height) {
        return this.panel.imageUpdate(img, infoflags, x, y, width, height);
    }

    /**
     * Finds all pixels currently on the screen with an exact color and replaces
     * them with another color.
     *
     * @param oldColor
     *        the color to find
     * @param newColor
     *        the color to replace with
     */
    public void replaceColor(int oldColor, int newColor) {
        this.replaceColor(oldColor, newColor, 0);
    }

    /**
     * Finds all pixels currently on the screen with a color (within a
     * tolerance) and replaces them with another color.
     * <p>
     * Pixels are selected if each component of the color is within the
     * tolerance of the corresponding component of the specified color.
     *
     * @param oldColor
     *        the color to find
     * @param newColor
     *        the color to replace with
     * @param tolerance
     *        the tolerance of each component of the color
     */
    public void replaceColor(int oldColor, int newColor, int tolerance) {
        int oldAlpha = getAlpha(oldColor);
        int oldRed = getRed(oldColor);
        int oldGreen = getGreen(oldColor);
        int oldBlue = getBlue(oldColor);
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int rgb = this.image.getRGB(x, y);
                if (Math.abs(oldAlpha - getAlpha(rgb)) <= tolerance
                        && Math.abs(oldRed - getRed(rgb)) <= tolerance
                        && Math.abs(oldGreen - getGreen(rgb)) <= tolerance
                        && Math.abs(oldBlue - getBlue(rgb)) <= tolerance) {
                    this.image.setRGB(x, y, newColor);
                }
            }
        }
    }

    /**
     * Sets whether this window should be above all other windows.
     *
     * @param alwaysOnTop
     *        whether the window should be on top
     *
     * @throws SecurityException
     *         if permission to change window attributes is not granted
     *
     * @see Window#setAlwaysOnTop(boolean)
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.window.setAlwaysOnTop(alwaysOnTop);
    }

    /**
     * Sets whether this window renders using anti-aliasing.
     *
     * @param antiAliasing
     *        true if anti-aliasing should be used
     */
    public void setAntiAlias(boolean antiAlias) {
        if (antiAlias != this.isAntiAliased) {
            setAntiAliasForce(antiAlias);
        }
    }

    /**
     * Sets whether this window renders using anti-aliasing (bypassing checks).
     *
     * @param antiAliasing
     *        true if anti-aliasing should be used
     */
    private void setAntiAliasForce(boolean antiAlias) {
        this.isAntiAliased = antiAlias;
        this.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Sets the background color of this window.
     *
     * @param color
     *        the background color
     */
    public void setBackgroundColor(Color color) {
        this.panel.setBackground(Objects.requireNonNull(color));
    }

    /**
     * Sets the background color of this window.
     *
     * @param color
     *        the background color as an int
     */
    public void setBackgroundColor(int color) {
        this.setBackgroundColor(new Color(color));
    }

    /**
     * Sets the color of a single pixel
     *
     * @param x
     *        the x-coordinate of the pixel
     * @param y
     *        the y-coordinate of the pixel
     * @param color
     *        the color of the pixel
     *
     * @throws IndexOutOfBoundsException
     *         if the coordinates are out of bounds
     */
    public void setPixelRGB(int x, int y, int rgb) {
        this.checkPixelLocation(x, y);
        this.image.setRGB(x, y, rgb);
    }

    /**
     * Sets the color of every pixel on the {@code DrawingPanel}. If the size of
     * the array doesn't match, the window will be resized to fit.
     *
     * @param pixels
     *        the 2D array of pixels to set
     *
     * @implNote This method expects that the input array is rectangular;
     *           otherwise, behavior is undefined and may cause artifacts.
     */
    public void setPixelsRGB(int[][] pixels) {
        if (this.height != pixels.length || this.width != pixels[0].length) {
            this.setSize(pixels.length, pixels[0].length);
        }

        for (int y = 0; y < pixels.length; y++) {
            this.image.setRGB(0, y, this.width, 1, pixels[y], 0, 1);
        }
    }

    /**
     * Sets the size of the window.
     *
     * @param width
     *        the width of the window
     * @param height
     *        the height of the window
     *
     * @throws IllegalArgumentException
     *         if the width or height is less than 1 or larger than the maximum
     */
    public void setSize(int width, int height) {
        checkSize(width, height);
        this.width = width;
        this.height = height;

        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        this.graphics = newImage.createGraphics();
        this.graphics.drawImage(image, 0, 0, panel);
        this.image = newImage;

        this.panel.setImage(this.image);
        this.setAntiAliasForce(this.isAntiAliased);
        this.window.pack();
    }

    /**
     * Brings this window to the front of all other windows if possible.
     */
    public void toFront() {
        this.window.toFront();
    }

    /**
     * Sets the title of this window.
     *
     * @param title
     *        the new title of the window
     *
     * @throws NullPointerException
     *         if the title is null
     */
    public void setTitle(String title) {
        this.window.setTitle(Objects.requireNonNull(title));
    }

    /**
     * Loads an image from a file.
     *
     * @param filePath
     *        the path to the file
     *
     * @return the loaded image
     *
     * @throws FileNotFoundException
     *         if the file doesn't exist
     */
    public static Image loadImage(String filePath)
            throws FileNotFoundException {
        return loadImage(new File(filePath));
    }

    /**
     * Loads an image from a file.
     *
     * @param file
     *        the file to load
     *
     * @return the loaded image, or {@code null} if an error occured
     *
     * @throws NullPointerException
     *         if the file is null
     * @throws FileNotFoundException
     *         if the file doesn't exist
     *
     * @implNote This method attempts to return the same {@code Image} instance
     *           to multiple calls with the same file.
     */
    public static Image loadImage(File file) throws FileNotFoundException {
        if (!Objects.requireNonNull(file)
                    .exists()) {
            throw new FileNotFoundException("File not found: " + file);
        }

        try {
            URL url = file.toURI()
                          .toURL();
            return Toolkit.getDefaultToolkit()
                          .getImage(url);
        } catch (MalformedURLException e) {
            debugPrintException(e, NO_INSTANCE_NUMBER);
            return null;
        }
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to the
     * precision and accuracy of system timers and schedulers.
     *
     * @param milliseconds
     *        the length of time to sleep
     *
     * @throws IllegalArgumentException
     *         if the value of {@code milliseconds} is negative
     *
     * @see Thread#sleep(long)
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            debugPrintException(e, NO_INSTANCE_NUMBER);
        }
    }

    /**
     * Gets the alpha component of a pixel.
     *
     * @param pixelARGB
     *        the ARGB value of the pixel
     *
     * @return the alpha component of the pixel (0-255)
     */
    public static int getAlpha(int pixelARGB) {
        return (pixelARGB & PIXEL_ALPHA_MASK) >>> 24;
    }

    /**
     * Gets the red component of a pixel.
     *
     * @param pixelARGB
     *        the ARGB value of the pixel
     *
     * @return the red component of the pixel (0-255)
     */
    public static int getRed(int pixelARGB) {
        return (pixelARGB & PIXEL_RED_MASK) >>> 16;
    }

    /**
     * Gets the green component of a pixel.
     *
     * @param pixelARGB
     *        the ARGB value of the pixel
     *
     * @return the green component of the pixel (0-255)
     */
    public static int getGreen(int pixelARGB) {
        return (pixelARGB & PIXEL_GREEN_MASK) >>> 8;
    }

    /**
     * Gets the blue component of a pixel.
     *
     * @param pixelARGB
     *        the ARGB value of the pixel
     *
     * @return the blue component of the pixel (0-255)
     */
    public static int getBlue(int pixelARGB) {
        return (pixelARGB & PIXEL_BLUE_MASK);
    }

    /**
     * Calculates the ARGB value from a pixel's subcomponents.
     *
     * @param red
     *        the red component of the pixel (0-255)
     * @param green
     *        the green component of the pixel (0-255)
     * @param blue
     *        the blue component of the pixel (0-255)
     * @param alpha
     *        the alpha component of the pixel (0-255)
     *
     * @return the ARGB value of the pixel
     */
    public static int getPixelARGB(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Calculates the ARGB value from a pixel's subcomponents, assuming full
     * alpha (opaque).
     *
     * @param red
     *        the red component of the pixel (0-255)
     * @param green
     *        the green component of the pixel (0-255)
     * @param blue
     *        the blue component of the pixel (0-255)
     *
     * @return the ARGB value of the pixel
     */
    public static int getPixelARGB(int red, int green, int blue) {
        return getPixelARGB(255, red, green, blue);
    }

    /**
     * Sets whether this class should automatically attempt close the program if
     * all windows are closed and the main method is no longer running.
     *
     * @param autoClose
     *        {@code true} to automatically close the program.
     */
    public static void setAutoExit(boolean autoExit) {
        AUTO_SHUTDOWN_ENABLED = autoExit;
    }

    /**
     * An internal method that ensures the size values are valid.
     *
     * @param width
     *        the width of the window
     * @param height
     *        the height of the window
     *
     * @throws IllegalArgumentException
     *         if the width or height are invalid
     */
    private static void checkSize(int width, int height) {
        if (width < 1 || width > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Width must be between 1 and " + MAX_SIZE + ".");
        }
        if (height < 1 || height > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Height must be between 1 and " + MAX_SIZE + ".");
        }
    }

    /**
     * An internal method that checks to see if the {@code main(String... args)}
     * method is still running for auto-shutdown pusposes.
     *
     * @return false if the main execution is complete
     */
    @SuppressWarnings("java:S3014")
    private static boolean mainMethodRunning() {
        if (!MAIN_RUNNING) {
            return false;
        }

        ThreadGroup mainGroup = Thread.currentThread()
                                      .getThreadGroup();
        ThreadGroup parent = mainGroup.getParent();
        while (parent != null) {
            mainGroup = parent;
            parent = mainGroup.getParent();
        }

        ThreadGroup[] childGroups = new ThreadGroup[mainGroup.activeGroupCount()];
        mainGroup.enumerate(childGroups, false);
        for (ThreadGroup childGroup : childGroups) {
            if ("main".equals(childGroup.getName())) {
                Thread[] childThreads = new Thread[childGroup.activeCount()];
                childGroup.enumerate(childThreads, false);
                for (Thread childThread : childThreads) {
                    if ("main".equals(childThread.getName())) {
                        return true;
                    }
                }
            }
        }

        MAIN_RUNNING = false;
        return false;
    }

    /**
     * An internal method that prints an exception to the console.
     *
     * @param message
     *        the message to print
     * @param instanceNumber
     *        the instance number of the {@code DrawingPanel}
     */
    private static void debugPrint(String message, int instanceNum) {
        if (DEBUG) {
            System.out.print(
                    "[DrawingPanel" + (instanceNum == NO_INSTANCE_NUMBER ? "] "
                            : ("-" + instanceNum + "] ")));
            System.out.println(message);
        }
    }

    /**
     * An internal method that prints an exception to the console.
     *
     * @param e
     *        the exception to print
     * @param instanceNumber
     *        the instance number of the {@code DrawingPanel}
     */
    private static void debugPrintException(Throwable t, int instanceNum) {
        if (DEBUG) {
            System.out.println(
                    "[DrawingPanel" + (instanceNum == NO_INSTANCE_NUMBER ? "] "
                            : ("-" + instanceNum + "] ")));
            t.printStackTrace();
        }
    }

    /**
     * An internal method that calculates the delay between calls to
     * {@code repaint()}
     *
     * @param frameRate
     *        the frames per second
     *
     * @return the delay in nanoseconds
     */
    private static long calculateDelayNanos(int frameRate) {
        return 1_000_000_000L / frameRate;
    }

    /**
     * An internal implementation of {@code JPanel} that is used to draw the
     * image to the screen.
     */
    private static class ImagePanel extends JPanel {
        /**
         * The image that is drawn to the screen
         */
        private transient BufferedImage image;

        /**
         * Creates a new {@code ImagePanel} with the specified image.
         *
         * @param image
         *        the image to draw
         */
        @SuppressWarnings("java:S1699")
        ImagePanel(BufferedImage image) {
            this.setImage(image);
            this.setBackground(Color.WHITE);
            this.setPreferredSize(
                    new Dimension(image.getWidth(), image.getHeight()));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            graphics.drawImage(this.image, 0, 0, this);
        }

        /**
         * Sets a new image to draw and resizes the panel.
         *
         * @param image
         *        the image to draw
         */
        void setImage(BufferedImage image) {
            this.image = image;
            this.setPreferredSize(
                    new Dimension(image.getWidth(), image.getHeight()));
            this.repaint();
        }
    }

    /**
     * An internal timer implementation that is used to determine call the
     * {@code repaint()} method at regular intervals, inspired by
     * {@link java.util.Timer}
     */
    @SuppressWarnings("java:S2972")
    private class DPTimer implements Runnable {
        /**
         * Whether this thread should continue to run
         */
        private volatile boolean running;

        /**
         * The action that should be performed at each interval
         */
        private Runnable action;

        /**
         * The delay between calls to {@code action}
         */
        private long delayNanos;

        /**
         * The last point the interval was changed
         */
        private AtomicLong referenceTime = new AtomicLong();

        /**
         * The number of times the action has been called since
         * {@code referenceTime}
         */
        private AtomicLong iterationCount = new AtomicLong();

        /**
         * Creates a new {@code DPTimer} with the specified action and delay.
         *
         * @param action
         *        the action to perform
         * @param delayNanos
         *        the delay between calls to {@code action}
         */
        DPTimer(Runnable action, long delayNanos) {
            this.action = action;
            this.delayNanos = delayNanos;
        }

        @Override
        public void run() {
            this.running = true;
            this.iterationCount.set(0);
            this.referenceTime.set(System.nanoTime());
            long currentTime = referenceTime.get();
            long targetTime = referenceTime.get();
            long sleepTime = 0;

            while (this.running) {
                currentTime = System.nanoTime();
                if (currentTime < targetTime) {
                    sleepTime = ((targetTime - currentTime) >> 23) * 7;
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            debugPrintException(e);
                        }
                    }
                } else {
                    action.run();
                    iterationCount.incrementAndGet();
                    targetTime = referenceTime.get()
                            + (iterationCount.get() * delayNanos);
                }
            }
        }

        /**
         * Changes the delay between calls to {@code action}
         */
        void setDelay(long delayNanos) {
            this.delayNanos = delayNanos;
            this.referenceTime.set(System.nanoTime());
            this.iterationCount.set(0);
        }

        /**
         * Stops the timer and its action from being called. Actions that have
         * already begun will continue to completion.
         */
        void stop() {
            this.running = false;
        }
    }
}
