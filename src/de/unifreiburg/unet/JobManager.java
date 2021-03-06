/**************************************************************************
 *
 * Copyright (C) 2018 Thorsten Falk
 *
 *        Image Analysis Lab, University of Freiburg, Germany
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 **************************************************************************/

package de.unifreiburg.unet;

// ImageJ stuff
import ij.*;
import ij.process.*;
import ij.gui.*;

// Java GUI stuff
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;

public class JobManager extends JFrame
{

  private static JobManager _manager = null;

  public static JobManager instance() {
    if (_manager == null) _manager = new JobManager("U-Net Job Manager");
    return _manager;
  }

  private JobTableModel _jobTableModel;
  private static JDialog helpDialog = null;

  private JobManager() {
    super();
    init();
  }

  private JobManager(GraphicsConfiguration gc) {
    super(gc);
    init();
  }

  private JobManager(String title) {
    super(title);
    init();
  }

  private JobManager(String title, GraphicsConfiguration gc) {
    super(title, gc);
    init();
  }

  private void init() {
    setLayout(new GridBagLayout());

    _jobTableModel = new JobTableModel();
    JTable table = new JTable(_jobTableModel);

    table.getColumn("Status").setCellRenderer(new ProgressCellRenderer());
    table.getColumn("Progress").setCellRenderer(new ProgressCellRenderer());
    table.getColumn("Show").setCellRenderer(new ComponentCellRenderer());
    JButton terminatingButton = new JButton("Terminating...");
    table.getColumn("Show").setMinWidth(
        terminatingButton.getPreferredSize().width);
    table.getColumn("Show").setMaxWidth(
        terminatingButton.getPreferredSize().width);
    table.getColumn("Show").setPreferredWidth(
        terminatingButton.getPreferredSize().width);
    table.addMouseListener(new JTableButtonMouseListener(table));
    JScrollPane tableScroller = new JScrollPane(table);

    JButton newSegmentationJobButton = new JButton("Segmentation");
    newSegmentationJobButton.setToolTipText(
        "Segment the current image (stack) using U-Net");
    newSegmentationJobButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            _jobTableModel.createSegmentationJob();
          }});

    JButton newDetectionJobButton = new JButton("Detection");
    newDetectionJobButton.setToolTipText(
        "Detect objects in the current image (stack) using U-Net");
    newDetectionJobButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            _jobTableModel.createDetectionJob();
          }});

    JButton newFinetuneJobButton = new JButton("Finetuning");
    newFinetuneJobButton.setToolTipText(
        "Finetune U-Net model to new data based on ImageJ ROI annotations");
    newFinetuneJobButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            _jobTableModel.createFinetuneJob();
          }});

    JButton helpButton = new JButton("Help");
    helpButton.setToolTipText("Show README");
    helpButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (helpDialog == null) {
              helpDialog = new JDialog(
                  _manager, "U-Net Segmentation Help", false);
              helpDialog.getContentPane().setLayout(
                  new BorderLayout());
              JEditorPane helpPanel;
              try {
                helpPanel = new JEditorPane(
                    getClass().getResource("/resources/README.html"));
              }
              catch (java.io.IOException e2) {
                helpPanel = new JEditorPane(
                    "text/html", "404 - Internal Resource not found");
              }
              helpPanel.setEditable(false);
              helpPanel.addHyperlinkListener(
                  new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                      if (e.getEventType() ==
                          HyperlinkEvent.EventType.ACTIVATED) {
                        if (Desktop.isDesktopSupported()) {
                          try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                          }
                          catch (java.net.URISyntaxException e2) {}
                          catch (java.io.IOException e2) {}
                        }
                      }
                    }});
              JScrollPane helpScroller = new JScrollPane(helpPanel);
              helpDialog.add(helpScroller, BorderLayout.CENTER);
              helpDialog.setMinimumSize(new Dimension(600, 400));
            }
            helpDialog.setVisible(true);
          }});

    GridBagConstraints c = new GridBagConstraints();

    // First row: the table
    c.gridy = 0;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.CENTER;
    c.gridx = 0;
    c.gridwidth = 5;
    add(tableScroller, c);
    c.weighty = 0;
    c.fill = GridBagConstraints.NONE;

    // Second row: the buttons
    c.gridy = 1;
    c.gridx = 0;
    c.gridwidth = 1;
    c.anchor = GridBagConstraints.LAST_LINE_START;
    add(newSegmentationJobButton, c);
    c.gridx = 1;
    add(newDetectionJobButton, c);
    c.gridx = 2;
    add(newFinetuneJobButton, c);
    c.gridx = 3;
    c.anchor = GridBagConstraints.LAST_LINE_END;
    add(helpButton, c);

    setSize(getLayout().preferredLayoutSize(this));
  }

  public void addJob(Job job) {
    _jobTableModel.add(job);
  }

}

class ProgressCellRenderer extends JProgressBar implements TableCellRenderer {

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    if (value instanceof ProgressMonitor) {
      ProgressMonitor s = (ProgressMonitor)value;
      if (!getString().equals(s.message())) setString(s.message());
      if (getMaximum() != (int)s.getMax()) setMaximum((int)s.getMax());
      if (getValue() != (int)s.getCount()) setValue((int)s.getCount());
      if (isIndeterminate() != (s.getMax() == 0))
          setIndeterminate(s.getMax() == 0);
      if (!isStringPainted()) setStringPainted(true);
    }
    else if (value instanceof Float) {
      int progress = Math.round(((Float) value) * 100f);
      if (getValue() != progress) setValue(progress);
      if (!isStringPainted()) setStringPainted(true);
    }
    return this;
  }

}

class ComponentCellRenderer implements TableCellRenderer {

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    if (value instanceof Component) return (Component)value;
    return null;
  }
}

class JTableButtonMouseListener implements MouseListener {

  private JTable _table;
  private int _row, _column;
  private JButton _button = null;

  private boolean _forwardEventToButton(MouseEvent e) {
    _row = e.getY() / _table.getRowHeight();
    _column = _table.getColumnModel().getColumnIndexAtX(e.getX());
    _button = null;
    if (_row >= _table.getRowCount() || _row < 0 ||
        _column >= _table.getColumnCount() || _column < 0) return false;

    Object value = _table.getValueAt(_row, _column);
    if(!(value instanceof JButton)) return false;
    _button = (JButton)value;
    if (!_button.isEnabled()) return false;

    MouseEvent buttonEvent =
        (MouseEvent)SwingUtilities.convertMouseEvent(_table, e, _button);
    _button.dispatchEvent(buttonEvent);

    // This is necessary so that when a button is pressed and released
    // it gets rendered properly. Otherwise, the button may still appear
    // pressed down when it has been released.
    _table.repaint();

    return true;
  }

  public JTableButtonMouseListener(JTable table) {
    _table = table;
  }

  public void mouseClicked(MouseEvent e) {
    _forwardEventToButton(e);
    if (_button != null) _button.doClick();
  }

  public void mouseEntered(MouseEvent e) {
    _forwardEventToButton(e);
  }

  public void mouseExited(MouseEvent e) {
    _forwardEventToButton(e);
  }

  public void mousePressed(MouseEvent e) {
    _forwardEventToButton(e);
  }

  public void mouseReleased(MouseEvent e) {
    _forwardEventToButton(e);
  }

}
