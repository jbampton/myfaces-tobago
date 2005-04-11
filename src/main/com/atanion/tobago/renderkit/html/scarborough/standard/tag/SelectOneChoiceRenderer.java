/*
 * Copyright (c) 2003 Atanion GmbH, Germany
 * All rights reserved. Created 07.02.2003 16:00:00.
 * $Id$
 */
package com.atanion.tobago.renderkit.html.scarborough.standard.tag;

import com.atanion.tobago.TobagoConstants;
import com.atanion.tobago.component.ComponentUtil;
import com.atanion.tobago.renderkit.HtmlUtils;
import com.atanion.tobago.renderkit.SelectOneRendererBase;
import com.atanion.tobago.renderkit.html.HtmlRendererUtil;
import com.atanion.tobago.util.LayoutUtil;
import com.atanion.tobago.webapp.TobagoResponseWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class SelectOneChoiceRenderer extends SelectOneRendererBase {

// ///////////////////////////////////////////// constant

  private static final Log LOG = LogFactory.getLog(SelectOneChoiceRenderer.class);

// ///////////////////////////////////////////// attribute

// ///////////////////////////////////////////// constructor

// ///////////////////////////////////////////// code


  public void encodeEndTobago(FacesContext facesContext,
      UIComponent uiComponent) throws IOException {

    UISelectOne component = (UISelectOne)uiComponent;
    List<SelectItem> items = ComponentUtil.getSelectItems(component);

    TobagoResponseWriter writer
        = (TobagoResponseWriter) facesContext.getResponseWriter();

    if (LOG.isDebugEnabled()) {
      LOG.debug("items.size() = '" + items.size() + "'");
    }

    boolean disabled = items.size() == 0 ||
        ComponentUtil.getBooleanAttribute(component, ATTR_DISABLED);

    UIComponent label = component.getFacet(TobagoConstants.FACET_LABEL);



    if (label != null) {
      writer.startElement("table", component);
      writer.writeAttribute("border", "0", null);
      writer.writeAttribute("cellspacing", "0", null);
      writer.writeAttribute("cellpadding", "0", null);
      writer.writeAttribute("summary", "", null);
      writer.writeAttribute("title", null, ATTR_TIP);
      writer.startElement("tr", null);
      writer.startElement("td", null);
      writer.writeText("", null);

      HtmlRendererUtil.encodeHtml(facesContext, label);

      writer.endElement("td");
      writer.startElement("td", null);
    }

    writer.startElement("select", component);
    writer.writeAttribute("name", component.getClientId(facesContext), null);
    writer.writeAttribute("id", component.getClientId(facesContext), null);
    writer.writeAttribute("disabled", disabled);
    writer.writeAttribute("style", null, TobagoConstants.ATTR_STYLE);
    writer.writeAttribute("class", null, TobagoConstants.ATTR_STYLE_CLASS);
    writer.writeAttribute("title", null, ATTR_TIP);
    String onchange = HtmlUtils.generateOnchange(component, facesContext);
    if (onchange != null) {
      writer.writeAttribute("onchange", onchange, null);
    }

    Object value = component.getValue();
    for (SelectItem item : items) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("item value = '" + item.getValue() + "'");
        LOG.debug("item class = '" + item.getClass().getName() + "'");
        LOG.debug("item label = '" + item.getLabel() + "'");
        LOG.debug("item descr = '" + item.getDescription() + "'");
      }
      writer.startElement("option", null);
      writer.writeAttribute("value", item.getValue(), null);
      if (item.getValue().equals(value)) {
        writer.writeAttribute("selected", "selected", null);
      }
      writer.writeText(item.getLabel(), null);
      writer.endElement("option");
    }
    writer.endElement("select");

    if (label != null) {
      writer.endElement("td");
      writer.endElement("tr");
      writer.endElement("table");
    }

  }

  public int getComponentExtraWidth(FacesContext facesContext, UIComponent component) {
    int space = 0;

    if (component.getFacet(TobagoConstants.FACET_LABEL) != null) {
      int labelWidht = LayoutUtil.getLabelWidth(component);
      space += labelWidht != 0 ? labelWidht : getLabelWidth(facesContext, component);
    }

    return space;
  }
// ///////////////////////////////////////////// bean getter + setter

}

