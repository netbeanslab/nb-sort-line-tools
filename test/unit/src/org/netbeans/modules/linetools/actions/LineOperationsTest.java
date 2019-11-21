/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2019 Sun Microsystems, Inc. All rights reserved.
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

import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.netbeans.editor.BaseDocument;
import org.netbeans.junit.NbTestCase;

public class LineOperationsTest extends NbTestCase {

    public LineOperationsTest(String name) {
        super(name);
    }

    @Override
    protected boolean runInEQ() {
        return true;
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    @Override
    public void setUp() {
    }

    @AfterEach
    @Override
    public void tearDown() {
    }

    @Test
    public void testSortLinesAscending_01() throws Exception {
        String text = ""
                + "z\n"
                + "y\n"
                + "x\n"
                + "b\n"
                + "c\n"
                + "a\n";
        String expected = ""
                + "a\n"
                + "b\n"
                + "c\n"
                + "x\n"
                + "y\n"
                + "z\n";
        testSortLinesAsc(text, expected);
    }

    @Test
    public void testSortLinesDescending_01() throws Exception {
        String text = ""
                + "a\n"
                + "b\n"
                + "c\n"
                + "x\n"
                + "y\n"
                + "z\n";
        String expected = ""
                + "z\n"
                + "y\n"
                + "x\n"
                + "c\n"
                + "b\n"
                + "a\n";
        testSortLinesDesc(text, expected);
    }

    private void testSortLinesAsc(String text, String expected) throws Exception {
        testSortLines(text, expected, true);
    }

    private void testSortLinesDesc(String text, String expected) throws Exception {
        testSortLines(text, expected, false);
    }

    private void testSortLines(String text, String expected, boolean asc) throws Exception {
        JTextComponent textComponent = new JEditorPane();
        BaseDocument document = new BaseDocument(false, "text/plain");
        textComponent.setDocument(document);
        document.insertString(0, text, null);
        textComponent.setSelectionStart(0);
        textComponent.setSelectionEnd(document.getLength());
        textComponent.getCaret().setSelectionVisible(true);
        if (asc) {
            LineOperations.sortLinesAscending(textComponent);
        } else {
            LineOperations.sortLinesDescending(textComponent);
        }
        String actual = document.getText(0, document.getLength());
        assertEquals(expected, actual);
    }

}
