import greenfoot.*;
import java.util.ArrayList;

public class DebugDisplay extends Actor {
    private GreenfootImage background;
    private Color textColor = Color.WHITE;
    private Font textFont = new Font("Consolas", false, false, 14);
    private ArrayList < String > logLines = new ArrayList < > ();
    private final int MAX_LINES = 12;

    private boolean isVisible = false;
    private boolean f2Held = false;

    public DebugDisplay() {
        background = new GreenfootImage(800, 300);
        background.setColor(new Color(0, 0, 0, 200));
        background.fill();
        setImage(new GreenfootImage(background));
        clearLog();
        applyVisibility();
    }

    public void act() {

        boolean pressed = Greenfoot.isKeyDown("f2");
        if (pressed && !f2Held) {
            isVisible = !isVisible;
            applyVisibility();
        }
        f2Held = pressed;
    }

    public void addLogMessage(String message) {
        logLines.add(0, message);
        if (logLines.size() > MAX_LINES) logLines.remove(logLines.size() - 1);
        if (isVisible) redraw();
    }

    public void clearLog() {
        logLines.clear();
        logLines.add("--- Combat Log Initialized ---");
        if (isVisible) redraw();
    }

    private void redraw() {
        GreenfootImage img = new GreenfootImage(background);
        img.setFont(textFont);
        img.setColor(textColor);
        int y = 20;
        for (String line: logLines) {
            img.drawString(line, 10, y);
            y += 20;
        }
        setImage(img);
    }

    private void applyVisibility() {
        if (isVisible) {
            redraw();
            getImage().setTransparency(255);
        } else {

            getImage().setTransparency(0);
        }
    }
}