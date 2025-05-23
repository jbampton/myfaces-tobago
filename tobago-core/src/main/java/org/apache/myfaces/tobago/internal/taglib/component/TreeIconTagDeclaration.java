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

import org.apache.myfaces.tobago.apt.annotation.Tag;
import org.apache.myfaces.tobago.apt.annotation.TagAttribute;
import org.apache.myfaces.tobago.apt.annotation.UIComponentTag;
import org.apache.myfaces.tobago.apt.annotation.UIComponentTagAttribute;
import org.apache.myfaces.tobago.component.RendererTypes;
import org.apache.myfaces.tobago.internal.taglib.declaration.HasAlt;
import org.apache.myfaces.tobago.internal.taglib.declaration.HasIdBindingAndRendered;
import org.apache.myfaces.tobago.internal.taglib.declaration.HasTip;
import org.apache.myfaces.tobago.internal.taglib.declaration.IsDisabled;
import org.apache.myfaces.tobago.internal.taglib.declaration.IsVisual;

import jakarta.faces.component.UIGraphic;

/**
 * Renders an image beside a tree node.
 */
@Tag(name = "treeIcon")
@UIComponentTag(
    uiComponent = "org.apache.myfaces.tobago.component.UITreeIcon",
    uiComponentFacesClass = "jakarta.faces.component.UIGraphic",
    componentFamily = UIGraphic.COMPONENT_FAMILY,
    rendererType = RendererTypes.TREE_ICON,
    interfaces = {
        // As long as no behavior event names are defined, ClientBehaviorHolder must be implemented for Mojarra.
        "jakarta.faces.component.behavior.ClientBehaviorHolder"
    },
    allowedChildComponents = "NONE")
public interface TreeIconTagDeclaration extends HasIdBindingAndRendered, HasTip, IsDisabled, IsVisual, HasAlt {

  /**
   * Name of the icon.
   */
  @TagAttribute
  @UIComponentTagAttribute
  void setValue(String value);

  /**
   * Name of the folder icon, if not set "value" will be used.
   */
  @TagAttribute
  @UIComponentTagAttribute
  void setClosed(String closed);

  /**
   * Name of the open folder icon, if not set "closed" will be used.
   */
  @TagAttribute
  @UIComponentTagAttribute
  void setOpen(String open);
}
