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

package org.apache.myfaces.tobago.context;

import org.apache.myfaces.tobago.config.TobagoConfig;
import org.apache.myfaces.tobago.internal.context.ClientPropertiesKey;
import org.apache.myfaces.tobago.internal.util.Deprecation;
import org.apache.myfaces.tobago.layout.Measure;
import org.apache.myfaces.tobago.util.VariableResolverUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Locale;

/**
 * The ClientProperties contains data, which are individual for each user.
 * It is stored in the session by default, but the application can override this in the faces-config.xml.
 * <p/>
 * The managed bean name which is Tobago using for the instance is {@link #MANAGED_BEAN_NAME}.
 * <p/>
 * Please use {@link org.apache.myfaces.tobago.util.VariableResolverUtils#resolveClientProperties(FacesContext)}
 * to access to the users client properties.
 */

public class ClientProperties implements Serializable {

  /**
   * The name of the managed bean
   */
  public static final String MANAGED_BEAN_NAME = "tobagoClientProperties";

  private static final long serialVersionUID = 2L;

  private static final Logger LOG = LoggerFactory.getLogger(ClientProperties.class);

  private String contentType = "html";
  private Theme theme;
  private UserAgent userAgent = UserAgent.DEFAULT;
  private boolean debugMode;

  private Measure verticalScrollbarWeight;
  private Measure horizontalScrollbarWeight;

  /** 
   * managed bean constructor
   */
  public ClientProperties() {
    this(FacesContext.getCurrentInstance());
  }

  private ClientProperties(FacesContext facesContext) {

    ExternalContext externalContext = facesContext.getExternalContext();

    // user agent
    String requestUserAgent = (String) externalContext.getRequestHeaderMap().get("User-Agent");
    this.userAgent = UserAgent.getInstance(requestUserAgent);
    if (LOG.isDebugEnabled()) {
      LOG.debug("userAgent='" + this.userAgent + "' from header " + "'User-Agent: " + requestUserAgent + "'");
    }

    // theme
    String requestTheme = (String) externalContext.getRequestParameterMap().get("tobago.theme");
    TobagoConfig config = TobagoConfig.getInstance(facesContext);
    // TODO log error if tobago config is not initialized
    this.theme = config.getTheme(requestTheme);
    if (LOG.isDebugEnabled()) {
      LOG.debug("theme='" + theme.getName() + "' from requestParameter " + "tobago.theme='" + requestTheme + "'");
    }

    reset();
  }

  public static ClientProperties getInstance(FacesContext facesContext) {
    return (ClientProperties) VariableResolverUtils.resolveVariable(facesContext, MANAGED_BEAN_NAME);
  }

  private void reset() {
    ClientPropertiesKey.reset(FacesContext.getCurrentInstance());
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
    reset();
  }

  public Theme getTheme() {
    return theme;
  }
  
  public void setTheme(Theme theme) {
    this.theme = theme;
    reset();
  }

  public UserAgent getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(UserAgent userAgent) {
    this.userAgent = userAgent;
    reset();
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }

  /**
   * @deprecated since 2.0.0, please use {@link UIViewRoot#getLocale()}
   */
  public Locale getLocale() {
    Deprecation.LOG.warn("Please get locale via UIViewRoot.");
    return FacesContext.getCurrentInstance().getViewRoot().getLocale();
  }

  /**
   * @deprecated since 2.0.0
   * This setter should not be called from the application directly,
   * but via {@link UIViewRoot#setLocale(Locale locale)} 
   */
  public void setLocale(Locale locale) {
    Deprecation.LOG.warn("Please set locale via UIViewRoot.");
    FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);
  }

  public Measure getVerticalScrollbarWeight() {
    return verticalScrollbarWeight;
  }

  public void setVerticalScrollbarWeight(Measure verticalScrollbarWeight) {
    this.verticalScrollbarWeight = verticalScrollbarWeight;
  }

  public Measure getHorizontalScrollbarWeight() {
    return horizontalScrollbarWeight;
  }

  public void setHorizontalScrollbarWeight(Measure horizontalScrollbarWeight) {
    this.horizontalScrollbarWeight = horizontalScrollbarWeight;
  }

  public void updateUserAgent(FacesContext facesContext) {
    ExternalContext externalContext = facesContext.getExternalContext();
    String requestUserAgent = (String) externalContext.getRequestHeaderMap().get("User-Agent");
    final UserAgent newUserAgent = UserAgent.getInstance(requestUserAgent);
    if (newUserAgent != userAgent) {
      userAgent = newUserAgent;
      reset();
    }
  }
}
