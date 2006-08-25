// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/dataAccess/shape/EsriPointList.java,v $
// $RCSfile: EsriPointList.java,v $
// $Revision: 1.8 $
// $Date: 2006/08/25 15:36:12 $
// $Author: dietrick $
// 
// **********************************************************************

package com.bbn.openmap.dataAccess.shape;

import java.util.Iterator;

import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMPoint;
import com.bbn.openmap.omGraphics.OMScalingIcon;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.util.Debug;

/**
 * An EsriGraphicList ensures that only EsriPoints are added to its list.
 * 
 * @author Doug Van Auken
 * @author Don Dietrick
 */
public class EsriPointList extends EsriGraphicList {

    /**
     * Over-ride the add( ) method to trap for inconsistent shape geometry.
     * 
     * @param shape the non-null OMGraphic to add
     */
    public void add(OMGraphic shape) {
        try {

            if (typeMatches(shape)) {
                graphics.add(shape);
                addExtents(((EsriGraphic) shape).getExtents());
            } else if (shape instanceof OMPoint) {
                shape = EsriPoint.convert((OMPoint) shape);
                // test for null in next if statement.
            } else if (shape instanceof OMText) {
                shape = EsriTextPoint.convert((OMText) shape);
            } else if (shape instanceof OMScalingIcon) {
                shape = EsriIconPoint.convert((OMScalingIcon) shape);
            } else if (shape instanceof OMGraphicList
                    && !((OMGraphicList) shape).isVague()) {
                for (Iterator it = ((OMGraphicList) shape).iterator(); it.hasNext();) {
                    add((OMGraphic) it.next());
                }
            } else {
                Debug.message("esri",
                        "EsriPointList.add()- graphic isn't an EsriGraphic with matching type, can't add.");
                return;
            }
        } catch (ClassCastException cce) {
        }
    }

    public boolean typeMatches(OMGraphic omg) {
        return omg instanceof EsriGraphic
                && ((EsriGraphic) omg).getType() == getType();
    }

    /**
     * Construct an EsriPointList.
     */
    public EsriPointList() {
        super();
        setType(SHAPE_TYPE_POINT);
    }

    /**
     * Construct an EsriPointList with an initial capacity.
     * 
     * @param initialCapacity the initial capacity of the list
     */
    public EsriPointList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Construct an EsriPointList with an initial capacity and a standard
     * increment value.
     * 
     * @param initialCapacity the initial capacity of the list
     * @param capacityIncrement the capacityIncrement for resizing
     * @deprecated capacityIncrement doesn't do anything.
     */
    public EsriPointList(int initialCapacity, int capacityIncrement) {
        super(initialCapacity);
    }

    public EsriGraphic shallowCopy() {
        EsriPointList ret = new EsriPointList(size());
        ret.setAttributes(getAttributes());
        for (Iterator iter = iterator(); iter.hasNext();) {
            EsriGraphic g = (EsriGraphic) iter.next();
            ret.add((OMGraphic) g.shallowCopy());
        }
        return ret;
    }
}
