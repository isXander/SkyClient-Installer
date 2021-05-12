package co.uk.isxander.skyclient.installer.start;

import co.uk.isxander.skyclient.installer.SkyClient;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.io.PrintStream;

public class Main {

    public static void main(String[] args) throws Exception {
        PrintStream fileOut = new PrintStream("./skyclient.log");
        System.setOut(fileOut);

        System.out.println("Setting LAF...");
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SkyClient.getInstance();
    }

}
