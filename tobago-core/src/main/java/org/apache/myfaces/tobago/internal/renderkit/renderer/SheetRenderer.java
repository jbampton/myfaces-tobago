/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.myfaces.tobago.internal.renderkit.renderer;

import org.apache.myfaces.tobago.component.Attributes;
import org.apache.myfaces.tobago.component.Facets;
import org.apache.myfaces.tobago.component.LabelLayout;
import org.apache.myfaces.tobago.component.RendererTypes;
import org.apache.myfaces.tobago.component.UIColumnSelector;
import org.apache.myfaces.tobago.component.UILink;
import org.apache.myfaces.tobago.component.UIOut;
import org.apache.myfaces.tobago.component.UIPanel;
import org.apache.myfaces.tobago.component.UIReload;
import org.apache.myfaces.tobago.component.UISheet;
import org.apache.myfaces.tobago.context.Markup;
import org.apache.myfaces.tobago.context.TobagoResourceBundle;
import org.apache.myfaces.tobago.event.SheetAction;
import org.apache.myfaces.tobago.internal.component.AbstractUIColumn;
import org.apache.myfaces.tobago.internal.component.AbstractUIColumnBase;
import org.apache.myfaces.tobago.internal.component.AbstractUIData;
import org.apache.myfaces.tobago.internal.component.AbstractUIOut;
import org.apache.myfaces.tobago.internal.component.AbstractUIRow;
import org.apache.myfaces.tobago.internal.component.AbstractUISheet;
import org.apache.myfaces.tobago.internal.component.AbstractUIStyle;
import org.apache.myfaces.tobago.internal.layout.Cell;
import org.apache.myfaces.tobago.internal.layout.Grid;
import org.apache.myfaces.tobago.internal.layout.OriginCell;
import org.apache.myfaces.tobago.internal.renderkit.CommandMap;
import org.apache.myfaces.tobago.internal.util.HtmlRendererUtils;
import org.apache.myfaces.tobago.internal.util.JsonUtils;
import org.apache.myfaces.tobago.internal.util.RenderUtils;
import org.apache.myfaces.tobago.internal.util.StringUtils;
import org.apache.myfaces.tobago.layout.ShowPosition;
import org.apache.myfaces.tobago.layout.TextAlign;
import org.apache.myfaces.tobago.model.ExpandedState;
import org.apache.myfaces.tobago.model.Selectable;
import org.apache.myfaces.tobago.model.SheetState;
import org.apache.myfaces.tobago.model.TreePath;
import org.apache.myfaces.tobago.renderkit.RendererBase;
import org.apache.myfaces.tobago.renderkit.css.BootstrapClass;
import org.apache.myfaces.tobago.renderkit.css.CssItem;
import org.apache.myfaces.tobago.renderkit.css.CustomClass;
import org.apache.myfaces.tobago.renderkit.css.Icons;
import org.apache.myfaces.tobago.renderkit.css.TobagoClass;
import org.apache.myfaces.tobago.renderkit.html.DataAttributes;
import org.apache.myfaces.tobago.renderkit.html.HtmlAttributes;
import org.apache.myfaces.tobago.renderkit.html.HtmlElements;
import org.apache.myfaces.tobago.renderkit.html.HtmlInputTypes;
import org.apache.myfaces.tobago.util.ComponentUtils;
import org.apache.myfaces.tobago.webapp.TobagoResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.AjaxBehavior;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SheetRenderer extends RendererBase {

  private static final Logger LOG = LoggerFactory.getLogger(SheetRenderer.class);

  private static final String SUFFIX_WIDTHS = ComponentUtils.SUB_SEPARATOR + "widths";
  private static final String SUFFIX_COLUMN_RENDERED = ComponentUtils.SUB_SEPARATOR + "rendered";
  private static final String SUFFIX_SCROLL_POSITION = ComponentUtils.SUB_SEPARATOR + "scrollPosition";
  private static final String SUFFIX_SELECTED = ComponentUtils.SUB_SEPARATOR + "selected";

  @Override
  public void decode(final FacesContext facesContext, final UIComponent component) {

    final UISheet sheet = (UISheet) component;
    final List<AbstractUIColumnBase> columns = sheet.getAllColumns();
    final String clientId = sheet.getClientId(facesContext);

    String key = clientId + SUFFIX_WIDTHS;
    final Map requestParameterMap = facesContext.getExternalContext().getRequestParameterMap();
    final SheetState state = sheet.getState();
    if (requestParameterMap.containsKey(key)) {
      final String widths = (String) requestParameterMap.get(key);
      ensureColumnWidthsSize(state.getColumnWidths(), columns, JsonUtils.decodeIntegerArray(widths));
    }

    key = clientId + SUFFIX_SELECTED;
    if (requestParameterMap.containsKey(key)) {
      final String selected = (String) requestParameterMap.get(key);
      if (LOG.isDebugEnabled()) {
        LOG.debug("selected = " + selected);
      }
      List<Integer> selectedRows;
      try {
        selectedRows = StringUtils.parseIntegerList(selected);
      } catch (final NumberFormatException e) {
        LOG.warn(selected, e);
        selectedRows = Collections.emptyList();
      }

      ComponentUtils.setAttribute(sheet, Attributes.selectedListString, selectedRows);
    }

    final String value
        = facesContext.getExternalContext().getRequestParameterMap().get(clientId + SUFFIX_SCROLL_POSITION);
    if (value != null) {
      state.getScrollPosition().update(value);
    }
    RenderUtils.decodedStateOfTreeData(facesContext, sheet);

/* this will be done by the javax.faces.component.UIData.processDecodes() because these are facets.
    for (UIComponent facet : sheet.getFacets().values()) {
      facet.decode(facesContext);
    }
*/
  }

  @Override
  public void encodeBegin(final FacesContext facesContext, final UIComponent component) throws IOException {

    final UISheet sheet = (UISheet) component;
    final String sheetId = sheet.getClientId(facesContext);
    final Markup markup = sheet.getMarkup();
    final TobagoResponseWriter writer = getResponseWriter(facesContext);

    UIComponent header = sheet.getHeader();
    if (header == null) {
      header = ComponentUtils.createComponent(facesContext, UIPanel.COMPONENT_TYPE, null, "_header");
      final List<AbstractUIColumnBase> columns = sheet.getAllColumns();
      int i = 0;
      for (final AbstractUIColumnBase column : columns) {
        if (!(column instanceof AbstractUIRow)) {
          final AbstractUIOut out = (AbstractUIOut) ComponentUtils.createComponent(
              facesContext, UIOut.COMPONENT_TYPE, RendererTypes.Out, "_col" + i);
//        out.setValue(column.getLabel());
          ValueExpression valueExpression = column.getValueExpression(Attributes.label.getName());
          if (valueExpression != null) {
            out.setValueExpression(Attributes.value.getName(), valueExpression);
          } else {
            out.setValue(ComponentUtils.getAttribute(column, Attributes.label));
          }
          valueExpression = column.getValueExpression(Attributes.rendered.getName());
          if (valueExpression != null) {
            out.setValueExpression(Attributes.rendered.getName(), valueExpression);
          } else {
            out.setRendered(ComponentUtils.getBooleanAttribute(column, Attributes.rendered));
          }
          out.setLabelLayout(LabelLayout.skip);
          header.getChildren().add(out);
        }
        i++;
      }
      sheet.setHeader(header);
    }
    sheet.init(facesContext);

    // Outer sheet div
    writer.startElement(HtmlElements.DIV);
    writer.writeIdAttribute(sheetId);
    writer.writeAttribute(DataAttributes.MARKUP, JsonUtils.encode(markup), false);
    HtmlRendererUtils.writeDataAttributes(facesContext, writer, sheet);
    writer.writeClassAttribute(
        TobagoClass.SHEET,
        TobagoClass.SHEET.createMarkup(markup),
        TobagoClass.SHEET.createDefaultMarkups(sheet),
        sheet.getCustomClass());
    final UIComponent facetReload = ComponentUtils.getFacet(sheet, Facets.reload);
    if (facetReload != null && facetReload instanceof UIReload && facetReload.isRendered()) {
      final UIReload update = (UIReload) facetReload;
      writer.writeAttribute(DataAttributes.RELOAD, update.getFrequency());
    }
// todo    writer.writeCommandMapAttribute(JsonUtils.encode(RenderUtils.getBehaviorCommands(facesContext, sheet)));
    final CommandMap commands = RenderUtils.getBehaviorCommands(facesContext, sheet);
    writer.writeAttribute(DataAttributes.BEHAVIOR_COMMANDS, JsonUtils.encode(commands), false);
    writer.writeAttribute(DataAttributes.SELECTION_MODE, sheet.getSelectable().name(), false);
    writer.writeAttribute(DataAttributes.FIRST, Integer.toString(sheet.getFirst()), false);
    final StringBuilder builder = new StringBuilder();

    final boolean autoLayout = sheet.isAutoLayout();
    if (!autoLayout) {
      builder.append("{\"columns\":");
      JsonUtils.encode(sheet.getColumnLayout(), builder);
      builder.append("}");
      writer.writeAttribute(DataAttributes.LAYOUT, builder.toString(), true);
    }
  }

  @Override
  public void encodeChildren(final FacesContext facesContext, final UIComponent component) throws IOException {
    final UISheet sheet = (UISheet) component;
    for (UIComponent child : sheet.getChildren()) {
      if (child instanceof AbstractUIStyle) {
        child.encodeAll(facesContext);
      }
    }
  }

  @Override
  public void encodeEnd(final FacesContext facesContext, final UIComponent uiComponent) throws IOException {

    final UISheet sheet = (UISheet) uiComponent;
    final TobagoResponseWriter writer = getResponseWriter(facesContext);

    final String sheetId = sheet.getClientId(facesContext);
    final Selectable selectable = sheet.getSelectable();
    final Application application = facesContext.getApplication();
    final SheetState state = sheet.getSheetState(facesContext);
    final List<Integer> columnWidths = sheet.getState().getColumnWidths();
    final boolean definedColumnWidths = sheet.getState().isDefinedColumnWidths();
    final List<Integer> selectedRows = getSelectedRows(sheet, state);
    final List<AbstractUIColumnBase> columns = sheet.getAllColumns();
    final boolean autoLayout = sheet.isAutoLayout();

    ensureColumnWidthsSize(columnWidths, columns, Collections.emptyList());

    if (!autoLayout) {
      encodeHiddenInput(writer,
          JsonUtils.encode(definedColumnWidths ? columnWidths : Collections.emptyList()),
          sheetId + SUFFIX_WIDTHS);

      final ArrayList<String> encodedRendered = new ArrayList<>();
      for (AbstractUIColumnBase column : columns) {
        if (!(column instanceof AbstractUIRow)) {
          encodedRendered.add(Boolean.toString(column.isRendered()));
        }
      }

      encodeHiddenInput(writer,
          JsonUtils.encode(encodedRendered.toArray(new String[encodedRendered.size()])),
          sheetId + SUFFIX_COLUMN_RENDERED);
    }

    encodeHiddenInput(writer,
        sheet.getState().getScrollPosition().encode(),
        sheet.getClientId(facesContext) + SUFFIX_SCROLL_POSITION);

    if (selectable != Selectable.none) {
      encodeHiddenInput(writer,
          StringUtils.joinWithSurroundingSeparator(selectedRows),
          sheetId + SUFFIX_SELECTED);
    }

    StringBuilder expandedValue = null;
    if (sheet.isTreeModel()) {
      expandedValue = new StringBuilder(",");
    }

    encodeTableBody(
        facesContext, sheet, writer, sheetId, selectable, columnWidths, selectedRows, columns,
        autoLayout, expandedValue);

    if (sheet.isPagingVisible()) {
      writer.startElement(HtmlElements.FOOTER);
      writer.writeClassAttribute(TobagoClass.SHEET__FOOTER);

      // show row range
      final Markup showRowRange = markupForLeftCenterRight(sheet.getShowRowRange());
      if (showRowRange != Markup.NULL) {
        final UILink command
            = ensurePagingCommand(application, sheet, Facets.pagerRow.name(), SheetAction.toRow, false);
        final String pagerCommandId = command.getClientId(facesContext);

        writer.startElement(HtmlElements.UL);
        writer.writeClassAttribute(TobagoClass.SHEET__PAGING, TobagoClass.SHEET__PAGING.createMarkup(showRowRange),
            BootstrapClass.PAGINATION);
        writer.startElement(HtmlElements.LI);
        writer.writeClassAttribute(BootstrapClass.PAGE_ITEM);
        writer.writeAttribute(HtmlAttributes.TITLE,
            TobagoResourceBundle.getString(facesContext, "sheetPagingInfoRowPagingTip"), true);
        writer.startElement(HtmlElements.SPAN);
        writer.writeClassAttribute(TobagoClass.SHEET__PAGING_TEXT, BootstrapClass.PAGE_LINK);
        if (sheet.getRowCount() != 0) {
          final Locale locale = facesContext.getViewRoot().getLocale();
          final int first = sheet.getFirst() + 1;
          final int last1 = sheet.hasRowCount()
              ? sheet.getLastRowIndexOfCurrentPage()
              : -1;
          final boolean unknown = !sheet.hasRowCount();
          final String key; // plural
          if (unknown) {
            if (first == last1) {
              key = "sheetPagingInfoUndefinedSingleRow";
            } else {
              key = "sheetPagingInfoUndefinedRows";
            }
          } else {
            if (first == last1) {
              key = "sheetPagingInfoSingleRow";
            } else {
              key = "sheetPagingInfoRows";
            }
          }
          final String inputMarker = "{#}";
          final Object[] args = {inputMarker, last1 == -1 ? "?" : last1, unknown ? "" : sheet.getRowCount()};
          final MessageFormat detail = new MessageFormat(
              TobagoResourceBundle.getString(facesContext, key), locale);
          final String formatted = detail.format(args);
          final int pos = formatted.indexOf(inputMarker);
          if (pos >= 0) {
            writer.writeText(formatted.substring(0, pos));
            writer.startElement(HtmlElements.SPAN);
            writer.writeClassAttribute(TobagoClass.SHEET__PAGING_OUTPUT);
            writer.writeText(Integer.toString(first));
            writer.endElement(HtmlElements.SPAN);
            writer.startElement(HtmlElements.INPUT);
            writer.writeIdAttribute(pagerCommandId);
            writer.writeNameAttribute(pagerCommandId);
            writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.TEXT);
            writer.writeClassAttribute(TobagoClass.SHEET__PAGING_INPUT);
            writer.writeAttribute(HtmlAttributes.VALUE, first);
            if (!unknown) {
              writer.writeAttribute(HtmlAttributes.MAXLENGTH, Integer.toString(sheet.getRowCount()).length());
            }
            writer.endElement(HtmlElements.INPUT);
            writer.writeText(formatted.substring(pos + inputMarker.length()));
          } else {
            writer.writeText(formatted);
          }
        } else {
          writer.write(TobagoResourceBundle.getString(facesContext, "sheetPagingInfoEmptyRow"));
        }
        writer.endElement(HtmlElements.SPAN);
        writer.endElement(HtmlElements.LI);
        writer.endElement(HtmlElements.UL);
      }

      // show direct links
      final Markup showDirectLinks = markupForLeftCenterRight(sheet.getShowDirectLinks());
      if (showDirectLinks != Markup.NULL) {
        writer.startElement(HtmlElements.UL);
        writer.writeClassAttribute(TobagoClass.SHEET__PAGING, TobagoClass.SHEET__PAGING.createMarkup(showDirectLinks),
            BootstrapClass.PAGINATION);
        if (sheet.isShowDirectLinksArrows()) {
          final boolean disabled = sheet.isAtBeginning();
          encodeLink(facesContext, sheet, application, disabled, SheetAction.first, null, Icons.STEP_BACKWARD, null);
          encodeLink(facesContext, sheet, application, disabled, SheetAction.prev, null, Icons.BACKWARD, null);
        }
        encodeDirectPagingLinks(facesContext, application, sheet);
        if (sheet.isShowDirectLinksArrows()) {
          final boolean disabled = sheet.isAtEnd();
          encodeLink(facesContext, sheet, application, disabled, SheetAction.next, null, Icons.FORWARD, null);
          encodeLink(facesContext, sheet, application, disabled || !sheet.hasRowCount(), SheetAction.last, null,
              Icons.STEP_FORWARD, null);
        }
        writer.endElement(HtmlElements.UL);
      }

      // show page range
      final Markup showPageRange = markupForLeftCenterRight(sheet.getShowPageRange());
      if (showPageRange != Markup.NULL) {
        final UILink command
            = ensurePagingCommand(application, sheet, Facets.pagerPage.name(), SheetAction.toPage, false);
        final String pagerCommandId = command.getClientId(facesContext);

        writer.startElement(HtmlElements.UL);
        writer.writeClassAttribute(TobagoClass.SHEET__PAGING, TobagoClass.SHEET__PAGING.createMarkup(showPageRange),
            BootstrapClass.PAGINATION);
        if (sheet.isShowPageRangeArrows()) {
          final boolean disabled = sheet.isAtBeginning();
          encodeLink(facesContext, sheet, application, disabled, SheetAction.first, null, Icons.STEP_BACKWARD, null);
          encodeLink(facesContext, sheet, application, disabled, SheetAction.prev, null, Icons.BACKWARD, null);
        }
        writer.startElement(HtmlElements.LI);
        writer.writeClassAttribute(BootstrapClass.PAGE_ITEM);
        writer.startElement(HtmlElements.SPAN);
        writer.writeClassAttribute(TobagoClass.SHEET__PAGING_TEXT, BootstrapClass.PAGE_LINK);
        writer.writeAttribute(HtmlAttributes.TITLE,
            TobagoResourceBundle.getString(facesContext, "sheetPagingInfoPagePagingTip"), true);
        if (sheet.getRowCount() != 0) {
          final Locale locale = facesContext.getViewRoot().getLocale();
          final int first = sheet.getCurrentPage() + 1;
          final boolean unknown = !sheet.hasRowCount();
          final int pages = unknown ? -1 : sheet.getPages();
          final String key;
          if (unknown) {
            if (first == pages) {
              key = "sheetPagingInfoUndefinedSinglePage";
            } else {
              key = "sheetPagingInfoUndefinedPages";
            }
          } else {
            if (first == pages) {
              key = "sheetPagingInfoSinglePage";
            } else {
              key = "sheetPagingInfoPages";
            }
          }
          final String inputMarker = "{#}";
          final Object[] args = {inputMarker, pages == -1 ? "?" : pages};
          final MessageFormat detail = new MessageFormat(
              TobagoResourceBundle.getString(facesContext, key), locale);
          final String formatted = detail.format(args);
          final int pos = formatted.indexOf(inputMarker);
          if (pos >= 0) {
            writer.writeText(formatted.substring(0, pos));
            writer.startElement(HtmlElements.SPAN);
            writer.writeClassAttribute(TobagoClass.SHEET__PAGING_OUTPUT);
            writer.writeText(Integer.toString(first));
            writer.endElement(HtmlElements.SPAN);
            writer.startElement(HtmlElements.INPUT);
            writer.writeIdAttribute(pagerCommandId);
            writer.writeNameAttribute(pagerCommandId);
            writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.TEXT);
            writer.writeClassAttribute(TobagoClass.SHEET__PAGING_INPUT);
            writer.writeAttribute(HtmlAttributes.VALUE, first);
            if (!unknown) {
              writer.writeAttribute(HtmlAttributes.MAXLENGTH, Integer.toString(pages).length());
            }
            writer.endElement(HtmlElements.INPUT);
            writer.writeText(formatted.substring(pos + inputMarker.length()));
          } else {
            writer.writeText(formatted);
          }
        } else {
          writer.writeText(TobagoResourceBundle.getString(facesContext, "sheetPagingInfoEmptyPage"));
        }
        writer.endElement(HtmlElements.SPAN);
        writer.endElement(HtmlElements.LI);
        if (sheet.isShowPageRangeArrows()) {
          final boolean disabled = sheet.isAtEnd();
          encodeLink(facesContext, sheet, application, disabled, SheetAction.next, null, Icons.FORWARD, null);
          encodeLink(facesContext, sheet, application, disabled || !sheet.hasRowCount(), SheetAction.last, null,
              Icons.STEP_FORWARD, null);
        }
        writer.endElement(HtmlElements.UL);
      }

      writer.endElement(HtmlElements.FOOTER);
    }

    if (sheet.isTreeModel()) {
      writer.startElement(HtmlElements.INPUT);
      writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.HIDDEN);
      final String expandedId = sheetId + ComponentUtils.SUB_SEPARATOR + AbstractUIData.SUFFIX_EXPANDED;
      writer.writeNameAttribute(expandedId);
      writer.writeIdAttribute(expandedId);
      writer.writeClassAttribute(TobagoClass.SHEET__EXPANDED);
      writer.writeAttribute(HtmlAttributes.VALUE, expandedValue.toString(), false);
      writer.endElement(HtmlElements.INPUT);
    }

    writer.endElement(HtmlElements.DIV);
  }

  private void encodeTableBody(FacesContext facesContext, UISheet sheet, TobagoResponseWriter writer, String sheetId,
                               Selectable selectable, List<Integer> columnWidths, List<Integer> selectedRows,
                               List<AbstractUIColumnBase> columns, boolean autoLayout, StringBuilder expandedValue)
      throws IOException {

    final boolean showHeader = sheet.isShowHeader();
    Markup sheetMarkup = sheet.getMarkup() != null ? sheet.getMarkup() : Markup.NULL;
    final ExpandedState expandedState = sheet.isTreeModel() ? sheet.getExpandedState() : null;

    if (showHeader && !autoLayout) {
      // if no autoLayout, we render the header in a separate table.

      writer.startElement(HtmlElements.HEADER);
      writer.writeClassAttribute(TobagoClass.SHEET__HEADER);
      writer.startElement(HtmlElements.TABLE);
      writer.writeAttribute(HtmlAttributes.CELLSPACING, "0", false);
      writer.writeAttribute(HtmlAttributes.CELLPADDING, "0", false);
      writer.writeAttribute(HtmlAttributes.SUMMARY, "", false);
      writer.writeClassAttribute(
          BootstrapClass.TABLE,
          TobagoClass.SHEET__HEADER_TABLE,
          sheetMarkup.contains(Markup.INVERSE) ? BootstrapClass.TABLE_INVERSE : null,
          sheetMarkup.contains(Markup.DARK) ? BootstrapClass.TABLE_DARK : null,
          sheetMarkup.contains(Markup.BORDERED) ? BootstrapClass.TABLE_BORDERED : null,
          sheetMarkup.contains(Markup.SMALL) ? BootstrapClass.TABLE_SM : null,
          !autoLayout ? TobagoClass.TABLE_LAYOUT__FIXED : null);

      writeColgroup(writer, columnWidths, columns, true);

      writer.startElement(HtmlElements.TBODY);
      encodeHeaderRows(facesContext, sheet, writer, columns);
      writer.endElement(HtmlElements.TBODY);
      writer.endElement(HtmlElements.TABLE);
      writer.endElement(HtmlElements.HEADER);
    }

    writer.startElement(HtmlElements.DIV);
    writer.writeIdAttribute(sheetId + ComponentUtils.SUB_SEPARATOR + "data_div");
    writer.writeClassAttribute(TobagoClass.SHEET__BODY);

    writer.startElement(HtmlElements.TABLE);
    writer.writeAttribute(HtmlAttributes.CELLSPACING, "0", false);
    writer.writeAttribute(HtmlAttributes.CELLPADDING, "0", false);
    writer.writeAttribute(HtmlAttributes.SUMMARY, "", false);
    writer.writeClassAttribute(
        BootstrapClass.TABLE,
        TobagoClass.SHEET__BODY_TABLE,
        sheetMarkup.contains(Markup.INVERSE) ? BootstrapClass.TABLE_INVERSE : null,
        sheetMarkup.contains(Markup.DARK) ? BootstrapClass.TABLE_DARK : null,
        sheetMarkup.contains(Markup.STRIPED) ? BootstrapClass.TABLE_STRIPED : null,
        sheetMarkup.contains(Markup.BORDERED) ? BootstrapClass.TABLE_BORDERED : null,
        sheetMarkup.contains(Markup.HOVER) ? BootstrapClass.TABLE_HOVER : null,
        sheetMarkup.contains(Markup.SMALL) ? BootstrapClass.TABLE_SM : null,
        !autoLayout ? TobagoClass.TABLE_LAYOUT__FIXED : null);

    if (showHeader && autoLayout) {
      writer.startElement(HtmlElements.THEAD);
      encodeHeaderRows(facesContext, sheet, writer, columns);
      writer.endElement(HtmlElements.THEAD);
    }

    if (!autoLayout) {
      writeColgroup(writer, columnWidths, columns, false);
    }

    // Print the Content

    if (LOG.isDebugEnabled()) {
      LOG.debug("first = " + sheet.getFirst() + "   rows = " + sheet.getRows());
    }

    writer.startElement(HtmlElements.TBODY);

    final String var = sheet.getVar();

    boolean emptySheet = true;
    // rows = 0 means: show all
    final int last = sheet.isRowsUnlimited() ? Integer.MAX_VALUE : sheet.getFirst() + sheet.getRows();

    for (int rowIndex = sheet.getFirst(); rowIndex < last; rowIndex++) {
      sheet.setRowIndex(rowIndex);
      if (!sheet.isRowAvailable()) {
        break;
      }

      final Object rowRendered = sheet.getAttributes().get("rowRendered");
      if (rowRendered instanceof Boolean && !((Boolean) rowRendered)) {
        continue;
      }

      emptySheet = false;

      if (LOG.isDebugEnabled()) {
        LOG.debug("var       " + var);
        LOG.debug("list      " + sheet.getValue());
      }

      if (sheet.isTreeModel()) {
        final TreePath path = sheet.getPath();
        if (sheet.isFolder() && expandedState.isExpanded(path)) {
          expandedValue.append(rowIndex);
          expandedValue.append(",");
        }
      }

      writer.startElement(HtmlElements.TR);
      if (rowRendered instanceof Boolean) {
        // if rowRendered attribute is set we need the rowIndex on the client
        writer.writeAttribute(DataAttributes.ROW_INDEX, rowIndex);
      }

      final boolean selected = selectedRows.contains(rowIndex);
      final String[] rowMarkups = (String[]) sheet.getAttributes().get("rowMarkup");
      Markup rowMarkup = Markup.NULL;
      if (selected) {
        rowMarkup = rowMarkup.add(Markup.SELECTED);
      }
      if (rowMarkups != null) {
        rowMarkup = rowMarkup.add(Markup.valueOf(rowMarkups));
      }
      final String parentId = sheet.getRowParentClientId();
      if (parentId != null) {
        writer.writeAttribute(DataAttributes.TREE_PARENT, parentId, false);
      }

      // the row client id depends from the existence of an UIRow component! TBD: is this good?
      String rowClientId = sheet.getRowClientId();
      CustomClass rowClass = null;
      for (final UIColumn column : columns) {
        if (column.isRendered()) {
          if (column instanceof AbstractUIRow) {
            final AbstractUIRow row = (AbstractUIRow) column;
            writer.writeCommandMapAttribute(JsonUtils.encode(RenderUtils.getBehaviorCommands(facesContext, row)));
            rowClientId = row.getClientId(facesContext);
            rowClass = row.getCustomClass();
            // todo: Markup.CLICKABLE ???
          }
        }
      }
      writer.writeIdAttribute(rowClientId);
      writer.writeClassAttribute(
          TobagoClass.SHEET__ROW,
          TobagoClass.SHEET__ROW.createMarkup(rowMarkup),
          selected ? BootstrapClass.TABLE_INFO : null,
          rowClass,
          sheet.isRowVisible() ? null : BootstrapClass.D_NONE);

      for (final UIColumn column : columns) {
        if (column.isRendered()) {
          if (column instanceof AbstractUIColumn) {
            AbstractUIColumn normalColumn = (AbstractUIColumn) column;
            writer.startElement(HtmlElements.TD);
            Markup markup = normalColumn.getMarkup();
            if (markup == null) {
              markup = Markup.NULL;
            }
            markup = markup.add(getMarkupForAlign(normalColumn));
            writer.writeClassAttribute(
                TobagoClass.SHEET__CELL,
                TobagoClass.SHEET__CELL.createMarkup(markup),
                normalColumn.getCustomClass());

            if (normalColumn instanceof UIColumnSelector) {
              UIColumnSelector selector = (UIColumnSelector) normalColumn;
              writer.startElement(HtmlElements.INPUT);
              writer.writeNameAttribute(sheetId + "_data_row_selector_" + rowIndex);
              if (selectable.isSingle()) {
                writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.RADIO);
              } else {
                writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.CHECKBOX);
              }
              writer.writeAttribute(HtmlAttributes.CHECKED, selected);
              writer.writeAttribute(HtmlAttributes.DISABLED, selector.isDisabled());
              writer.writeClassAttribute(
                  BootstrapClass.FORM_CHECK_INLINE,
                  TobagoClass.SHEET__COLUMN_SELECTOR);
              writer.endElement(HtmlElements.INPUT);
            } else /*if (normalColumn instanceof AbstractUIColumnNode)*/ {
              normalColumn.encodeAll(facesContext);
            } /*else {
              final List<UIComponent> children = sheet.getRenderedChildrenOf(normalColumn);
              for (final UIComponent grandKid : children) {
                grandKid.encodeAll(facesContext);
              }
            }*/

            writer.endElement(HtmlElements.TD);
          }
        }
      }

      if (!autoLayout) {
        writer.startElement(HtmlElements.TD);
        writer.writeClassAttribute(TobagoClass.SHEET__CELL, TobagoClass.SHEET__CELL.createMarkup(Markup.FILLER));
//      writer.write("&nbsp;");
        writer.startElement(HtmlElements.DIV);
        writer.endElement(HtmlElements.DIV);
        writer.endElement(HtmlElements.TD);
      }

      writer.endElement(HtmlElements.TR);
    }

    sheet.setRowIndex(-1);

    if (emptySheet && showHeader) {
      writer.startElement(HtmlElements.TR);
      for (final UIColumn column : columns) {
        if (!(column instanceof AbstractUIRow)) {
          writer.startElement(HtmlElements.TD);
          writer.startElement(HtmlElements.DIV);
          writer.endElement(HtmlElements.DIV);
          writer.endElement(HtmlElements.TD);
        }
      }
      if (!autoLayout) {
        writer.startElement(HtmlElements.TD);
        writer.writeClassAttribute(TobagoClass.SHEET__CELL, TobagoClass.SHEET__CELL.createMarkup(Markup.FILLER));
//      writer.write("&nbsp;");
        writer.startElement(HtmlElements.DIV);
        writer.endElement(HtmlElements.DIV);
        writer.endElement(HtmlElements.TD);
      }
      writer.endElement(HtmlElements.TR);
    }

    writer.endElement(HtmlElements.TBODY);

    writer.endElement(HtmlElements.TABLE);
    writer.endElement(HtmlElements.DIV);

