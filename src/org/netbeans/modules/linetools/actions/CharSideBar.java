/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.linetools.actions;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.netbeans.editor.Registry;
import org.netbeans.editor.SideBarFactory;
import org.openide.awt.StatusDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * This action installs the Firefox(TM) style Incremental Search Side Bar.
 *
 * @author Sandip V. Chitale (Sandip.Chitale@Sun.Com)
 */
public final class CharSideBar extends JToolBar implements HelpCtx.Provider  {
    private JButton        closeButton;
    private JLabel         charLabel;
    private JTextField     charTextField;
    private JCheckBox      matchCaseCheckBox;
    private JLabel         matchCaseLabel;

    static enum MODE {
        FROM,
        AFTER,
        UPTO,
        TO
    };

    private MODE mode = MODE.TO;

    public CharSideBar() {
        setFloatable(false);
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        // ESCAPE to put focus back in the editor
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
                "loose-focus"); // NOI18N
        getActionMap().put("loose-focus", // NOI18N
                new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                looseFocus();
            }
        });

        closeButton = new JButton(" ",
                new ImageIcon(Utilities.loadImage("org/netbeans/modules/linetools/actions/close.gif"))); // NOI18N
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                looseFocus();
            }
        });
        closeButton.setToolTipText(NbBundle.getMessage(CharSideBar.class, "TOOLTIP_CharSidebar")); // NOI18N

        charLabel = new JLabel(""); // NOI18N

        // configure incremental search text field
        charTextField = new JTextField(6) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        charTextField.setToolTipText(NbBundle.getMessage(CharSideBar.class, "TOOLTIP_Char")); // NOI18N

        // listen on text change
        charTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
            }
            public void insertUpdate(DocumentEvent e) {
                tryCharOperation();
            }
            public void removeUpdate(DocumentEvent e) {
                tryCharOperation();
            }

            private void tryCharOperation() {
                StatusDisplayer.getDefault().setStatusText("");
                // text changed - zap
                String text = charTextField.getText();
                if (text.length() > 0) {
                    try {
                        Integer.parseInt(text);
                        StatusDisplayer.getDefault().setStatusText(
                                NbBundle.getMessage(CharSideBar.class, "MSG_ForceOperation")); // NOI18N
                    } catch (NumberFormatException nfe) {
                        doCharOperation(text);
                    }
                }
            }
        });

        charTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = charTextField.getText();
                if (text.length() > 0) {
                    doCharOperation(text);
                } else {
                    LineOperations.beep();
                }
            }
        });

        // configure match case check box
        matchCaseCheckBox = new JCheckBox("", true);
        matchCaseCheckBox.setFocusPainted(false);
        matchCaseCheckBox.setMargin(new Insets(1,5,1,5));
        matchCaseCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                charTextField.requestFocusInWindow();
            }
        });
        matchCaseLabel = new JLabel(NbBundle.getMessage(CharSideBar.class, "CTL_MatchCase")); // NOI18N

        add(closeButton);
        add(charLabel);
        add(charTextField);
        add(matchCaseCheckBox);
        add(matchCaseLabel);

        // padding
        add(new JPanel());

        // initially not visible
        setVisible(false);

        setMode(MODE.TO);
    }

    private void doCharOperation(String chars) {
        if (chars == null || chars.length() == 0) {
            return;
        }
        int times = 1;
        char charAt = chars.charAt(0);
        if (Character.isDigit(charAt)) {
            if (chars.length() == 1) {
                doCharOperation(charAt, times);
            } else {
                charAt = chars.charAt(chars.length() -1);
                chars = chars.substring(0, chars.length() - 1);
                try {
                    times = Integer.parseInt(chars);
                    doCharOperation(charAt, times);
                } catch (NumberFormatException nfe) {
                    LineOperations.beep();
                }
            }
        } else {
            doCharOperation(charAt, times);
        }
    }

    private void doCharOperation(char opChar, int times) {
        looseFocus();
        switch (mode) {
            case FROM:
                LineOperations.fromChar(Registry.getMostActiveComponent(), opChar, matchCaseCheckBox.isSelected(), times);
                break;
            case AFTER:
                LineOperations.afterChar(Registry.getMostActiveComponent(), opChar, matchCaseCheckBox.isSelected(), times);
                break;
            case UPTO:
                LineOperations.uptoChar(Registry.getMostActiveComponent(), opChar, matchCaseCheckBox.isSelected(), times);
                break;
            case TO:
                LineOperations.toChar(Registry.getMostActiveComponent(), opChar, matchCaseCheckBox.isSelected(), times);
                break;
        }
    }

    private void gainFocus() {
        setVisible(true);
        charTextField.requestFocusInWindow();
        charTextField.setText("");
    }

    private void looseFocus() {
        setVisible(false);
        JTextComponent textComponent = Registry.getMostActiveComponent();
        if (textComponent != null && textComponent.isEnabled()) {
            textComponent.requestFocusInWindow();
        }
    }

    public CharSideBar.MODE getMode() {
        return mode;
    }

    public void setMode(CharSideBar.MODE mode) {
        this.mode = mode;
        switch (this.mode) {
            case FROM:
                charLabel.setText(NbBundle.getMessage(CharSideBar.class, "CTL_CharFrom")); // NOI18N
                break;
            case AFTER:
                charLabel.setText(NbBundle.getMessage(CharSideBar.class, "CTL_CharAfter")); // NOI18N
                break;
            case UPTO:
                charLabel.setText(NbBundle.getMessage(CharSideBar.class, "CTL_CharUpto")); // NOI18N
                break;
            case TO:
                charLabel.setText(NbBundle.getMessage(CharSideBar.class, "CTL_CharTo")); // NOI18N
                break;
        }
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.netbeans.modules.linetools..about"); // NOI18N
    }

    /**
     * Factory for creating the incremental search sidebar
     */
    public static final class Factory implements SideBarFactory {
        public JComponent createSideBar(JTextComponent target) {
            final CharSideBar zapCharSideBar = new CharSideBar();
            target.getInputMap().put(KeyStroke.getKeyStroke("control alt released COMMA"),
                    "char-from"); // NOI18N
            target.getActionMap().put("char-from", // NOI18N
                    new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    zapCharSideBar.setMode(CharSideBar.MODE.FROM);
                    zapCharSideBar.gainFocus();
                }
            });
            target.getInputMap().put(KeyStroke.getKeyStroke("control alt shift released COMMA"),
                    "char-after"); // NOI18N
            target.getActionMap().put("char-after", // NOI18N
                    new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    zapCharSideBar.setMode(CharSideBar.MODE.AFTER);
                    zapCharSideBar.gainFocus();
                }
            });
            target.getInputMap().put(KeyStroke.getKeyStroke("control alt shift released PERIOD"),
                    "char-upto"); // NOI18N
            target.getActionMap().put("char-upto", // NOI18N
                    new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    zapCharSideBar.setMode(CharSideBar.MODE.UPTO);
                    zapCharSideBar.gainFocus();
                }
            });
            target.getInputMap().put(KeyStroke.getKeyStroke("control alt released PERIOD"),
                    "char-to"); // NOI18N
            target.getActionMap().put("char-to", // NOI18N
                    new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    zapCharSideBar.setMode(CharSideBar.MODE.TO);
                    zapCharSideBar.gainFocus();
                }
            });
            return zapCharSideBar;
        }
    }
}
