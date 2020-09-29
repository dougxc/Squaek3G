/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package example.shell;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.microedition.io.*;

/**
 * An input dialog that presents the user with a list of choices.
 *
 * @author Doug Simon
 */
class ChoiceDialog extends Dialog implements ActionListener {

    private String selection;
    private Choice list;
    private final Hashtable choices;

    public static String show(Frame owner, String title, Hashtable choices) {
        ChoiceDialog dialog = new ChoiceDialog(owner, title, choices);
        return dialog.selection;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "OK") {
            selection = (String)choices.get(list.getSelectedItem());
        } else {
            selection = null;
        }
        dispose();
    }

    private Button createButton(String label) {
        Button button = new Button(label);
        button.setActionCommand(label);
        button.addActionListener(this);
        return button;
    }

    private ChoiceDialog(Frame owner, String title, Hashtable shells) {
        super(owner, title, true);
        this.choices = shells;

        Panel buttons = new Panel();
        buttons.add(createButton("OK"));
        buttons.add(createButton("Cancel"));

        list = new Choice();
        Enumeration keys = shells.keys();
        while (keys.hasMoreElements()) {
            list.add((String)keys.nextElement());
        }

        add("Center", list);
        add("South", buttons);
        pack();
        show();
    }

}

