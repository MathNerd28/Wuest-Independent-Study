package com.mathnerd28.hangman;

import com.mathnerd28.drawingpanel.DrawingPanel;

public class Main {
    public static void main(String... args) {
        DrawingPanel panel = new DrawingPanel(500, 500);
        panel.sleep(5000);
        panel.close();
    }
}
