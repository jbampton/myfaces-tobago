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
import org.apache.myfaces.tobago.apt.annotation.UIComponentTag;
import org.apache.myfaces.tobago.component.RendererTypes;
import org.apache.myfaces.tobago.internal.taglib.declaration.HasFor;
import org.apache.myfaces.tobago.internal.taglib.declaration.HasIdBindingAndRendered;
import org.apache.myfaces.tobago.internal.taglib.declaration.HasTip;
import org.apache.myfaces.tobago.internal.taglib.declaration.IsAlwaysVisible;
import org.apache.myfaces.tobago.internal.taglib.declaration.IsReadonly;
import org.apache.myfaces.tobago.internal.taglib.declaration.IsVisual;

import javax.faces.component.UIOutput;

/**
 * Renders a panel to put paginators in it.
 */
@Tag(name = "paginatorPanel")
@UIComponentTag(
    uiComponent = "org.apache.myfaces.tobago.component.UIPaginatorPanel",
    uiComponentFacesClass = "javax.faces.component.UIOutput",
    componentFamily = UIOutput.COMPONENT_FAMILY,
    rendererType = RendererTypes.PAGINATOR_PANEL,
    allowedChildComponenents = {
        "org.apache.myfaces.tobago.PaginatorList",
        "org.apache.myfaces.tobago.PaginatorPage",
        "org.apache.myfaces.tobago.PaginatorRow"
    }
)

public interface PaginatorPanelTagDeclaration
    extends HasIdBindingAndRendered, HasTip, IsVisual, IsReadonly, IsAlwaysVisible, HasFor {
}