// END RENDER BODY CONTENT
  }

  private void encodeHiddenInput(TobagoResponseWriter writer, String value, String idWithSuffix) throws IOException {
    writer.startElement(HtmlElements.INPUT);
    writer.writeIdAttribute(idWithSuffix);
    writer.writeNameAttribute(idWithSuffix);
    writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.HIDDEN);
    writer.writeAttribute(HtmlAttributes.VALUE, value, false);
    writer.endElement(HtmlElements.INPUT);
  }

  private void ensureColumnWidthsSize(
      final List<Integer> columnWidths, final List<AbstractUIColumnBase> columns, final List<Integer> samples) {
    // we have to fill the non rendered positions with some values.
    // on client site, we don't know nothing about the non-rendered columns.
    int i = 0;
    int j = 0;
    for (AbstractUIColumnBase column : columns) {
      if (!(column instanceof AbstractUIRow)) {
        Integer newValue;
        if (j < samples.size()) {
          newValue = samples.get(j);
          j++;
        } else {
          newValue = null;
        }
        if (columnWidths.size() > i) {
          if (newValue != null) {
            columnWidths.set(i, newValue);
          }
        } else {
          columnWidths.add(newValue != null ? newValue : -1); // -1 means unknown or undefined
        }
        i++;
      }
    }
  }

  private Markup getMarkupForAlign(UIColumn column) {
    final String textAlign = ComponentUtils.getStringAttribute(column, Attributes.align);
    if (textAlign != null) {
      switch (TextAlign.valueOf(textAlign)) {
        case right:
          return Markup.RIGHT;
        case center:
          return Markup.CENTER;
        case justify:
          return Markup.JUSTIFY;
        default:
          // nothing to do
      }
    }
    return null;
  }

  private void encodeHeaderRows(
      final FacesContext facesContext, final UISheet sheet, final TobagoResponseWriter writer,
      final List<AbstractUIColumnBase> columns)
      throws IOException {

    final Selectable selectable = sheet.getSelectable();
    final Grid grid = sheet.getHeaderGrid();
    final boolean autoLayout = sheet.isAutoLayout();
    final boolean multiHeader = grid.getRowCount() > 1;
    int offset = 0;

    for (int i = 0; i < grid.getRowCount(); i++) {
      writer.startElement(HtmlElements.TR);
      final AbstractUIRow row = ComponentUtils.findChild(sheet, AbstractUIRow.class);
      if (row != null) {
        writer.writeClassAttribute(row.getCustomClass());
      }
      for (int j = 0; j < columns.size(); j++) {
        final AbstractUIColumnBase column = columns.get(j);
        if (!column.isRendered() || column instanceof AbstractUIRow) {
          offset++;
        } else {
          final Cell cell = grid.getCell(j - offset, i);
          if (cell instanceof OriginCell) {
            writer.startElement(HtmlElements.TH);
            if (cell.getColumnSpan() > 1) {
              writer.writeAttribute(HtmlAttributes.COLSPAN, cell.getColumnSpan());
            }
            if (cell.getRowSpan() > 1) {
              writer.writeAttribute(HtmlAttributes.ROWSPAN, cell.getRowSpan());
            }

            final UIComponent cellComponent = cell.getComponent();

            Markup align;
            final String alignString = ComponentUtils.getStringAttribute(column, Attributes.align);
            if (multiHeader && cell.getColumnSpan() > 1) {
              align = Markup.CENTER;
            } else if (alignString != null) {
              switch (TextAlign.valueOf(alignString)) {
                case right:
                  align = Markup.RIGHT;
                  break;
                case center:
                  align = Markup.CENTER;
                  break;
                case justify:
                  align = Markup.JUSTIFY;
                  break;
                default:
                  align = null;
              }
            } else {
              align = null;
            }
            writer.writeClassAttribute(
                TobagoClass.SHEET__HEADER_CELL,
                TobagoClass.SHEET__CELL.createMarkup(align),
                column.getCustomClass());
            writer.startElement(HtmlElements.SPAN);
            Icons sorterIcon = null;
            Markup markup = Markup.NULL;
            String tip = ComponentUtils.getStringAttribute(column, Attributes.tip);
            // sorter icons should only displayed when there is only 1 column and not input
            if (cell.getColumnSpan() == 1 && cellComponent instanceof UIOut) {
              final boolean sortable = ComponentUtils.getBooleanAttribute(column, Attributes.sortable);
              if (sortable) {
                UILink sortCommand = (UILink) ComponentUtils.getFacet(column, Facets.sorter);
                if (sortCommand == null) {
                  final String columnId = column.getClientId(facesContext);
                  final String sorterId = columnId.substring(columnId.lastIndexOf(":") + 1) + "_" + UISheet.SORTER_ID;
                  sortCommand = (UILink) ComponentUtils.createComponent(
                      facesContext, UILink.COMPONENT_TYPE, RendererTypes.Link, sorterId);
                  final AjaxBehavior reloadBehavior = createReloadBehavior(sheet);
                  sortCommand.addClientBehavior("click", reloadBehavior);
                  sortCommand.setRendererType(RendererTypes.SHEET_PAGE_COMMAND);
                  ComponentUtils.setAttribute(sortCommand, Attributes.sheetAction, SheetAction.sort);
                  ComponentUtils.setFacet(column, Facets.sorter, sortCommand);
                }
                writer.writeIdAttribute(sortCommand.getClientId(facesContext));
                writer.writeCommandMapAttribute(
                    JsonUtils.encode(RenderUtils.getBehaviorCommands(facesContext, sortCommand)));

                if (tip == null) {
                  tip = "";
                } else {
                  tip += " - ";
                }
                tip += TobagoResourceBundle.getString(facesContext, "sheetTipSorting");

                markup = markup.add(Markup.SORTABLE);

                final SheetState sheetState = sheet.getSheetState(facesContext);
                if (column.getId().equals(sheetState.getSortedColumnId())) {
                  final String sortTitle;
                  if (sheetState.isAscending()) {
                    sorterIcon = Icons.ANGLE_UP;
                    sortTitle = TobagoResourceBundle.getString(facesContext, "sheetAscending");
                    markup = markup.add(Markup.ASCENDING);
                  } else {
                    sorterIcon = Icons.ANGLE_DOWN;
                    sortTitle = TobagoResourceBundle.getString(facesContext, "sheetDescending");
                    markup = markup.add(Markup.DESCENDING);
                  }
                  if (sortTitle != null) {
                    tip += " - " + sortTitle;
                  }
                }
              }
            }

            writer.writeClassAttribute(TobagoClass.SHEET__HEADER, TobagoClass.SHEET__HEADER.createMarkup(markup));
            writer.writeAttribute(HtmlAttributes.TITLE, tip, true);

            if (column instanceof UIColumnSelector && selectable.isMulti()) {
              writer.startElement(HtmlElements.INPUT);
              writer.writeAttribute(HtmlAttributes.TYPE, HtmlInputTypes.CHECKBOX);

              writer.writeClassAttribute(TobagoClass.SHEET__COLUMN_SELECTOR);
              writer.writeAttribute(
                  HtmlAttributes.TITLE,
                  TobagoResourceBundle.getString(facesContext, "sheetMenuSelect"),
                  true);
              writer.endElement(HtmlElements.INPUT);
            } else {
              cellComponent.encodeAll(facesContext);
            }

            if (sorterIcon != null) {
              writer.startElement(HtmlElements.I);
              writer.writeClassAttribute(Icons.FA, sorterIcon);
              writer.endElement(HtmlElements.I);
            }

            writer.endElement(HtmlElements.SPAN);
            if (!autoLayout) {
              if (column.isResizable()) {
                encodeResizing(writer, sheet, j - offset + cell.getColumnSpan() - 1);
              }
            }

            writer.endElement(HtmlElements.TH);
          }
        }
      }
      if (!autoLayout) {
        // Add two filler columns. The second one get the size of the scrollBar via JavaScript.
        encodeHeaderFiller(writer, sheet);
        encodeHeaderFiller(writer, sheet);
      }

      writer.endElement(HtmlElements.TR);
    }
  }

  private void encodeHeaderFiller(final TobagoResponseWriter writer, final UISheet sheet) throws IOException {
    writer.startElement(HtmlElements.TH);
    writer.writeClassAttribute(
        TobagoClass.SHEET__HEADER_CELL,
        TobagoClass.SHEET__HEADER_CELL.createMarkup(Markup.FILLER));
    writer.startElement(HtmlElements.SPAN);
    writer.writeClassAttribute(TobagoClass.SHEET__HEADER);
    writer.endElement(HtmlElements.SPAN);
    writer.endElement(HtmlElements.TH);
  }

  private void writeColgroup(
      final TobagoResponseWriter writer, final List<Integer> columnWidths,
      final List<AbstractUIColumnBase> columns, boolean isHeader) throws IOException {
    writer.startElement(HtmlElements.COLGROUP);

    int i = 0;
    for (AbstractUIColumnBase column : columns) {
      if (!(column instanceof AbstractUIRow)) {
        if (column.isRendered()) {
          final Integer width = columnWidths.get(i);
          writeCol(writer, width >= 0 ? width : null);
        }
        i++;
      }
    }
    writeCol(writer, null); // extra entry for resizing...
    if (isHeader) {
      writeCol(writer, null); // extra entry for headerFiller
    }
    // TODO: the value should be added to the list
    writer.endElement(HtmlElements.COLGROUP);
  }

  private void writeCol(final TobagoResponseWriter writer, final Integer columnWidth) throws IOException {
    writer.startElement(HtmlElements.COL);
    writer.writeAttribute(HtmlAttributes.WIDTH, columnWidth);
    writer.endElement(HtmlElements.COL);
  }

  private Markup markupForLeftCenterRight(final ShowPosition position) {
    switch (position) {
      case left:
        return Markup.LEFT;
      case center:
        return Markup.CENTER;
      case right:
        return Markup.RIGHT;
      default:
        return Markup.NULL;
    }
  }

  @Override
  public boolean getRendersChildren() {
    return true;
  }

  private List<Integer> getSelectedRows(final UISheet data, final SheetState state) {
    List<Integer> selected = (List<Integer>) ComponentUtils.getAttribute(data, Attributes.selectedListString);
    if (selected == null && state != null) {
      selected = state.getSelectedRows();
    }
    if (selected == null) {
      selected = Collections.emptyList();
    }
    return selected;
  }

  private void encodeLink(
      final FacesContext facesContext, final UISheet data, final Application application,
      final boolean disabled, final SheetAction action, Integer target, Icons icon, CssItem liClass)
      throws IOException {

    final String facet = action == SheetAction.toPage || action == SheetAction.toRow
        ? action.name() + "-" + target
        : action.name();
    final UILink command = ensurePagingCommand(application, data, facet, action, disabled);
    if (target != null) {
      ComponentUtils.setAttribute(command, Attributes.pagingTarget, target);
    }

    final Locale locale = facesContext.getViewRoot().getLocale();
    final String message = TobagoResourceBundle.getString(facesContext, action.getBundleKey());
    final String tip = new MessageFormat(message, locale).format(new Integer[]{target}); // needed fot ToPage

    final TobagoResponseWriter writer = getResponseWriter(facesContext);
    writer.startElement(HtmlElements.LI);
    writer.writeClassAttribute(liClass, disabled ? BootstrapClass.DISABLED : null, BootstrapClass.PAGE_ITEM);
// todo button
    writer.startElement(HtmlElements.A);
    writer.writeClassAttribute(BootstrapClass.PAGE_LINK);
    writer.writeAttribute(HtmlAttributes.HREF, "#/", false);
    writer.writeIdAttribute(command.getClientId(facesContext));
    writer.writeAttribute(HtmlAttributes.TITLE, tip, true);
    if (!disabled) {
      writer.writeCommandMapAttribute(JsonUtils.encode(RenderUtils.getBehaviorCommands(facesContext, command)));
    }
    writer.writeAttribute(HtmlAttributes.DISABLED, disabled);
    if (icon != null) {
      writer.startElement(HtmlElements.I);
      writer.writeClassAttribute(Icons.FA, icon);
      writer.endElement(HtmlElements.I);
    } else {
      writer.writeText(String.valueOf(target));
    }
    writer.endElement(HtmlElements.A);
    writer.endElement(HtmlElements.LI);
  }

  // TODO sheet.getColumnLayout() may return the wrong number of column...
  // TODO
  // TODO

  private void encodeResizing(final TobagoResponseWriter writer, final AbstractUISheet sheet, final int columnIndex)
      throws IOException {
    writer.startElement(HtmlElements.SPAN);
    writer.writeClassAttribute(TobagoClass.SHEET__HEADER_RESIZE);
    writer.writeAttribute(DataAttributes.COLUMN_INDEX, Integer.toString(columnIndex), false);
    writer.write("&nbsp;&nbsp;"); // is needed for IE
    writer.endElement(HtmlElements.SPAN);
  }

  private void encodeDirectPagingLinks(
      final FacesContext facesContext, final Application application, final UISheet sheet)
      throws IOException {

    final UILink command
        = ensurePagingCommand(application, sheet, Facets.PAGER_PAGE_DIRECT, SheetAction.toPage, false);
    int linkCount = ComponentUtils.getIntAttribute(sheet, Attributes.directLinkCount);
    linkCount--;  // current page needs no link
    final ArrayList<Integer> prevs = new ArrayList<>(linkCount);
    int page = sheet.getCurrentPage() + 1;
    for (int i = 0; i < linkCount && page > 1; i++) {
      page--;
      if (page > 0) {
        prevs.add(0, page);
      }
    }

    final ArrayList<Integer> nexts = new ArrayList<>(linkCount);
    page = sheet.getCurrentPage() + 1;
    final int pages = sheet.hasRowCount() || sheet.isRowsUnlimited() ? sheet.getPages() : Integer.MAX_VALUE;
    for (int i = 0; i < linkCount && page < pages; i++) {
      page++;
      if (page > 1) {
        nexts.add(page);
      }
    }

    if (prevs.size() > (linkCount / 2)
        && nexts.size() > (linkCount - (linkCount / 2))) {
      while (prevs.size() > (linkCount / 2)) {
        prevs.remove(0);
      }
      while (nexts.size() > (linkCount - (linkCount / 2))) {
        nexts.remove(nexts.size() - 1);
      }
    } else if (prevs.size() <= (linkCount / 2)) {
      while (prevs.size() + nexts.size() > linkCount) {
        nexts.remove(nexts.size() - 1);
      }
    } else {
      while (prevs.size() + nexts.size() > linkCount) {
        prevs.remove(0);
      }
    }

    int skip = prevs.size() > 0 ? prevs.get(0) : 1;
    if (!sheet.isShowDirectLinksArrows() && skip > 1) {
      skip -= linkCount - (linkCount / 2);
      skip--;
      if (skip < 1) {
        skip = 1;
      }
      encodeLink(facesContext, sheet, application, false, SheetAction.toPage, skip, Icons.ELLIPSIS_H, null);
    }
    for (final Integer prev : prevs) {
      encodeLink(facesContext, sheet, application, false, SheetAction.toPage, prev, null, null);
    }
    encodeLink(facesContext, sheet, application, false, SheetAction.toPage,
        sheet.getCurrentPage() + 1, null, BootstrapClass.ACTIVE);

    for (final Integer next : nexts) {
      encodeLink(facesContext, sheet, application, false, SheetAction.toPage, next, null, null);
    }

    skip = nexts.size() > 0 ? nexts.get(nexts.size() - 1) : pages;
    if (!sheet.isShowDirectLinksArrows() && skip < pages) {
      skip += linkCount / 2;
      skip++;
      if (skip > pages) {
        skip = pages;
      }
      encodeLink(facesContext, sheet, application, false, SheetAction.toPage, skip, Icons.ELLIPSIS_H, null);
    }
  }

  private UILink ensurePagingCommand(
      final Application application, final UISheet sheet, final String facet, final SheetAction action,
      final boolean disabled) {

    final Map<String, UIComponent> facets = sheet.getFacets();
    UILink command = (UILink) facets.get(facet);
    if (command == null) {
      command = (UILink) application.createComponent(UILink.COMPONENT_TYPE);
      command.setRendererType(RendererTypes.SHEET_PAGE_COMMAND);
//      command.addActionListener(new SheetActionListener()); XXX to activate: remove RendererType
      command.setRendered(true);
      ComponentUtils.setAttribute(command, Attributes.sheetAction, action);
      command.setDisabled(disabled);
      facets.put(facet, command);

      // add AjaxBehavior
      final AjaxBehavior behavior = createReloadBehavior(sheet);
      command.addClientBehavior("click", behavior);
    }
    return command;
  }

  private AjaxBehavior createReloadBehavior(UISheet sheet) {
    AjaxBehavior reloadBehavior = findReloadBehavior(sheet);
    final ArrayList<String> renderIds = new ArrayList<>();
    if (!renderIds.contains(sheet.getId())) {
      renderIds.add(sheet.getId());
    }
    if (reloadBehavior != null) {
      renderIds.addAll(reloadBehavior.getRender());
    }
    final ArrayList<String> executeIds = new ArrayList<>();
    if (!executeIds.contains(sheet.getId())) {
      executeIds.add(sheet.getId());
    }
    if (reloadBehavior != null) {
      executeIds.addAll(reloadBehavior.getExecute());
    }
    final AjaxBehavior behavior = new AjaxBehavior();
    behavior.setExecute(executeIds);
    behavior.setRender(renderIds);
    return behavior;
  }

  private AjaxBehavior findReloadBehavior(ClientBehaviorHolder holder) {
    final List<ClientBehavior> reload = holder.getClientBehaviors().get("reload");
    if (reload != null && !reload.isEmpty() && reload.get(0) instanceof AjaxBehavior) {
      return (AjaxBehavior) reload.get(0);
    } else {
      return null;
    }
  }
}
