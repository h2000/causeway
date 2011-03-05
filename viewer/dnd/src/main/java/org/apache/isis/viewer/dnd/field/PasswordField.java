/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.isis.viewer.dnd.field;

import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.runtimes.dflt.runtime.context.IsisContext;
import org.apache.isis.viewer.dnd.drawing.Canvas;
import org.apache.isis.viewer.dnd.drawing.Color;
import org.apache.isis.viewer.dnd.drawing.ColorsAndFonts;
import org.apache.isis.viewer.dnd.drawing.Size;
import org.apache.isis.viewer.dnd.drawing.Text;
import org.apache.isis.viewer.dnd.util.Properties;
import org.apache.isis.viewer.dnd.view.Content;
import org.apache.isis.viewer.dnd.view.Toolkit;
import org.apache.isis.viewer.dnd.view.UserActionSet;
import org.apache.isis.viewer.dnd.view.ViewSpecification;
import org.apache.isis.viewer.dnd.view.content.TextParseableContent;
import org.apache.isis.viewer.dnd.view.text.TextContent;


public class PasswordField extends TextField {
    protected static final Text style = Toolkit.getText(ColorsAndFonts.TEXT_NORMAL);
    private int maxTextWidth;
    private char echoCharacter;

    public PasswordField(final Content content, final ViewSpecification design) {
        super((TextParseableContent) content, design, true, TextContent.NO_WRAPPING);
        setMaxTextWidth(TEXT_WIDTH);
        final String echoCharacterSetting = IsisContext.getConfiguration().getString(Properties.PROPERTY_BASE + "echo");
        if (echoCharacterSetting == null || echoCharacterSetting.equals(" ")) {
            echoCharacter = '*';
        } else {
            echoCharacter = echoCharacterSetting.charAt(0);
        }
    }

    @Override
    public void contentMenuOptions(final UserActionSet options) {
        options.add(new ClearValueOption(this));
        options.setColor(Toolkit.getColor(ColorsAndFonts.COLOR_MENU_VALUE));
    }

    protected boolean provideClearCopyPaste() {
        return false;
    }

    /**
     * Only allow deletion of last character, ie don;t allow editing of the internals of the password.
     */
    public void delete() {
        textContent.deleteLeft(cursor);
        cursor.left();
        selection.resetTo(cursor);
        changeMade();
    }

    /**
     * disable left key.
     */
    protected void left(boolean alt, boolean shift) {}

    /**
     * disable right key.
     */
    protected void right(boolean alt, boolean shift) {}

    /**
     * disable home key.
     */
    protected void home(boolean alt, boolean shift) {}

    /**
     * disable end key.
     */
    protected void end(boolean alt, boolean shift) {}

    /**
     * disable page down key.
     */
    protected void pageDown(boolean shift, boolean ctrl) {}

    /**
     * disable page up key.
     */
    protected void pageUp(boolean shift, boolean ctrl) {}

    private String echoPassword(String password) {
        final int length = password.length();
        String echoedPassword = "";
        for (int i = 0; i < length; i++) {
            echoedPassword += echoCharacter;
        }
        return echoedPassword;
    }

    @Override
    public Size getRequiredSize(Size availableSpace) {
        final int width = HPADDING + maxTextWidth + HPADDING;
        int height = style.getTextHeight() + VPADDING;
        height = Math.max(height, Toolkit.defaultFieldHeight());

        return new Size(width, height);
    }

    /**
     * Set the maximum width of the field, as a number of characters
     */
    private void setMaxTextWidth(final int noCharacters) {
        maxTextWidth = style.charWidth('o') * noCharacters;
    }

    protected void align() {}

    protected void drawHighlight(Canvas canvas, int maxWidth) {}

    @Override
    protected void drawLines(final Canvas canvas, final Color color, final int width) {
        final int baseline = getBaseline();
        canvas.drawLine(HPADDING, baseline, HPADDING + width, baseline, color);
    }

    @Override
    protected void drawText(final Canvas canvas, final Color textColor, final int width) {

        final String[] lines = textContent.getDisplayLines();
        if (lines.length > 1) {
            throw new IsisException("Password field should contain a string that contains no line breaks; contains "
                    + lines.length);
        }

        final String chars = lines[0];
        if (chars == null) {
            throw new IsisException();
        }
        if (chars.endsWith("\n")) {
            throw new IsisException();
        }

        final int baseline = getBaseline();
        String echoPassword = echoPassword(chars);

        // draw cursor
        if (hasFocus() && canChangeValue().isAllowed()) {
            final int pos = style.stringWidth(echoPassword) - HPADDING;
            Color color = Toolkit.getColor(ColorsAndFonts.COLOR_TEXT_CURSOR);
            canvas.drawLine(pos, (baseline + style.getDescent()), pos, baseline - style.getAscent(), color);
        }

        // draw text
        canvas.drawText(echoPassword, HPADDING, baseline, textColor, style);
    }

}
