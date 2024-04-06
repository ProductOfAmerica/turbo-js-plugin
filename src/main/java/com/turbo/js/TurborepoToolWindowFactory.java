package com.turbo.js;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Calendar;
import java.util.Objects;

final class TurboJSToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CalendarToolWindowContent toolWindowContent = new CalendarToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class CalendarToolWindowContent {
        private static final String TURBO_JSON_PNG_PATH = "/icons/turbo-json.png";

        private final JPanel contentPanel = new JPanel();
        private final JLabel currentDate = new JLabel();
        private final JLabel timeZone = new JLabel();
        private final JLabel currentTime = new JLabel();
        private final ToolWindow toolWindow;

        public CalendarToolWindowContent(ToolWindow toolWindow) {
            this.toolWindow = toolWindow;
            setupContentPanel();
            updateCurrentDateTime();
        }

        private void setupContentPanel() {
            contentPanel.setLayout(new BorderLayout(0, 20));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
            contentPanel.add(createCalendarPanel(), BorderLayout.PAGE_START);
            contentPanel.add(createControlsPanel(), BorderLayout.CENTER);
        }

        @NotNull
        private JPanel createCalendarPanel() {
            JPanel calendarPanel = new JPanel();
            setIconLabel(currentDate);
            setIconLabel(timeZone);
            setIconLabel(currentTime);
            calendarPanel.add(currentDate);
            calendarPanel.add(timeZone);
            calendarPanel.add(currentTime);
            return calendarPanel;
        }

        private void setIconLabel(JLabel label) {
            URL location = Objects.requireNonNull(getClass().getResource(CalendarToolWindowContent.TURBO_JSON_PNG_PATH));
            label.setIcon(new ImageIcon(location));
        }

        @NotNull
        private JPanel createControlsPanel() {
            JPanel controlsPanel = new JPanel();
            controlsPanel.add(createButton("Refresh", this::updateCurrentDateTime));
            controlsPanel.add(createButton("Hide", () -> toolWindow.hide(null)));
            return controlsPanel;
        }

        private JButton createButton(String text, Runnable action) {
            JButton button = new JButton(text);
            button.addActionListener(e -> action.run());
            return button;
        }

        private void updateCurrentDateTime() {
            Calendar calendar = Calendar.getInstance();
            currentDate.setText(getCurrentDate(calendar));
            timeZone.setText(getTimeZone(calendar));
            currentTime.setText(getCurrentTime(calendar));
        }

        private String getCurrentDate(Calendar calendar) {
            return calendar.get(Calendar.DAY_OF_MONTH) + "/" + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR);
        }

        private String getTimeZone(Calendar calendar) {
            long gmtOffset = calendar.get(Calendar.ZONE_OFFSET); // offset from GMT in milliseconds
            String gmtOffsetString = String.valueOf(gmtOffset / 3600000);
            return (gmtOffset > 0) ? "GMT + " + gmtOffsetString : "GMT - " + gmtOffsetString;
        }

        private String getCurrentTime(Calendar calendar) {
            return getFormattedValue(calendar, Calendar.HOUR_OF_DAY) + ":" + getFormattedValue(calendar, Calendar.MINUTE);
        }

        private String getFormattedValue(Calendar calendar, int calendarField) {
            int value = calendar.get(calendarField);
            return StringUtils.leftPad(Integer.toString(value), 2, "0");
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
