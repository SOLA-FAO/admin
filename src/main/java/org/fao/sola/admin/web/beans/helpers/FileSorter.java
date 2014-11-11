/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.fao.sola.admin.web.beans.helpers;

import java.io.File;
import java.util.Comparator;

/**
 * Allows to sort array of {@link File} entity by item order.
 */
public class FileSorter implements Comparator<File>{

    @Override
    public int compare(File f1, File f2) {
        if(f1.lastModified() > f2.lastModified())
            return -1;
        else if(f1.lastModified() < f2.lastModified())
            return 1;
        else
            return 0;
    }
    
}
