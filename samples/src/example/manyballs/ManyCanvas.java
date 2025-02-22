/*
 * @(#)ManyCanvas.java  1.11 01/08/21
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */

package example.manyballs;

import javax.microedition.lcdui.*;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import com.sun.squawk.*;

public class ManyCanvas extends javax.microedition.lcdui.Canvas {

    Display display;

    // a set of free roaming balls
    SmallBall[] balls;
    int numBalls;
    int width, height;
    boolean paused;
    static int NUM_HISTORY = 8;
    long times[] = new long[NUM_HISTORY];
    int times_idx;

    public ManyCanvas(Display d, int maxBalls, int initBalls) {

        display = d; // save the display

        // initialize the array of balls
        balls = new SmallBall[maxBalls];

        width = getWidth();
        height = getHeight();

        // Start the balls
        if (initBalls > maxBalls) {
            initBalls = maxBalls;
        }
        for (int i = 0; i != initBalls; ++i) {
            balls[i] = new SmallBall(this, 0, 0, width, height - 12 - 20);
        }
        numBalls = initBalls;
        paused = true;
    }

    /**
     * Draws the drawing frame (which also contains the ball) and the
     * controls.
     */
    String msg = null;
    protected void paint(Graphics g) {
//System.out.println("ManyCanvas::paint "+g);

        int x = g.getClipX();
        int y = g.getClipY();
        int w = g.getClipWidth();
        int h = g.getClipHeight();

        // Draw the frame
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);

        // Draw each ball
        for (int i = 0; i < numBalls; i++) {
            if (balls[i].inside(x, y, x + w, y + h)) {
                balls[i].paint(g);
            }
        }

        g.setColor(0);
        g.drawRect(0, 0, width - 1, height - 1);

        long now = System.currentTimeMillis();
        String str = null;
        if (times_idx >= NUM_HISTORY) {
            long oldTime = times[times_idx % NUM_HISTORY];
            if (oldTime == now) {
                // in case of divide-by-zero
                oldTime = now + 1;
            }
            long fps = ( (long) 1000 * (long) NUM_HISTORY) / (now - oldTime);

            // Set the performance metric to allow load balancing
            Isolate.currentIsolate().setProperty("performance.metric", "" + numBalls);

            if (times_idx % 20 == 0) {
                str = numBalls + " Ball(s) " + fps + " fps";
            }
        } else {
            if (times_idx % 20 == 0) {
                str = numBalls + " Ball(s)";
            }
        }

        if (msg != null) {
            g.setColor(0xffffff);
            g.setClip(0, height - 14, width, height);
            g.fillRect(0, height - 20, width - 2, 18);

            g.setColor(0);
            g.drawString(msg, 5, height - 14, 0);
            g.drawRect(0, 0, width - 1, height - 1);
            msg = null;
        }
        if (str != null) {
            /*
             * Do a complete repaint, so that the message will
             * be shown even in double-buffer mode.
             */
            repaint();
            msg = str;
        }

        times[times_idx % NUM_HISTORY] = now;
        ++times_idx;

    }

    /**
     * Handle a pen down event.
     */
    public void keyPressed(int keyCode) {

        int action = getGameAction(keyCode);

//System.out.println("ManyCanvas::keyPressed++ "+keyCode+" ACTION="+action);


        switch (action) {
            case LEFT:

                // Reduce the number of threads
                if (numBalls > 0) {

                    // decrement the counter
                    numBalls = numBalls - 1;

                    // stop the thread and remove the reference to it
                    balls[numBalls].stop = true;
                    balls[numBalls] = null;
                }
                break;

            case RIGHT:

                // Increase the number of threads
                if (numBalls < balls.length) {

                    // create a new ball and start it moving
                    balls[numBalls] =
                        new SmallBall(this, 0, 0, width, height - 12 - 20);
                    new Thread(balls[numBalls]).start();

                    // increment the counter
                    numBalls = numBalls + 1;
                }
                break;

            case UP:

                // Make them move faster
                SmallBall.faster();
                break;

            case DOWN:

                // Make them move slower
                SmallBall.slower();
                break;

            case KEY_NUM0:
            case KEY_NUM1:
            case KEY_NUM2:
            case KEY_NUM3:
            case KEY_NUM4:
            case KEY_NUM5:
            case KEY_NUM6:
            case KEY_NUM7:
            case KEY_NUM8:
            case KEY_NUM9:
                try {
                    int delay = action - KEY_NUM0;

                    DataInputStream in = Connector.openDataInputStream("systemproperties:");
                    while (in.available() != 0) {
                        String key = in.readUTF();
                        String value = in.readUTF();
                        System.out.println(key + "=" + value);
                        try {
                            Thread.currentThread().sleep(delay * 100);
                        } catch (InterruptedException ex) {
                        }
                    }
                    in.close();
                } catch (IOException ioe) {
                    System.err.println("Error reading system properties: " + ioe);
                }

                break
                    ;
        }
        repaint();
//System.out.println("ManyCanvas::keyPressed-- "+keyCode);
    }

    /**
     * Destroy
     */
    void destroy() {
//System.out.println("ManyCanvas::destroy");
        // kill all the balls and terminate
        for (int i = 0; i < balls.length && balls[i] != null; i++) {
            balls[i].stop = true;

            // enable the balls to be garbage collected
            balls[i] = null;
        }
        numBalls = 0;
    }

    /*
     * Return whether the canvas is paused or not.
     */
    boolean isPaused() {
        return paused;
    }

    /**
     * Pause the balls by signaling each of them to stop.
     * The ball object still exists and holds the current position
     * of the ball.  It may be restarted later.
     * The thread will terminate.
     * TBD: is a join needed?
     */
    void pause() {
//System.out.println("ManyCanvas::pause");
        if (!paused) {
            paused = true;
            for (int i = 0; i < balls.length && balls[i] != null; i++) {
                balls[i].stop = true;
            }
        }
        repaint();
    }

    /*
     * Start creates a new thread for each ball and start it.
     */
    void start() {
//System.out.println("ManyCanvas::start");
        if (paused) {
            paused = false;
            display.setCurrent(this);
            for (int i = 0; i < balls.length && balls[i] != null; i++) {
                Thread t = new Thread(balls[i]);
                t.start();
            }
        }

        repaint();

        String value = System.getProperty("manyballs.hibernation_interval");
        if (value != null) {
            int interval = Integer.parseInt(value);
            int count = Integer.MAX_VALUE;

            value = System.getProperty("manyballs.hibernation_count");
            if (value != null) {
                count = Integer.parseInt(value);
            }
            while (count-- > 0) {
                try {
                    try {
                        Thread.sleep(interval * 1000);
                    } catch (InterruptedException e) {
                    }

                    Isolate thisIsolate = Isolate.currentIsolate();
                    System.out.println("Hibernating " + thisIsolate);
                    thisIsolate.hibernate();
                    System.out.println("Reawoke " + thisIsolate);
//VM.println(VM.branchCount());
//VM.startTracing();
                    repaint();
                }
                catch (java.io.IOException ex) {
                    System.err.println("Error hibernating isolate: " + ex);
                    ex.printStackTrace();
                }
            }

        }


    }

}
