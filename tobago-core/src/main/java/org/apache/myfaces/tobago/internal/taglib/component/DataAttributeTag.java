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

package org.apache.myfaces.tobago.internal.taglib.component;

import org.apache.myfaces.tobago.apt.annotation.BodyContent;
import org.apache.myfaces.tobago.apt.annotation.Tag;
import org.apache.myfaces.tobago.apt.annotation.TagAttribute;
import org.apache.myfaces.tobago.apt.annotation.TagGeneration;
import org.apache.myfaces.tobago.util.ComponentUtils;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.webapp.UIComponentClassicTagBase;
import javax.faces.webapp.UIComponentELTag;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * PRELIMINARY - SUBJECT TO CHANGE
 *
 * Add an data attribute on the UIComponent
 * associated with the closest parent UIComponent custom action.
 * Data attributes will be passed through the renderers into the DOM of the user agent and
 * can be used by scripts.
 */
@Tag(name = "dataAttribute", bodyContent = BodyContent.EMPTY)
@TagGeneration(className = "org.apache.myfaces.tobago.internal.taglib.DataAttributeTag")
public abstract class DataAttributeTag extends TagSupport {

  private static final long serialVersionUID = 1L;

  /**
   * PRELIMINARY - SUBJECT TO CHANGE
   *
   * The name of the attribute in the parent component.
   */
  @TagAttribute(required = true, name = "name", type = "java.lang.String")
  public abstract void setName(ValueExpression name);

  public abstract String getNameValue();

  public abstract boolean isNameLiteral();

  public abstract Object getNameAsBindingOrExpression();

  public abstract String getNameExpression();

  /**
   * PRELIMINARY - SUBJECT TO CHANGE
   *
   * The value of the attribute in the parent component.
   */
  @TagAttribute(required = true, name = "value", type = "java.lang.String")
  public abstract void setValue(ValueExpression value);

  public abstract String getValueValue();

  public abstract boolean isValueLiteral();

  public abstract Object getValueAsBindingOrExpression();

  public abstract String getValueExpression();

  /**
   * @throws javax.servlet.jsp.JspException if a JSP error occurs
   */
  public int doStartTag() throws JspException {

    // Locate our parent UIComponentTag
    UIComponentClassicTagBase tag =
        UIComponentELTag.getParentUIComponentClassicTagBase(pageContext);
    if (tag == null) {
      // TODO Message resource i18n
      throw new JspException("Not nested in faces tag");
    }

    if (!tag.getCreated()) {
      return (SKIP_BODY);
    }

    final UIComponent component = tag.getComponentInstance();
    if (component == null) {
      // TODO Message resource i18n
      throw new JspException("Component Instance is null");
    }
    final Object attributeName;
    if (!isNameLiteral()) {
      attributeName = getNameAsBindingOrExpression();
      if (attributeName == null) {
        // TODO Message resource i18n
        throw new JspException("Can not get ValueBinding for attribute name " + getNameExpression());
      }
    } else {
      attributeName = getNameValue();
    }

    final Object attributeValue;
    if (!isValueLiteral()) {
      attributeValue = getValueAsBindingOrExpression();
      if (attributeValue == null) {
        // TODO Message resource i18n
        throw new JspException("Can not get ValueBinding for attribute value " + getValueExpression());
      }
    } else {
      attributeValue = getValueValue();
    }

    ComponentUtils.putDataAttribute(component, attributeName, attributeValue);

    return (SKIP_BODY);
  }
}
