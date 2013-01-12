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

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.editor.BaseDocument;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author Sandip V. Chitale (Sandip.Chitale@Sun.Com)
 */
public class LineOperations {

    public  static final String FILE_SEPARATORS = "/\\";
    private static final String DOT = ".";
    private static final String DASH = "-";
    public  static final String FILE_SEPARATOR_DOT = File.separatorChar + DOT;
    public  static final String FILE_SEPARATOR_DOT_DASH = FILE_SEPARATOR_DOT + DASH;
    public  static final String FILE_SEPARATORS_DOT_DASH = FILE_SEPARATORS + DOT + DASH;

    static void exchangeDotAndMark(JEditorPane textComponent) {
        Caret caret = textComponent.getCaret();
        // check if there is a selection
        if (caret.isSelectionVisible()) {
            int selStart = caret.getDot();
            int selEnd = caret.getMark();
            caret.setDot(selStart);
            caret.moveDot(selEnd);
        }
    }

    static final void sortLinesAscending(JTextComponent textComponent) {
        sortLines(textComponent);
    }

    static final void sortLinesDescending(JTextComponent textComponent) {
        sortLines(textComponent, true);
    }

    static final void sortLines(JTextComponent textComponent) {
        sortLines(textComponent, false);
    }

    static final void sortLines(JTextComponent textComponent, boolean descending) {
        Caret caret = textComponent.getCaret();
        if (textComponent.isEditable() && caret.isSelectionVisible()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                int selStart = caret.getDot();
                int selEnd = caret.getMark();
                int start = Math.min(selStart, selEnd);
                int end =   Math.max(selStart, selEnd) - 1;

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                if (zeroBaseStartLineNumber == -1 || zeroBaseEndLineNumber == -1 || (zeroBaseStartLineNumber == zeroBaseEndLineNumber)) {
                    // could not get line number or same line
                    beep();
                    return;
                }

                int startOffset = rootElement.getElement(zeroBaseStartLineNumber).getStartOffset();
                int endOffset = rootElement.getElement(zeroBaseEndLineNumber).getEndOffset();

                try {
                    int numberOfLines = zeroBaseEndLineNumber - zeroBaseStartLineNumber + 1;
                    String[] linesText = new String[numberOfLines];
                    for (int i = 0; i < numberOfLines; i++) {
                        // get line text
                        Element lineElement = rootElement.getElement(zeroBaseStartLineNumber + i);
                        int lineStartOffset = lineElement.getStartOffset();
                        int lineEndOffset = lineElement.getEndOffset();

                        linesText[i] = doc.getText(lineStartOffset, (lineEndOffset - lineStartOffset));
                    }

                    Comparator comparator = null;
                    if (descending) {
                        if (matchCase) {
                            comparator = REVERSE_STRING_COMPARATOR;
                        } else {
                            comparator = REVERSE_STRING_COMPARATOR_CASE_INSENSITIVE;
                        }
                    } else {
                        if (matchCase) {
                        } else {
                            comparator = String.CASE_INSENSITIVE_ORDER;
                        }
                    }
                    
                    if (isRemoveDuplicateLines()) {
                        SortedSet<String> uniqifySet = new TreeSet<String>(matchCase ? null : String.CASE_INSENSITIVE_ORDER);
                        uniqifySet.addAll(Arrays.asList(linesText));
                        linesText = uniqifySet.toArray(new String[0]);
                    }
                    
                    if (comparator == null) {
                            Arrays.sort(linesText);
                    } else {
                        Arrays.sort(linesText, comparator);
                    }
                    
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < linesText.length; i++) {
                        sb.append(linesText[i]);
                    }

                    // remove the lines
                    doc.remove(startOffset, Math.min(doc.getLength(),endOffset) - startOffset);

                    // insert the sorted text
                    doc.insertString(startOffset, sb.toString(), null);

                } catch (BadLocationException ex) {
                    ErrorManager.getDefault().notify(ex);
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    /**
     * Holds value of property removeDuplicateLines.
     */
    private static boolean removeDuplicateLines;

    /**
     * Getter for property removeDuplicateLines.
     * @return Value of property removeDuplicateLines.
     */
    static boolean isRemoveDuplicateLines() {
        return removeDuplicateLines;
    }

    /**
     * Setter for property removeDuplicateLines.
     * @param removeDuplicateLines New value of property removeDuplicateLines.
     */
    static void setRemoveDuplicateLines(boolean removeDuplicateLines) {
        LineOperations.removeDuplicateLines = removeDuplicateLines;
    }

    private static boolean matchCase = true;

    /**
     * Return wheather the sorting shoul be done in a case sensitive fashion.
     * @return
     */
    public static boolean isMatchCase() {
        return matchCase;
    }

    /**
     * Set wheather the sorting shoul be done in a case sensitive fashion.
     *
     * @param matchCase
     */
    public static void setMatchCase(boolean matchCase) {
        LineOperations.matchCase = matchCase;
    }

    private static Comparator<String> REVERSE_STRING_COMPARATOR = Collections.reverseOrder();
    private static Comparator<String> REVERSE_STRING_COMPARATOR_CASE_INSENSITIVE = Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER);


    static void filter(JTextComponent textComponent) {
        Caret caret = textComponent.getCaret();
        if (textComponent.isEditable() && caret.isSelectionVisible()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                int selStart = caret.getDot();
                int selEnd = caret.getMark();
                int start = Math.min(selStart, selEnd);
                int end =   Math.max(selStart, selEnd) - 1;

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                if (zeroBaseStartLineNumber == -1 || zeroBaseEndLineNumber == -1) {
                    // could not get line number or same line
                    beep();
                    return;
                }

                NotifyDescriptor.InputLine filterCommand = new NotifyDescriptor.InputLine("Enter Filter command:",
                        "Filter command"
                        ,NotifyDescriptor.OK_CANCEL_OPTION
                        ,NotifyDescriptor.PLAIN_MESSAGE);

                if (DialogDisplayer.getDefault().notify(filterCommand) == NotifyDescriptor.OK_OPTION) {
                    int startOffset = rootElement.getElement(zeroBaseStartLineNumber).getStartOffset();
                    int endOffset = rootElement.getElement(zeroBaseEndLineNumber).getEndOffset();

                    try {
                        int numberOfLines = zeroBaseEndLineNumber - zeroBaseStartLineNumber + 1;
                        String[] linesText = new String[numberOfLines];
                        for (int i = 0; i < numberOfLines; i++) {
                            // get line text
                            Element lineElement = rootElement.getElement(zeroBaseStartLineNumber + i);
                            int lineStartOffset = lineElement.getStartOffset();
                            int lineEndOffset = lineElement.getEndOffset();

                            linesText[i] = doc.getText(lineStartOffset, (lineEndOffset - lineStartOffset - 1));
                        }

                        try {
                            FilterProcess filterProcess = new FilterProcess(filterCommand.getInputText().split(" "));

                            PrintWriter in = filterProcess.exec();
                            for (int i = 0; i < linesText.length; i++) {
                                in.println(linesText[i]);
                            }
                            in.close();
                            if (filterProcess.waitFor() == 0) {
                                linesText = filterProcess.getStdOutOutput();
                                if (linesText != null) {
                                    StringBuffer sb = new StringBuffer();
                                    for (int i = 0; i < linesText.length; i++) {
                                        sb.append(linesText[i] + "\n");
                                    }

                                    // remove the lines
                                    doc.remove(startOffset, Math.min(doc.getLength(),endOffset) - startOffset);

                                    // insert the sorted text
                                    doc.insertString(startOffset, sb.toString(), null);
                                }
                            }
                            filterProcess.destroy();
                        } catch (IOException fe) {
                            ErrorManager.getDefault().notify(ErrorManager.USER, fe);
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    static void filterOutput(JTextComponent textComponent) {
        Caret caret = textComponent.getCaret();
        if (textComponent.isEditable() && caret.isSelectionVisible()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                int selStart = caret.getDot();
                int selEnd = caret.getMark();
                int start = Math.min(selStart, selEnd);
                int end =   Math.max(selStart, selEnd) - 1;

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                if (zeroBaseStartLineNumber == -1 || zeroBaseEndLineNumber == -1) {
                    // could not get line number or same line
                    beep();
                    return;
                }

                NotifyDescriptor.InputLine filterCommand = new NotifyDescriptor.InputLine("Enter Filter command (output sent to Output window):",
                        "Filter command"
                        ,NotifyDescriptor.OK_CANCEL_OPTION
                        ,NotifyDescriptor.PLAIN_MESSAGE);

                if (DialogDisplayer.getDefault().notify(filterCommand) == NotifyDescriptor.OK_OPTION) {
                    int startOffset = rootElement.getElement(zeroBaseStartLineNumber).getStartOffset();
                    int endOffset = rootElement.getElement(zeroBaseEndLineNumber).getEndOffset();

                    try {
                        int numberOfLines = zeroBaseEndLineNumber - zeroBaseStartLineNumber + 1;
                        String[] linesText = new String[numberOfLines];
                        for (int i = 0; i < numberOfLines; i++) {
                            // get line text
                            Element lineElement = rootElement.getElement(zeroBaseStartLineNumber + i);
                            int lineStartOffset = lineElement.getStartOffset();
                            int lineEndOffset = lineElement.getEndOffset();

                            linesText[i] = doc.getText(lineStartOffset, (lineEndOffset - lineStartOffset - 1));
                        }

                        try {
                            FilterProcess filterProcess = new FilterProcess(filterCommand.getInputText().split(" "));

                            PrintWriter in = filterProcess.exec();
                            for (int i = 0; i < linesText.length; i++) {
                                in.println(linesText[i]);
                            }
                            in.close();
                            if (filterProcess.waitFor() == 0) {
                                InputOutput io = IOProvider.getDefault().getIO(filterCommand.getInputText(), true);
                                linesText = filterProcess.getStdOutOutput();
                                if (linesText != null) {
                                    PrintWriter pw = new PrintWriter(io.getOut());
                                    for (int i = 0; i < linesText.length; i++) {
                                        pw.println(linesText[i]);
                                    }
                                }
                                linesText = filterProcess.getStdErrOutput();
                                if (linesText != null) {
                                    PrintWriter pw = new PrintWriter(io.getErr());
                                    for (int i = 0; i < linesText.length; i++) {
                                        pw.println(linesText[i]);
                                    }
                                }
                            }
                            filterProcess.destroy();
                        } catch (IOException fe) {
                            ErrorManager.getDefault().notify(ErrorManager.USER, fe);
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    static final void fromChar(JTextComponent textComponent, char fromChar, boolean matchCase, int times) {
        if (textComponent.isEditable()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                Caret caret = textComponent.getCaret();
                int start = textComponent.getCaretPosition();

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);

                if (zeroBaseStartLineNumber == -1) {
                    // could not get line number
                    beep();
                    return;
                } else {
                    int startLineStartOffset = rootElement.getElement(zeroBaseStartLineNumber).getStartOffset();
                    try {
                        String text = doc.getText(startLineStartOffset, start - startLineStartOffset);

                        char lowercaseFromChar = Character.toLowerCase(fromChar);
                        int textLength = text.length();
                        for (int i = textLength-1; i >= 0; i--) {
                            char charAt = text.charAt(i);
                            if (charAt == fromChar || (!matchCase && Character.toLowerCase(charAt) == lowercaseFromChar)) {
                                times--;
                                if (times == 0) {
                                    caret.moveDot(startLineStartOffset + i);
                                    break;
                                }
                            }
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    static final void afterChar(JTextComponent textComponent, char afterChar, boolean matchCase, int times) {
        if (textComponent.isEditable()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                Caret caret = textComponent.getCaret();
                int start = textComponent.getCaretPosition();

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);

                if (zeroBaseStartLineNumber == -1) {
                    // could not get line number
                    beep();
                    return;
                } else {
                    int startLineStartOffset =  rootElement.getElement(zeroBaseStartLineNumber).getStartOffset();
                   try {
                        String text = doc.getText(startLineStartOffset, start - startLineStartOffset);

                        char lowercaseAfterChar = Character.toLowerCase(afterChar);
                        int textLength = text.length();
                        for (int i = textLength-1; i >= 0; i--) {
                            char charAt = text.charAt(i);
                            if (charAt == afterChar || (!matchCase && Character.toLowerCase(charAt) == lowercaseAfterChar)) {
                                times--;
                                if (times == 0) {
                                    caret.moveDot(startLineStartOffset + i + 1);
                                    break;
                                }
                            }
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    static final void uptoChar(JTextComponent textComponent, char uptoChar, boolean matchCase, int times) {
        if (textComponent.isEditable()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                Caret caret = textComponent.getCaret();
                int start = textComponent.getCaretPosition();

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);

                if (zeroBaseStartLineNumber == -1) {
                    // could not get line number
                    beep();
                    return;
                } else {
                    int startLineEndOffset = rootElement.getElement(zeroBaseStartLineNumber).getEndOffset();
                    try {
                        String text = doc.getText(start + 1, startLineEndOffset - start - 1);

                        char lowercaseUptoChar = Character.toLowerCase(uptoChar);
                        int textLength = text.length();
                        for (int i = 0; i < textLength; i++) {
                            char charAt = text.charAt(i);
                            if (charAt == uptoChar || (!matchCase && Character.toLowerCase(charAt) == lowercaseUptoChar)) {
                                times--;
                                if (times == 0) {
                                    caret.moveDot(start+1+i);
                                    break;
                                }
                            }
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    static final void toChar(JTextComponent textComponent, char toChar, boolean matchCase, int times) {
        if (textComponent.isEditable()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                Caret caret = textComponent.getCaret();
                int start = textComponent.getCaretPosition();

                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);

                if (zeroBaseStartLineNumber == -1) {
                    // could not get line number
                    beep();
                    return;
                } else {
                    int startLineEndOffset = rootElement.getElement(zeroBaseStartLineNumber).getEndOffset();
                    try {
                        String text = doc.getText(start + 1, startLineEndOffset - start - 1);

                        char lowercaseToChar = Character.toLowerCase(toChar);
                        int textLength = text.length();
                        for (int i = 0; i < textLength; i++) {
                            char charAt = text.charAt(i);
                            if (charAt == toChar || (!matchCase && Character.toLowerCase(charAt) == lowercaseToChar)) {
                                times--;
                                if (times == 0) {
                                    caret.moveDot(start+1+i+1);
                                    break;
                                }
                            }
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    static final void cycle(JTextComponent textComponent, String cycleString) {
        if (textComponent.isEditable()) {
            Document doc = textComponent.getDocument();
            if (doc instanceof BaseDocument) {
                ((BaseDocument)doc).atomicLock();
            }
            try {
                Element rootElement = doc.getDefaultRootElement();

                Caret caret = textComponent.getCaret();
                boolean selection = false;
                boolean backwardSelection = false;
                int start = textComponent.getCaretPosition();
                int end = start;

                // check if there is a selection
                if (caret.isSelectionVisible()) {
                    int selStart = caret.getDot();
                    int selEnd = caret.getMark();
                    start = Math.min(selStart, selEnd);
                    end =   Math.max(selStart, selEnd);
                    selection = true;
                    backwardSelection = (selStart >= selEnd);
                }


                int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                if (zeroBaseStartLineNumber == -1) {
                    // could not get line number
                    beep();
                    return;
                } else {
                    try {
                        // get line text
                        Element startLineElement = rootElement.getElement(zeroBaseStartLineNumber);
                        int startLineStartOffset = startLineElement.getStartOffset();

                        Element endLineElement = rootElement.getElement(zeroBaseEndLineNumber);
                        int endLineEndOffset = endLineElement.getEndOffset();

                        if (!selection) {
                            start = startLineStartOffset;
                            end = endLineEndOffset;
                        }

                        String linesText = doc.getText(start, (end - start));

                        linesText = cycle(linesText, cycleString);

                        // replace the line or selection
                        doc.remove(start, Math.min(doc.getLength(),end) - start);

                        // insert the text before the previous line
                        doc.insertString(start, linesText, null);

                        if (selection) {
                            if (backwardSelection) {
                                caret.setDot(start);
                                caret.moveDot(end);
                            } else {
                                caret.setDot(end);
                                caret.moveDot(start);
                            }
                        } else {
                            // set caret position
                            textComponent.setCaretPosition(start);
                        }
                    } catch (BadLocationException ex) {
                        ErrorManager.getDefault().notify(ex);
                    }
                }
            } finally {
                if (doc instanceof BaseDocument) {
                    ((BaseDocument)doc).atomicUnlock();
                }
            }
        } else {
            beep();
        }
    }

    public static String cycle(String target, String cycleChars) {
        if (target == null) {
            return null;
        }

        if (cycleChars == null) {
            return target;
        }

        Set<Character> cycleSet = getCharSet(cycleChars);
        if (cycleSet.size() <= 1){
            return target;
        }

        Set<Character> set = getCharSet(target);
        set.retainAll(cycleSet);
        switch (set.size()) {
            case 0:
                return target;
            case 1:
                char from = set.iterator().next();
                List<Character> cycleList = new ArrayList<Character>(cycleSet);
                char to = cycleList.get((cycleList.indexOf(from) + 1)%cycleList.size());
                return target.replace(from,to);
            default:
                char first = set.iterator().next();
                cycleSet.remove(first);
                Iterator<Character> cycleSetIterator = cycleSet.iterator();
                while(cycleSetIterator.hasNext()) {
                    target = target.replace(cycleSetIterator.next(), first);
                }
                return target;
        }
    }

    private static Set<Character> getCharSet(String target) {
        if (target == null) {
            return null;
        }

        if (target.length() == 0) {
            return new LinkedHashSet<Character>();
        }

        if (target.length() == 1) {
            return new LinkedHashSet<Character>(Collections.<Character>singleton(target.charAt(0)));
        }

        char[] targetarray = target.toCharArray();
        Character[] targetArray = new Character[targetarray.length];
        for (int i = 0; i < targetarray.length; i++) {
            targetArray[i] = targetarray[i];
        }

        Set<Character> targetCharsSet = new LinkedHashSet<Character>(Arrays.<Character>asList(targetArray));

        return targetCharsSet;
    }

    static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }
}
