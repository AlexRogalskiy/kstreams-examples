package ru.curs.counting.cli;

import com.googlecode.lanterna.gui2.AsynchronousTextGUIThread;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class GUI implements AutoCloseable {

    private final Screen screen;
    private final AsynchronousTextGUIThread guiThread;
    private final Map<String, KeyValuePanel> panelMap = new ConcurrentSkipListMap<>();
    private final ValueSetter valueSetter;
    private final Panel mainPanel;
    private final Map<String, Long> internalTable = new ConcurrentHashMap<>();

    public GUI(Screen screen) throws IOException {
        this.screen = screen;

        final WindowBasedTextGUI textGUI = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);
        guiThread = (AsynchronousTextGUIThread) textGUI.getGUIThread();
        screen.startScreen();

        final Window window = new BasicWindow("Information");
        Panel wrapperPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));

        wrapperPanel.addComponent(mainPanel);
        wrapperPanel.addComponent(new EmptySpace());

        window.setComponent(wrapperPanel);
        textGUI.addWindow(window);
        guiThread.start();
        valueSetter = new ValueSetter(guiThread);

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this::refresh,
                300, 200, TimeUnit.MILLISECONDS);
    }

    void refresh(){
        for(Map.Entry<String, Long> e: internalTable.entrySet()){
            internalUpdate(e.getKey(), e.getValue());
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        guiThread.stop();
        guiThread.waitForStop();
        screen.close();
        valueSetter.close();
    }


    public void update(String item, long newVal){
        internalTable.put(item, newVal);
    }

    private void internalUpdate(String item, double newVal) {
        if (newVal < 1) {
            Optional.ofNullable(panelMap.remove(item)).ifPresent(
                    p -> guiThread.invokeLater(() ->
                            mainPanel.removeComponent(p.getPanel()))
            );
        } else {
            KeyValuePanel panel = panelMap.get(item);
            if (panel == null) {
                panel = new KeyValuePanel(item, newVal);
                panelMap.put(item, panel);
                guiThread.invokeLater(() -> {
                    mainPanel.removeAllComponents();
                    panelMap.forEach((k, v) ->
                            mainPanel.addComponent(v.getPanel()));
                });
                valueSetter.lineHightlight(panel);
            } else {
                valueSetter.setValue(panel, newVal);
            }

        }
    }

}
