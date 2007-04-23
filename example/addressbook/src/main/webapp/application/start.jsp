<%--
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
--%>
<%@ taglib uri="http://myfaces.apache.org/tobago/component" prefix="tc" %>
<%@ taglib uri="http://myfaces.apache.org/tobago/extension" prefix="tx" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>

<f:view>
  <tc:loadBundle basename="addressbook" var="bundle" />

  <tc:page label="#{bundle.editorTitle}" state="#{layout}" width="#{layout.width}" height="#{layout.height}">
    <f:facet name="action">
      <tc:link action="list" />
    </f:facet>

    <tc:panel>
      <f:facet name="layout">
        <tc:gridLayout margin="10px" rows="*;fixed;*" columns="*;400px;*"/>
      </f:facet>

      <tc:cell spanX="3"/>

      <tc:cell/>
      <tc:panel>
        <f:facet name="layout">
          <tc:gridLayout rows="150px" columns="150px;250px"/>
        </f:facet>

        <tc:image value="image/org/tango-project/tango-icon-theme/address-book-splash-screen.png" width="150" height="150"/>
        <tc:panel>
          <f:facet name="layout">
            <tc:gridLayout rows="fixed;fixed;fixed;*"/>
          </f:facet>

          <tc:out value="Addressbook Demo"/>
          <tc:cell/>
          <tc:out value="Initializing ..."/>
          <tc:cell/>

        </tc:panel>

      </tc:panel>
      <tc:cell/>

      <tc:cell spanX="3"/>

    </tc:panel>
  </tc:page>
</f:view>
