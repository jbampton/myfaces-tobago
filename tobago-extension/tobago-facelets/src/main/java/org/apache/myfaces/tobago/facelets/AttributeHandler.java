package org.apache.myfaces.tobago.facelets;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.sun.facelets.FaceletContext;
import com.sun.facelets.el.ELAdaptor;
import com.sun.facelets.el.LegacyMethodBinding;
import com.sun.facelets.el.TagMethodExpression;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagException;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.jsf.ComponentSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.tobago.component.Attributes;
import org.apache.myfaces.tobago.component.SupportsMarkup;
import org.apache.myfaces.tobago.component.SupportsRenderedPartially;
import org.apache.myfaces.tobago.el.ConstantMethodBinding;
import org.apache.myfaces.tobago.util.ComponentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.convert.Converter;

public final class AttributeHandler extends TagHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AttributeHandler.class);

  private final TagAttribute name;

  private final TagAttribute value;

  private final TagAttribute mode;

  public AttributeHandler(TagConfig config) {
    super(config);
    this.name = getRequiredAttribute(Attributes.NAME);
    this.value = getRequiredAttribute(Attributes.VALUE);
    this.mode = getAttribute(Attributes.MODE);
  }

  public void apply(FaceletContext faceletContext, UIComponent parent)
      throws FacesException, ELException {
    if (parent == null) {
      throw new TagException(tag, "Parent UIComponent was null");
    }

    if (ComponentSupport.isNew(parent)) {

      if (mode != null) {
        if ("isNotSet".equals(mode.getValue())) {
          boolean result = false;
          String expressionString = value.getValue();
          if (!value.isLiteral()) {
            while (isSimpleExpression(expressionString)) {
              if (isMethodOrValueExpression(expressionString)) {
                ValueExpression expression
                    = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                if (expression == null) {
                  result = true;
                  break;
                } else {
                  expressionString = expression.getExpressionString();
                }
              } else {
                result = false;
                break;
              }
            }
          } else {
            result = StringUtils.isEmpty(expressionString);
          }
          parent.getAttributes().put(name.getValue(), Boolean.valueOf(result));
        } else if ("isSet".equals(mode.getValue())) {
          boolean result = true;
          String expressionString = value.getValue();
          if (!value.isLiteral()) {
            while (isSimpleExpression(expressionString)) {
              if (isMethodOrValueExpression(expressionString)) {
                ValueExpression expression
                    = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                if (expression == null) {
                  result = false;
                  break;
                } else {
                  expressionString = expression.getExpressionString();
                }
              } else {
                result = true;
                break;
              }
            }
          } else {
            result = StringUtils.isNotEmpty(expressionString);
          }
          parent.getAttributes().put(name.getValue(), Boolean.valueOf(result));
        } else if ("action".equals(mode.getValue())) {
          String expressionString = value.getValue();
          while (isSimpleExpression(expressionString)) {
            if (isMethodOrValueExpression(expressionString)) {
              ValueExpression expression
                  = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
              if (expression == null) {
                // when the action hasn't been set while using a composition.
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Variable can't be resolved: value='" + expressionString + "'");
                }
                expressionString = null;
                break;
              } else {
                expressionString = expression.getExpressionString();
              }
            } else {
              break;
            }
          }
          if (expressionString != null) {
            ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
            MethodExpression action = new TagMethodExpression(value, expressionFactory.createMethodExpression(
                faceletContext, expressionString, String.class, ComponentUtils.ACTION_ARGS));
            // TODO jsf 1.2
            ((ActionSource) parent).setAction(new LegacyMethodBinding(action));
          }
        } else if ("actionListener".equals(mode.getValue())) {
          String expressionString = value.getValue();
          while (isSimpleExpression(expressionString)) {
            if (isMethodOrValueExpression(expressionString)) {
              ValueExpression expression
                  = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
              if (expression == null) {
                if (LOG.isDebugEnabled()) {
                  // when the action hasn't been set while using a composition.
                  LOG.debug("Variable can't be resolved: value='" + expressionString + "'");
                }
                expressionString = null;
                break;
              } else {
                expressionString = expression.getExpressionString();
              }
            } else {
              LOG.warn("Only expressions are supported mode=actionListener value='" + expressionString + "'");
              expressionString = null;
              break;
            }
          }
          if (expressionString != null) {
            ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
            MethodExpression actionListener = new TagMethodExpression(value, expressionFactory.createMethodExpression(
                faceletContext, expressionString, null, ComponentUtils.ACTION_LISTENER_ARGS));
            // TODO jsf 1.2
            ((ActionSource) parent).setActionListener(new LegacyMethodBinding(actionListener));
          }
        } else if ("actionFromValue".equals(mode.getValue())) {
          if (!value.isLiteral()) {
            String result = value.getValue(faceletContext);
            parent.getAttributes().put(name.getValue(), new ConstantMethodBinding(result));
          }
        } else if ("valueIfSet".equals(mode.getValue())) {
          String expressionString = value.getValue();
          while (isSimpleExpression(expressionString)) {
            if (isMethodOrValueExpression(expressionString)) {
              ValueExpression expression
                  = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
              if (expression == null) {
                if (LOG.isDebugEnabled()) {
                  // when the action hasn't been set while using a composition.
                  LOG.debug("Variable can't be resolved: value='" + expressionString + "'");
                }
                expressionString = null;
                break;
              } else {
                expressionString = expression.getExpressionString();
              }
            } else {
              LOG.warn("Only expressions are supported mode=valueIfSet value='" + expressionString + "'");
              expressionString = null;
              break;
            }
          }
          if (expressionString != null) {
            ValueExpression expression = value.getValueExpression(faceletContext, Object.class);
            ELAdaptor.setExpression(parent, name.getValue(faceletContext), expression);
          }
        } else {
          throw new FacesException("Type " + mode + " not suppored");
        }
      } else {

        String nameValue = name.getValue(faceletContext);
        if (Attributes.RENDERED.equals(nameValue)) {
          if (value.isLiteral()) {
            parent.setRendered(value.getBoolean(faceletContext));
          } else {
            ELAdaptor.setExpression(parent, nameValue, value.getValueExpression(faceletContext, Boolean.class));
          }
        } else if (Attributes.RENDERED_PARTIALLY.equals(nameValue)
            && parent instanceof SupportsRenderedPartially) {

          if (value.isLiteral()) {
            String[] components = ComponentUtils.splitList(value.getValue());
            ((SupportsRenderedPartially) parent).setRenderedPartially(components);
          } else {
            ELAdaptor.setExpression(parent, nameValue, value.getValueExpression(faceletContext, Object.class));
          }
        } else if (Attributes.STYLE_CLASS.equals(nameValue)) {
          // TODO expression
          ComponentUtils.setStyleClasses(parent, value.getValue());
        } else if (Attributes.MARKUP.equals(nameValue)) {
          if (parent instanceof SupportsMarkup) {
            if (value.isLiteral()) {
              ComponentUtils.setMarkup(parent, value.getValue());
            } else {
              ValueExpression expression = value.getValueExpression(faceletContext, Object.class);
              ELAdaptor.setExpression(parent, nameValue, expression);
            }
          } else {
            LOG.error("Component is not instanceof SupportsMarkup. Instance is: " + parent.getClass().getName());
          }
        } else if (parent instanceof EditableValueHolder && Attributes.VALIDATOR.equals(nameValue)) {
          MethodExpression methodExpression = getMethodExpression(faceletContext, null, ComponentUtils.VALIDATOR_ARGS);
          if (methodExpression != null) {
            // TODO jsf 1.2
            ((EditableValueHolder) parent).setValidator(new LegacyMethodBinding(methodExpression));
          }
        } else if (parent instanceof EditableValueHolder
            && Attributes.VALUE_CHANGE_LISTENER.equals(nameValue)) {
          MethodExpression methodExpression =
              getMethodExpression(faceletContext, null, ComponentUtils.VALUE_CHANGE_LISTENER_ARGS);
          if (methodExpression != null) {
            // TODO jsf 1.2
            ((EditableValueHolder) parent).setValueChangeListener(new LegacyMethodBinding(methodExpression));
          }
        } else if (parent instanceof ValueHolder && Attributes.CONVERTER.equals(nameValue)) {
          setConverter(faceletContext, parent, nameValue);
        } else if (parent instanceof ActionSource && Attributes.ACTION.equals(nameValue)) {
          MethodExpression action = getMethodExpression(faceletContext, String.class, ComponentUtils.ACTION_ARGS);
          if (action != null) {
            // TODO jsf 1.2
            ((ActionSource) parent).setAction(new LegacyMethodBinding(action));
          }
        } else if (parent instanceof ActionSource && Attributes.ACTION_LISTENER.equals(nameValue)) {
          MethodExpression action = getMethodExpression(faceletContext, null, ComponentUtils.ACTION_LISTENER_ARGS);
          if (action != null) {
            // TODO jsf 1.2
            ((ActionSource) parent).setActionListener(new LegacyMethodBinding(action));
          }
        } else if (!parent.getAttributes().containsKey(nameValue)) {
          if (value.isLiteral()) {
            parent.getAttributes().put(nameValue, value.getValue());
          } else {
            ELAdaptor.setExpression(parent, nameValue, value.getValueExpression(faceletContext, Object.class));
          }
        }
      }
    }
  }

  private boolean isMethodOrValueExpression(String string) {
    return (string.startsWith("${") || string.startsWith("#{")) && string.endsWith("}");
  }

  private boolean isSimpleExpression(String string) {
    return string.indexOf('.') < 0 && string.indexOf('[') < 0;
  }

  private String removeElParenthesis(String string) {
    return string.substring(2, string.length() - 1);
  }

  private ValueExpression getExpression(FaceletContext faceletContext) {
    String myValue = removeElParenthesis(value.getValue());
    return faceletContext.getVariableMapper().resolveVariable(myValue);
  }

  private MethodExpression getMethodExpression(FaceletContext faceletContext, Class returnType, Class[] args) {
    // in a composition may be we get the method expression string from the current variable mapper
    // the expression can be empty
    // in this case return nothing
    if (value.getValue().startsWith("${")) {
      ValueExpression expression = getExpression(faceletContext);
      if (expression != null) {
        ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
        return new TagMethodExpression(value, expressionFactory.createMethodExpression(faceletContext,
            expression.getExpressionString(), returnType, args));
      } else {
        return null;
      }
    } else {
      return value.getMethodExpression(faceletContext, returnType, args);
    }
  }

  private void setConverter(FaceletContext faceletContext, UIComponent parent, String nameValue) {
    // in a composition may be we get the converter expression string from the current variable mapper
    // the expression can be empty
    // in this case return nothing
    if (value.getValue().startsWith("${")) {
      ValueExpression expression = getExpression(faceletContext);
      if (expression != null) {
        setConverter(faceletContext, parent, nameValue, expression);
      }
    } else {
      setConverter(faceletContext, parent, nameValue, value.getValueExpression(faceletContext, Object.class));
    }
  }

  private void setConverter(
      FaceletContext faceletContext, UIComponent parent, String nameValue, ValueExpression expression) {
    if (expression.isLiteralText()) {
      Converter converter =
          faceletContext.getFacesContext().getApplication().createConverter(expression.getExpressionString());
      ((ValueHolder) parent).setConverter(converter);
    } else {
      ELAdaptor.setExpression(parent, nameValue, expression);
    }
  }
}
