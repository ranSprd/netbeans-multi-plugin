/*
 * Copyright (c) 2008, R.Nagel <kiar@users.sourceforge.net>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright 
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the author nor the names of its contributors
 *       may be used to endorse or promote products derived from this
 *       software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT 
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY 
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * 
 */
// created by : R.Nagel <kiar@users.sourceforge.net>, 07.03.2008
//
// function   : model, contains the TopCompoment's
//
// todo       :
//
// modified   : 
package net.sf.openedfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Contains the ListModel
 */
public class OpenFilesModel {

    private static final Logger logger = Logger.getLogger(OpenFilesModel.class.getName());

    // the list which is used for the view
    private final ArrayList<OpenedListItem> modelList = new ArrayList<>();

    public final Mode findEditorMode() {
        Mode editorMode = null;
        
        try {
            // get a set of all available modes
            Set<? extends Mode> modes = WindowManager.getDefault().getModes();

            // make it robust
            if (modes != null) {
                editorMode = modes.stream()
                                .filter(mode -> mode != null)
                                .filter(mode -> "editor".equals( mode.getName()))
                                .findAny()
                                .orElse(null);
                }
        } catch (Exception e) {
            // if any error occurs, throws nothing
            editorMode = null;
        }
        return editorMode;
    }

    public List<TopComponent> getTCs() {
        return modelList.stream()
                        .map(item -> item.getTopComponent())
                        .collect( Collectors.toList());
    }

    // ----------------------
    public final void logActivation(TopComponent topComp) {
        OpenedListItem item = findItem(topComp);
        if (item != null) {
            item.logActivation();
        }
    }

    public final List<TopComponent> readOpenedWindows() {
        List<TopComponent> result = new ArrayList<>();
        Mode editorMode = findEditorMode();
        try {
            if (editorMode != null) {
                TopComponent comps[] = editorMode.getTopComponents();

                if (comps != null) {
                    for (TopComponent single : comps) {
                        if (single != null && single.isOpened()) {
                            result.add(single);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "unable to read the list of open windows", e);
        }
        return result;
    }

    /**
     * import the preList and make it to the model
     * @param preList
     * @param sortBy
     */
    public final void updateModel(List<TopComponent> preList, String sortBy) {
        // prepare the current open items
        ArrayList<OpenedListItem> tempList = new ArrayList<>();

        for (Iterator<TopComponent> it = preList.iterator(); it.hasNext();) {
            TopComponent component = it.next();
            OpenedListItem item = findItem(component);

            // new: create a new Item and log its activity
            if (item == null) {
                item = new OpenedListItem(component);
                item.logActivation();
            }

            tempList.add(item);
        }

        switch(sortBy) {
            case "ASC":
                sortByASC(tempList);
            break;
            case "DESC":
                sortByDESC(tempList);
            break;
            default:
                sortByLastRecentUsage(tempList);
            break;
        }
       

        synchronized (modelList) {
            modelList.clear();
            modelList.addAll(tempList);
        }

        // do some other NO AWT stuff here
    }

    private void sortByLastRecentUsage(ArrayList<OpenedListItem> tempList) {
        // sort by last recent usage
        Collections.sort(tempList, new Comparator<OpenedListItem>() {
            @Override
            public int compare(OpenedListItem o1, OpenedListItem o2) {
                return (int) (o2.getLastActivation() - o1.getLastActivation());
            }
        });
    }

    private void sortByASC(ArrayList<OpenedListItem> tempList) {
        // sort by last recent usage
        Collections.sort(tempList, new Comparator<OpenedListItem>() {
            @Override
            public int compare(OpenedListItem o1, OpenedListItem o2) {
                return (int) (o1.getTopComponent().getName().compareTo(o2.getTopComponent().getName()));
            }
            
        });
    }

    private void sortByDESC(ArrayList<OpenedListItem> tempList) {
        // sort by last recent usage
        Collections.sort(tempList, new Comparator<OpenedListItem>() {
            @Override
            public int compare(OpenedListItem o1, OpenedListItem o2) {
                return (int) (o2.getTopComponent().getName().compareTo(o1.getTopComponent().getName()));
            }
        });
    }

    public final void updateModel(String sortBy) {
        List<TopComponent> openEditors = this.readOpenedWindows();
        this.updateModel(openEditors, sortBy);
    }

    public final OpenedListItem findItem(TopComponent topComp) {
        if (topComp == null) {
            return null;
        }

        OpenedListItem temp = new OpenedListItem(topComp);
        for (OpenedListItem item : modelList) {
            if (item.equals(temp)) {
                return item;
            }
        }

        return null;
    }

}
