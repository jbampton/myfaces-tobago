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

import jakarta.faces.FacesException;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitResult;
import org.apache.myfaces.tobago.component.LabelLayout;
import org.apache.myfaces.tobago.component.SupportsLabelLayout;
import org.apache.myfaces.tobago.component.UIStyle;
import org.apache.myfaces.tobago.component.Visual;
import org.apache.myfaces.tobago.context.Markup;
import org.apache.myfaces.tobago.internal.component.AbstractUISegmentLayout;
import org.apache.myfaces.tobago.layout.SegmentJustify;
import org.apache.myfaces.tobago.renderkit.RendererBase;
import org.apache.myfaces.tobago.renderkit.css.BootstrapClass;
import org.apache.myfaces.tobago.renderkit.css.TobagoClass;
import org.apache.myfaces.tobago.renderkit.html.HtmlElements;
import org.apache.myfaces.tobago.util.ComponentUtils;
import org.apache.myfaces.tobago.webapp.TobagoResponseWriter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import java.io.IOException;

/**
 * Renders the 12 columns grid layout.
 */
public class SegmentLayoutRenderer<T extends AbstractUISegmentLayout> extends RendererBase<T> {


  @Override
  public boolean getRendersChildren() {
    return true;
  }

  @Override
  public void encodeBeginInternal(final FacesContext facesContext, final T component) throws IOException {

    final TobagoResponseWriter writer = getResponseWriter(facesContext);
    final Markup markup = component.getMarkup();
    final SegmentJustify segmentJustify = component.getJustify();

    writer.startElement(HtmlElements.TOBAGO_SEGMENT_LAYOUT);
    writer.writeIdAttribute(component.getClientId(facesContext));
//    writer.writeClassAttribute(BootstrapClass.FORM_HORIZONTAL, BootstrapClass.CONTAINER_FLUID);
    writer.writeClassAttribute(
        BootstrapClass.ROW,
        segmentJustify != null ? BootstrapClass.segmentJustify(segmentJustify) : null,
        markup != null && markup.contains(Markup.SPREAD) ? TobagoClass.SPREAD : null);
//    writer.writeClassAttribute(Classes.create(layout), BootstrapClass.FORM_GROUP);
  }

  @Override
  public void encodeChildrenInternal(final FacesContext facesContext, final T component) throws IOException {
    if (!component.isRendered()) {
      return;
    }
    BootstrapClass.Generator generator = new BootstrapClass.Generator(component);
    TobagoResponseWriter writer = getResponseWriter(facesContext);
    component.visitTree(VisitContext.createVisitContext(facesContext, null, ComponentUtils.SET_SKIP_UNRENDERED),
        (context, target) -> {
      if (!target.getClientId(facesContext).equals(component.getClientId(facesContext))
          && (target instanceof Visual && !((Visual) target).isPlain()
          || ComponentUtils.isStandardHtmlRendererType(target))) {
        try {
          encodeChild(facesContext, writer, generator, target);
        } catch (IOException ioException) {
          throw new FacesException(ioException);
        }
        return VisitResult.REJECT;
      } else if (target instanceof UIStyle) {
        try {
          target.encodeAll(facesContext);
        } catch (IOException ioException) {
          throw new FacesException(ioException);
        }
        return VisitResult.REJECT;
      } else {
        return VisitResult.ACCEPT;
      }
    });
  }

  private void encodeChild(
      final FacesContext facesContext, final TobagoResponseWriter writer,
      final BootstrapClass.Generator generator, final UIComponent child) throws IOException {

    if (child instanceof SupportsLabelLayout
        && LabelLayout.isSegment(((SupportsLabelLayout) child).getLabelLayout())) {

      final SupportsLabelLayout labeledChild = (SupportsLabelLayout) child;

      // left part
      labeledChild.setNextToRenderIsLabel(labeledChild.getLabelLayout() == LabelLayout.segmentLeft);
      encodeDiv(facesContext, writer, generator, child);
      generator.next();

      // right part
      labeledChild.setNextToRenderIsLabel(labeledChild.getLabelLayout() == LabelLayout.segmentRight);
      encodeDiv(facesContext, writer, generator, child);
      generator.next();

      LabelLayout.removeSegment(facesContext);
    } else { // normal case
      encodeDiv(facesContext, writer, generator, child);
      generator.next();
    }
  }

  private void encodeDiv(
      final FacesContext facesContext, final TobagoResponseWriter writer, final BootstrapClass.Generator generator,
      final UIComponent child)
      throws IOException {
    writer.startElement(HtmlElements.DIV);
    writer.writeClassAttribute(null, null, generator.generate(child));
    child.encodeAll(facesContext);
    writer.endElement(HtmlElements.DIV);
  }

  @Override
  public void encodeEndInternal(final FacesContext facesContext, final T component) throws IOException {
    final TobagoResponseWriter writer = getResponseWriter(facesContext);
    writer.endElement(HtmlElements.TOBAGO_SEGMENT_LAYOUT);
  }
}
