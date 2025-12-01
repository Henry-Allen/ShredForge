package com.shredforge;

import com.formdev.flatlaf.FlatDarkLaf;
import com.shredforge.ui.MainFrame;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefSettings;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main entry point for the Swing + JCEF version of ShredForge.
 */
public class SwingApp {
    
    private static CefApp cefApp;
    
    public static void main(String[] args) {
        // Set up exception handler to suppress benign JCEF/AppKit thread exceptions on macOS
        // These occur when CEF callbacks happen on the AppKit thread instead of EDT
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String threadName = thread.getName();
            // Suppress known benign JCEF exceptions on AppKit Thread
            if (threadName.contains("AppKit") || threadName.contains("AWT-AppKit")) {
                // Only log if it's not the common null message exception
                if (throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                    System.err.println("JCEF AppKit exception (suppressed): " + throwable.getMessage());
                }
                return;
            }
            // For other threads, print the full stack trace
            System.err.println("Uncaught exception in thread " + threadName + ":");
            throwable.printStackTrace();
        });
        
        // Set up FlatLaf dark theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf look and feel: " + e.getMessage());
        }
        
        // Initialize JCEF on a background thread, then launch UI
        SwingUtilities.invokeLater(() -> {
            try {
                // Show splash/loading while JCEF initializes
                JFrame splash = createSplashFrame();
                splash.setVisible(true);
                
                // Initialize JCEF in background
                new Thread(() -> {
                    try {
                        initializeCef();
                        SwingUtilities.invokeLater(() -> {
                            splash.dispose();
                            MainFrame frame = new MainFrame(cefApp);
                            frame.setVisible(true);
                        });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            splash.dispose();
                            JOptionPane.showMessageDialog(null, 
                                "Failed to initialize browser: " + e.getMessage(),
                                "Initialization Error", 
                                JOptionPane.ERROR_MESSAGE);
                            System.exit(1);
                        });
                    }
                }, "jcef-init").start();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, 
                    "Failed to start application: " + e.getMessage(),
                    "Startup Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
    
    private static JFrame createSplashFrame() {
        JFrame splash = new JFrame();
        splash.setUndecorated(true);
        splash.setSize(400, 200);
        splash.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        JLabel titleLabel = new JLabel("ShredForge");
        titleLabel.setFont(titleLabel.getFont().deriveFont(24f));
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JLabel statusLabel = new JLabel("Initializing browser engine...");
        statusLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(JProgressBar.CENTER_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(progress);
        
        splash.add(panel);
        return splash;
    }
    
    private static void initializeCef() throws CefInitializationException, UnsupportedPlatformException, IOException, InterruptedException {
        CefAppBuilder builder = new CefAppBuilder();
        
        // Set installation directory for CEF binaries
        Path cefDir = Path.of(System.getProperty("user.home"), ".shredforge", "jcef");
        Files.createDirectories(cefDir);
        builder.setInstallDir(cefDir.toFile());
        
        // Configure CEF settings
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
        
        // Build and initialize
        cefApp = builder.build();
        System.out.println("JCEF initialized successfully");
    }
    
    public static CefApp getCefApp() {
        return cefApp;
    }
    
    public static void shutdown() {
        if (cefApp != null) {
            cefApp.dispose();
            cefApp = null;
        }
    }
}
