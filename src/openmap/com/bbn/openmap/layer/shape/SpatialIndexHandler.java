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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/shape/SpatialIndexHandler.java,v $
// $RCSfile: SpatialIndexHandler.java,v $
// $Revision: 1.9 $
// $Date: 2006/08/25 15:36:15 $
// $Author: dietrick $
// 
// **********************************************************************

package com.bbn.openmap.layer.shape;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.bbn.openmap.Environment;
import com.bbn.openmap.I18n;
import com.bbn.openmap.Layer;
import com.bbn.openmap.PropertyConsumer;
import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.shape.SpatialIndex.Entry;
import com.bbn.openmap.omGraphics.DrawingAttributes;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PropUtils;

/**
 * The SpatialIndexHandler keeps track of all the stuff dealing with a
 * particular shape file - file names, colors, etc. You can ask it to create
 * OMGraphics based on a bounding box, and make adjustments to it through its
 * GUI.
 */
public class SpatialIndexHandler implements PropertyConsumer {
    public SpatialIndex spatialIndex;
    public String shapeFileName = null;
    public String spatialIndexFileName = null;
    public String imageURLString = null;

    protected String prettyName = null;
    protected DrawingAttributes drawingAttributes;
    protected boolean enabled = true;
    protected boolean buffered = false;
    protected String propertyPrefix;

    public final static String EnabledProperty = "enabled";
    public final static String BufferedProperty = "buffered";

    // for internationalization
    protected I18n i18n = Environment.getI18n();

    public SpatialIndexHandler() {}

    public SpatialIndexHandler(String prefix, Properties props) {
        setProperties(prefix, props);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("For " + prettyName + ":\n");
        sb.append("  Shape file name: " + shapeFileName + "\n");
        sb.append("  Spatal index file name: " + spatialIndexFileName + "\n");
        sb.append("  image URL: " + imageURLString + "\n");
        sb.append("  drawing attributes: " + drawingAttributes + "\n");
        return sb.toString();
    }

