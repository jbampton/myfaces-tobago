/*
  * Copyright (c) 2002 Atanion GmbH, Germany
  * All rights reserved. Created 28.04.2003 at 15:29:36.
  * $Id$
  */
package com.atanion.tobago.renderkit.html.scarborough.standard.tag;

import com.atanion.tobago.TobagoConstants;
import com.atanion.tobago.context.ResourceManagerUtil;
import com.atanion.tobago.component.ComponentUtil;
import com.atanion.tobago.component.UIPage;
import com.atanion.tobago.renderkit.CommandRendererBase;
import com.atanion.tobago.renderkit.DirectRenderer;
import com.atanion.tobago.renderkit.RenderUtil;
import com.atanion.tobago.renderkit.RendererBase;
import com.atanion.tobago.renderkit.LabelWithAccessKey;
import com.atanion.tobago.renderkit.HtmlUtils;
import com.atanion.tobago.webapp.TobagoResponseWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

public class MenubarRenderer extends RendererBase
    implements DirectRenderer {

// ///////////////////////////////////////////// constant

  private static final Log LOG = LogFactory.getLog(MenubarRenderer.class);

// ///////////////////////////////////////////// attribute

// ///////////////////////////////////////////// constructor

// ///////////////////////////////////////////// code

  public boolean getRendersChildren() {
    return true;
  }



  public void encodeChildren(FacesContext facesContext, UIComponent component)
      throws IOException {
  }

  public void encodeDirectEnd(FacesContext facesContext,
      UIComponent uiComponent) throws IOException {

    UIPanel menubar = (UIPanel) uiComponent ;
    final String clientId = menubar.getClientId(facesContext);

    ResponseWriter writer = facesContext.getResponseWriter();
    boolean suppressContainer = ComponentUtil.getBooleanAttribute(
        menubar, TobagoConstants.ATTR_SUPPPRESS_TOOLBAR_CONTAINER);

    if (! suppressContainer) {
      writer.startElement("div", menubar);
      writer.writeAttribute("id", clientId, null);
      writer.writeAttribute("class", null, TobagoConstants.ATTR_STYLE_CLASS);
      writer.writeAttribute("style", null, TobagoConstants.ATTR_STYLE);
      writer.endElement("div");
    }

    String setupFunction = "setupMenu"
        + clientId.replaceAll(":", "_").replaceAll("\\.", "_").replaceAll("-", "_");

    StringBuffer sb = new StringBuffer();
    sb.append("function ");
    sb.append(setupFunction);
    sb.append("(id, pageId) {\n");
    sb.append("  var menubar = document.getElementById(id);\n");
    sb.append("  if (menubar) {\n");
    sb.append("    var menu = createMenuRoot(id);\n");
    sb.append("    menubar.menu = menu;\n");

    addMenuEntrys(sb, "menu", facesContext, menubar, true);
    
    sb.append("    initMenuBar(id, pageId);\n");
    sb.append("  }\n");
    sb.append("}\n");

    final UIPage page = ComponentUtil.findPage(menubar);
    page.getScriptBlocks().add(sb.toString());
    page.getOnloadScripts().add(setupFunction + "('"
        + clientId + "', '" + page.getClientId(facesContext) + "');");
    page.getScriptFiles().add("tobago-menu.js", true);
    page.getStyleFiles().add("tobago-menu.css");

  }

  private void addMenuEntrys(StringBuffer sb, String var,
      FacesContext facesContext, UIComponent component, boolean warn) throws IOException {
    int i = 0;
    for (Iterator iter = component.getChildren().iterator(); iter.hasNext();) {
      UIComponent entry = (UIComponent) iter.next();
      if (entry instanceof UICommand) {
        addMenuEntry(sb, var, facesContext, (UICommand) entry);
      } else if ("menu".equals(entry.getAttributes().get(ATTR_MENU_TYPE))) {
        String name = var + "_" + i++;
        sb.append("    var " + name + " = " + createMenuEntry(facesContext, (UIPanel)entry) + ";\n");
        sb.append("    " + var + ".addMenuItem(" + name + ");\n");
        addMenuEntrys(sb, name, facesContext, entry, false);
      } else if (warn) {
        LOG.error("Illegal UIComponent class in menubar :"
            + entry.getClass().getName());
      }
    }


  }

  private String createMenuEntry(FacesContext facesContext, UIPanel uiPanel)
      throws IOException {

    final boolean disabled
        = ComponentUtil.getBooleanAttribute(uiPanel, ATTR_DISABLED);
    final boolean topMenu = uiPanel.getParent().getRendererType() != null;
    String spanClass
        = "tobago-menubar-item-span tobago-menubar-item-span-"
        + (disabled ? "disabled" : "enabled")
        + (topMenu ? " tobago-menubar-item-span-top" : "");

    final LabelWithAccessKey label = new LabelWithAccessKey(uiPanel);

    LOG.info("label.getText() = " + label.getText());



    ResponseWriter savedWriter = facesContext.getResponseWriter();
    StringWriter stringWriter = new StringWriter();
    TobagoResponseWriter writer
        = new TobagoResponseWriter(stringWriter, "text/html", "UTF8");
    facesContext.setResponseWriter(writer);

    writer.startElement("span", null);
    writer.writeAttribute("class", spanClass, null);
    writer.writeText(label.getText(), null);
    writer.endElement("span");

    if (! topMenu) {
      // uiPanel is a submenu
      LOG.info("adding subMenuMarker");
      addSubItemMarker(writer, facesContext);
    }
    else       LOG.info("NOT adding subMenuMarker");


    facesContext.setResponseWriter(savedWriter);


    return "new MenuItem('" + removeLFs(stringWriter.toString()) + "', null)";
  }

  private void addSubItemMarker(TobagoResponseWriter writer,
      FacesContext facesContext) throws IOException {
    writer.startElement("img", null);
    writer.writeAttribute("class", "tobago-menu-subitem-arrow", null);
    writer.writeAttribute("src",
        ResourceManagerUtil.getImage(facesContext, "MenuArrow.gif"), null);
    writer.endElement("img");
  }

  private String removeLFs(String s) {
    return s.replaceAll("\n", " ");
  }


  private void addMenuEntry(StringBuffer sb, String var, FacesContext facesContext,
      UICommand command) throws IOException {
    if ("menuItem".equals(command.getAttributes().get(ATTR_MENU_TYPE))
        || "menuCheck".equals(command.getAttributes().get(ATTR_MENU_TYPE)) ) {
      addMenuItem(sb, var, facesContext, command);
    }
    else if ("menuRadio".equals(command.getAttributes().get(ATTR_MENU_TYPE)) ) {
      addMenuRadio(sb, var, facesContext, command);
    }
  }

  private void addMenuRadio(StringBuffer sb, String var,
      FacesContext facesContext, UICommand command) {
    // todo: implement

  }

  private void addMenuItem(StringBuffer sb, String var, FacesContext facesContext,
      UICommand command) throws IOException {
    final boolean disabled
        = ComponentUtil.getBooleanAttribute(command, ATTR_DISABLED);
    String spanClass
        = "tobago-menubar-item-span tobago-menubar-item-span-"
        + (disabled ? "disabled" : "enabled");
    String onClick = createOnClick(facesContext, command);
    onClick = CommandRendererBase.appendConfirmationScript(onClick, command,
            facesContext);

    ResponseWriter savedWriter = facesContext.getResponseWriter();
    StringWriter stringWriter = new StringWriter();
    TobagoResponseWriter writer
        = new TobagoResponseWriter(stringWriter, "text/html", "UTF8");
    facesContext.setResponseWriter(writer);

    addImage(writer, facesContext, command);

    final LabelWithAccessKey label = new LabelWithAccessKey(command);
    writer.startElement("span", null);
    writer.writeAttribute("class", spanClass, null);
    writer.writeAttribute("accesskey", label.getAccessKey(), null);
    if (label.getText() != null) {
      RenderUtil.writeLabelWithAccessKey(writer, label);
    }
    writer.endElement("span");

    facesContext.setResponseWriter(savedWriter);

    sb.append("    ");
    sb.append(var);
    sb.append(".addMenuItem(new MenuItem('");
    sb.append(removeLFs(stringWriter.toString()));
    sb.append("', ");
    if (! disabled) {
      sb.append("\"");
      sb.append(onClick);
      sb.append("\"");
    }
    else {
      sb.append("null");
    }
    sb.append(", ");
    sb.append(disabled ? "true" : "false");
    sb.append("));\n");

  }

  private void addImage(TobagoResponseWriter writer, FacesContext facesContext,
      UICommand command) throws IOException {
    String image = null;
    if ("menuItem".equals(command.getAttributes().get(ATTR_MENU_TYPE))) {
      image = (String) command.getAttributes().get(ATTR_IMAGE);
      if (image != null) {
        image = ResourceManagerUtil.getImage(facesContext, image);
      }
    }
    else { // renderertype == "Menucheck"
      if (ComponentUtil.getBooleanAttribute(command, ATTR_CHECKED)) {
        image = ResourceManagerUtil.getImage(facesContext, "MenuCheckmark.gif");
      }
    }
    if (image != null) {
      writer.startElement("img", null);
      writer.writeAttribute("class", "tobago-menu-item-image", null);
      writer.writeAttribute("src", image, null);
      writer.endElement("img");
    }
  }

  public static String createOnClick(FacesContext facesContext,
      UIComponent component) {
    String type = (String) component.getAttributes().get(ATTR_TYPE);
    String command = (String) component.getAttributes().get(ATTR_ACTION_STRING);
    String clientId = component.getClientId(facesContext);
    String onclick;

    if (COMMAND_TYPE_NAVIGATE.equals(type)) {
      onclick = "navigateToUrl('"
          + HtmlUtils.generateUrl(facesContext, command) + "')";
    } else if (COMMAND_TYPE_RESET.equals(type)) {
      onclick = null;
    } else if (COMMAND_TYPE_SCRIPT.equals(type)) {
      onclick = command;
    } else { // default: Action.TYPE_SUBMIT
      onclick = "submitAction('" +
          ComponentUtil.findPage(component).getFormId(facesContext) +
          "','" + clientId + "')";
    }
    return onclick;
  }
// ///////////////////////////////////////////// bean getter + setter

}