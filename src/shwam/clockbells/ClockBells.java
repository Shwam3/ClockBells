package shwam.clockbells;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class ClockBells
{
    private static List<Clip> clipsToClose = new ArrayList<>();
    private static boolean isMuted = false;
    
    public static void main(String[] args)
    {
        Calendar calendar = Calendar.getInstance();
        URL chimesURL = ClockBells.class.getResource("/shwam/clockbells/resources/Chimes.wav");
        URL strikeURL = ClockBells.class.getResource("/shwam/clockbells/resources/Strike.wav");
        
        if (SystemTray.isSupported())
        {
            try
            {
                Image img = ImageIO.read(ClockBells.class.getResource("/shwam/clockbells/resources/Icon.png"));
                PopupMenu menu = new PopupMenu();
                CheckboxMenuItem mute = new CheckboxMenuItem("Mute");
                mute.addItemListener(e -> isMuted = e.getStateChange() == ItemEvent.SELECTED);
                menu.add(mute);
                MenuItem manualChime = new MenuItem("Chime now");
                manualChime.addActionListener(e ->
                {
                    boolean origMuted = isMuted;
                    isMuted = true;
                    
                    Calendar c = Calendar.getInstance();
                    int hour = c.get(Calendar.HOUR_OF_DAY) % 12;
                    hour = hour == 0 ? 12 : hour;
                    boolean isAM = c.get(Calendar.AM_PM) == Calendar.AM;

                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()) + "] Chiming manually for " + hour + (isAM ? "am" : "pm"));
                    play(chimesURL, 19500);

                    for (int i = 0; i < hour; i++)
                        play(strikeURL, 4300);

                    sleep(20000);
                    closeClips();
                    
                    isMuted = origMuted;
                });
                menu.add(manualChime);
                menu.addSeparator();
                MenuItem exit = new MenuItem("Exit");
                exit.addActionListener(e -> System.exit(0));
                menu.add(exit);
                TrayIcon icon = new TrayIcon(img, "Clock Chime", menu);
                icon.setImageAutoSize(true);
                
                SystemTray.getSystemTray().add(icon);
            }
            catch (AWTException | IOException ex) { ex.printStackTrace(); }
        }
        
        calendar = Calendar.getInstance();
        int minutesToNextHour = 60 - calendar.get(Calendar.MINUTE);
        int secondsToNextHour = 60 - calendar.get(Calendar.SECOND);
        int millisToNextHour = 1000 - calendar.get(Calendar.MILLISECOND);
        millisToNextHour = minutesToNextHour*60*1000 + secondsToNextHour*1000 + millisToNextHour - 60000;
        
        System.out.println("Waiting " + millisToNextHour + "ms until chiming");
        
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY) % 12;
                hour = hour == 0 ? 12 : hour;
                boolean isAM = calendar.get(Calendar.AM_PM) == Calendar.AM;
                
                if (!isMuted)
                {
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()) + "] Chiming for " + hour + (isAM ? "am" : "pm"));

                    play(chimesURL, 19500);

                    for (int i = 0; i < hour; i++)
                        play(strikeURL, 4300);

                    sleep(20000);
                    closeClips();
                }
                else
                    System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()) + "] Skipping chime for " + hour + (isAM ? "am" : "pm"));

            }
        }, millisToNextHour, 60*60*1000, TimeUnit.MILLISECONDS);
    }
    
    private static void play(URL file, int wait)
    {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file))
        {
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-15.0f);
            clip.start();
            clipsToClose.add(clip);
            sleep(wait);
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) { ex.printStackTrace(); }
    }
    
    private static void closeClips()
    {
        clipsToClose.stream().filter(c -> !c.isRunning()).forEach(c -> { c.close(); });
        clipsToClose.clear();
        System.out.println("Clips closed");
    }
    
    private static void sleep(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ex) { ex.printStackTrace(); }
    }
}