    /**
     * Get the GUI that controls the attributes of the handler.
     */
    public JComponent getGUI() {
        JPanel stuff = new JPanel();
        stuff.setBorder(BorderFactory.createRaisedBevelBorder());
        // stuff.add(new JLabel(prettyName));
        stuff.add(drawingAttributes.getGUI());

        JPanel checks = new JPanel(new GridLayout(0, 1));
        JCheckBox enableButton = new JCheckBox(i18n.get(SpatialIndexHandler.class,
                "enableButton",
                "Show"));
        enableButton.setSelected(enabled);
        enableButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JCheckBox jcb = (JCheckBox) ae.getSource();
                enabled = jcb.isSelected();
            }
        });
        checks.add(enableButton);

        JCheckBox bufferButton = new JCheckBox(i18n.get(SpatialIndexHandler.class,
                "bufferButton",
                "Buffer"));
        bufferButton.setSelected(buffered);
        bufferButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JCheckBox jcb = (JCheckBox) ae.getSource();
                buffered = jcb.isSelected();
            }
        });
        checks.add(bufferButton);
        stuff.add(checks);

        return stuff;
    }

    /** Property Consumer method. */
    public void setPropertyPrefix(String prefix) {
        propertyPrefix = prefix;
    }

    /** Property Consumer method. */
    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    /** Property Consumer method. */
    public void setProperties(Properties props) {
        setProperties(null, props);
    }

    /** Property Consumer method. */
    public void setProperties(String prefix, Properties props) {
        setPropertyPrefix(prefix);
        String realPrefix = PropUtils.getScopedPropertyPrefix(this);
        prettyName = props.getProperty(realPrefix + Layer.PrettyNameProperty);
        shapeFileName = props.getProperty(realPrefix
                + ShapeLayer.shapeFileProperty);
        spatialIndexFileName = props.getProperty(realPrefix
                + ShapeLayer.spatialIndexProperty);

        if (shapeFileName != null && !shapeFileName.equals("")) {
            if (spatialIndexFileName != null
                    && !spatialIndexFileName.equals("")) {
                spatialIndex = SpatialIndex.locateAndSetShapeData(shapeFileName,
                        spatialIndexFileName);
            } else {
                spatialIndex = SpatialIndex.locateAndSetShapeData(shapeFileName);
            }

            imageURLString = props.getProperty(realPrefix
                    + ShapeLayer.pointImageURLProperty);

            try {
                if (imageURLString != null && !imageURLString.equals("")) {
                    URL imageURL = PropUtils.getResourceOrFileOrURL(this,
                            imageURLString);
                    ImageIcon imageIcon = new ImageIcon(imageURL);
                    spatialIndex.setPointIcon(imageIcon);
                }
            } catch (MalformedURLException murle) {
                Debug.error("MultiShapeLayer.setProperties(" + realPrefix
                        + ": point image URL not so good: \n\t"
                        + imageURLString);

            } catch (NullPointerException npe) {
                // May happen if not connected to the internet.
                Debug.error("Can't access icon image: \n" + imageURLString);
            }

        } else {
            Debug.error(realPrefix
                    + ": One of the following properties was null or empty:");
            Debug.error("\t" + realPrefix + ShapeLayer.shapeFileProperty);
            Debug.error("\t" + realPrefix + ShapeLayer.spatialIndexProperty);
        }
        props.put(ShapeLayer.spatialIndexProperty,
                "Location of Spatial Index file - .ssx (File, URL or relative file path).");
        drawingAttributes = new DrawingAttributes(realPrefix, props);

        enabled = PropUtils.booleanFromProperties(props, realPrefix
                + EnabledProperty, enabled);
        buffered = PropUtils.booleanFromProperties(props, realPrefix
                + BufferedProperty, buffered);
    }

    /** Property Consumer method. */
    public Properties getProperties(Properties props) {
        if (props == null) {
            props = new Properties();
        }

        String prefix = PropUtils.getScopedPropertyPrefix(this);
        props.put(prefix + ShapeLayer.shapeFileProperty,
                (shapeFileName == null ? "" : shapeFileName));
        props.put(prefix + ShapeLayer.spatialIndexProperty,
                (spatialIndexFileName == null ? "" : spatialIndexFileName));
        props.put(prefix + ShapeLayer.pointImageURLProperty,
                (imageURLString == null ? "" : imageURLString));

        if (drawingAttributes != null) {
            drawingAttributes.getProperties(props);
        } else {
            DrawingAttributes da = (DrawingAttributes) DrawingAttributes.DEFAULT.clone();
            da.setPropertyPrefix(prefix);
            da.getProperties(props);
        }
        props.put(prefix + EnabledProperty, new Boolean(enabled).toString());
        props.put(prefix + BufferedProperty, new Boolean(buffered).toString());
        return props;
    }

    /** Property Consumer method. */
    public Properties getPropertyInfo(Properties props) {
        if (props == null) {
            props = new Properties();
        }
        String interString;

        // those strings are already internationalized in ShapeLayer.
        // So only thing to do is use
        // keys and values from there.The main question is: what about
        // .class?
        // What should I use as requestor field when calling
        // i18n.get(...) ? DFD - use the ShapeLayer class, so you
        // only have to modify one properties file with the
        // translation.

        interString = i18n.get(ShapeLayer.class,
                ShapeLayer.shapeFileProperty,
                I18n.TOOLTIP,
                "Location of Shape file - .shp (File, URL or relative file path).");
        props.put(ShapeLayer.shapeFileProperty, interString);
        interString = i18n.get(ShapeLayer.class,
                ShapeLayer.shapeFileProperty,
                ShapeLayer.shapeFileProperty);
        props.put(ShapeLayer.shapeFileProperty + LabelEditorProperty,
                interString);
        props.put(ShapeLayer.shapeFileProperty + ScopedEditorProperty,
                "com.bbn.openmap.util.propertyEditor.FUPropertyEditor");

        interString = i18n.get(ShapeLayer.class,
                ShapeLayer.spatialIndexProperty,
                I18n.TOOLTIP,
                "Location of Spatial Index file - .ssx (File, URL or relative file path).");
        props.put(ShapeLayer.spatialIndexProperty, interString);
        interString = i18n.get(ShapeLayer.class,
                ShapeLayer.spatialIndexProperty,
                ShapeLayer.spatialIndexProperty);
        props.put(ShapeLayer.spatialIndexProperty + LabelEditorProperty,
                interString);
        props.put(ShapeLayer.spatialIndexProperty + ScopedEditorProperty,
                "com.bbn.openmap.util.propertyEditor.FUPropertyEditor");

        interString = i18n.get(ShapeLayer.class,
                ShapeLayer.pointImageURLProperty,
                I18n.TOOLTIP,
                "Image file to use for map location of point data (optional).");
        props.put(ShapeLayer.pointImageURLProperty, interString);
        interString = i18n.get(ShapeLayer.class,
                ShapeLayer.pointImageURLProperty,
                ShapeLayer.pointImageURLProperty);
        props.put(ShapeLayer.pointImageURLProperty + LabelEditorProperty,
                interString);
        props.put(ShapeLayer.pointImageURLProperty + ScopedEditorProperty,
                "com.bbn.openmap.util.propertyEditor.FUPropertyEditor");

        if (drawingAttributes != null) {
            drawingAttributes.getPropertyInfo(props);
        } else {
            DrawingAttributes.DEFAULT.getPropertyInfo(props);
        }
        interString = i18n.get(SpatialIndexHandler.class,
                EnabledProperty,
                I18n.TOOLTIP,
                "Show file contents");
        props.put(EnabledProperty, interString);
        interString = i18n.get(SpatialIndexHandler.class,
                EnabledProperty,
                EnabledProperty);
        props.put(EnabledProperty + LabelEditorProperty, interString);
        props.put(EnabledProperty + ScopedEditorProperty,
                "com.bbn.openmap.util.propertyEditor.YesNoPropertyEditor");

        interString = i18n.get(SpatialIndexHandler.class,
                BufferedProperty,
                I18n.TOOLTIP,
                "Read and hold entire file contents (may be faster)");
        props.put(BufferedProperty, interString);
        interString = i18n.get(SpatialIndexHandler.class,
                BufferedProperty,
                BufferedProperty);
        props.put(BufferedProperty + LabelEditorProperty, interString);
        props.put(BufferedProperty + ScopedEditorProperty,
                "com.bbn.openmap.util.propertyEditor.YesNoPropertyEditor");

        return props;
    }

    /**
     * Create the OMGraphics out of the records that fall inside the bounding
     * box.
     * 
     * @param xmin double for the min horizontal limit of the bounding box.
     * @param ymin double for the min vertical limit of the bounding box.
     * @param xmax double for the max horizontal limit of the bounding box.
     * @param ymax double for the max vertical limit of the bounding box.
     */
    public OMGraphicList getGraphics(double xmin, double ymin, double xmax,
                                     double ymax) throws IOException,
            FormatException {
        return getGraphics(xmin,
                ymin,
                xmax,
                ymax,
                (OMGraphicList) null,
                (Projection) null);
    }

    /**
     * Given a bounding box, create OMGraphics from the ESRI records in the
     * shape file.
     * 
     * @param xmin double for the min horizontal limit of the bounding box.
     * @param ymin double for the min vertical limit of the bounding box.
     * @param xmax double for the max horizontal limit of the bounding box.
     * @param ymax double for the max vertical limit of the bounding box.
     * @param list OMGraphic list to add the new OMGraphics too. If null, a new
     *        OMGraphicList will be created.
     * @return OMGraphicList containing the new OMGraphics.
     */
    public OMGraphicList getGraphics(double xmin, double ymin, double xmax,
                                     double ymax, OMGraphicList list)
            throws IOException, FormatException {
        return getGraphics(xmin, ymin, xmax, ymax, list, (Projection) null);
    }

    /**
     * Given a bounding box, create OMGraphics from the ESRI records in the
     * shape file.
     * 
     * @param xmin double for the min horizontal limit of the bounding box.
     * @param ymin double for the min vertical limit of the bounding box.
     * @param xmax double for the max horizontal limit of the bounding box.
     * @param ymax double for the max vertical limit of the bounding box.
     * @param list OMGraphic list to add the new OMGraphics too. If null, a new
     *        OMGraphicList will be created.
     * @param proj the projection to use to generate the OMGraphics.
     * @return OMGraphicList containing the new OMGraphics.
     */
    public OMGraphicList getGraphics(double xmin, double ymin, double xmax,
                                     double ymax, OMGraphicList list,
                                     Projection proj) throws IOException,
            FormatException {
        if (list == null) {
            list = new OMGraphicList();
        }

        if (!buffered) {

            // Clean up if buffering turned off.
            if (bufferedList != null) {
                bufferedList = null;
            }

            spatialIndex.getOMGraphics(xmin,
                    ymin,
                    xmax,
                    ymax,
                    list,
                    drawingAttributes,
                    proj,
                    (Projection) null);

        } else {

            if (bufferedList == null) {
                bufferedList = getWholePlanet();
            }

            checkSpatialIndexEntries(xmin,
                    ymin,
                    xmax,
                    ymax,
                    list,
                    proj);

        }

        return list;
    }

    protected void checkSpatialIndexEntries(double xmin, double ymin,
                                            double xmax, double ymax,
                                            OMGraphicList retList,
                                            Projection proj) {
        // There should be the same number of objects in both iterators.
        Iterator entryIt = spatialIndex.entries.iterator();
        Iterator omgIt = bufferedList.iterator();
        while (entryIt.hasNext() && omgIt.hasNext()) {
            Entry entry = (Entry) entryIt.next();
            OMGraphic omg = (OMGraphic) omgIt.next();
            omg.generate(proj);
            if (entry.intersects(xmin, ymin, xmax, ymax)) {
                drawingAttributes.setTo(omg);
                retList.add(omg);
            }
        }
    }

    /**
     * Master list for buffering. Only used if buffering is enabled.
     */
    protected OMGraphicList bufferedList = null;

    /**
     * Get the graphics for the entire planet.
     */
    protected OMGraphicList getWholePlanet() throws IOException,
            FormatException {
        return spatialIndex.getOMGraphics(-180,
                -90,
                180,
                90,
                (OMGraphicList) null,
                drawingAttributes,
                (Projection) null,
                (Projection) null);
    }

    public void setPrettyName(String set) {
        prettyName = set;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public void setBuffered(boolean set) {
        buffered = set;
    }

    public boolean getBuffered() {
        return buffered;
    }

    public void setDrawingAttributes(DrawingAttributes set) {
        drawingAttributes = set;
    }

    public DrawingAttributes getDrawingAttributes() {
        return drawingAttributes;
    }

    public void setEnabled(boolean set) {
        enabled = set;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean close(boolean done) {
        if (spatialIndex != null) {
            return spatialIndex.close(done);
		}
        return false;
    }
}