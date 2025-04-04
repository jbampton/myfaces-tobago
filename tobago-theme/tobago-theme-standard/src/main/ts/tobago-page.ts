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

import {Overlay} from "./tobago-overlay";
import {OverlayType} from "./tobago-overlay-type";
import {Key} from "./tobago-key";
import {PageStatic} from "./tobago-page-static";

export class Page extends HTMLElement {

  submitActive = false;

  /**
   * The Tobago root element
   */
  static page(element: HTMLElement): Page {
    const rootNode = element.getRootNode() as ShadowRoot | Document;
    const pages = rootNode.querySelectorAll("tobago-page");
    if (pages.length > 0) {
      if (pages.length >= 2) {
        console.warn("Found more than one tobago-page element!");
      }
      return pages.item(0) as Page;
    }
    console.warn("Found no tobago page!");
    return null;
  }

  constructor() {
    super();
  }

  connectedCallback(): void {

    this.registerAjaxListener();

    this.form.addEventListener("submit", this.beforeSubmit.bind(this));

    window.addEventListener("unload", this.beforeUnload.bind(this));

    window.addEventListener("keydown", (event: KeyboardEvent): boolean => {
      if (event.key === Key.ENTER) {
        const target = event.target as HTMLElement;
        if (target.tagName === "A" || target.tagName === "BUTTON") {
          return true;
        }
        if (target.tagName === "TEXTAREA") {
          if (!event.metaKey && !event.ctrlKey) {
            return true;
          }
        }
        const name = target.getAttribute("name");
        let id = name ? name : target.id;
        while (id != null) {
          const command = document.querySelector(`[data-tobago-default='${id}']`);
          if (command) {
            command.dispatchEvent(new MouseEvent("click"));
            return false;
          }
          id = PageStatic.getNamingContainerId(id);
        }
        document.querySelector("[data-tobago-default]");
      }
    });
  }

  beforeSubmit(event: Event, decoupled = false): void {
    this.submitActive = true;
    if (!decoupled) {
      this.body.insertAdjacentHTML("beforeend",
          Overlay.htmlText(this.id, OverlayType.submit, this.waitOverlayDelayFull));
    }
    console.debug(this.body.querySelector("tobago-overlay"));
  }

  /**
   * Wrapper function to call application generated onunload function
   */
  beforeUnload(): void {
    console.debug("unload");
    // todo: here me may check, if user will loose its edit state on the page
  }

  registerAjaxListener(): void {
    faces.ajax.addOnEvent(this.facesResponse.bind(this));
  }

  facesResponse(event: EventData): void {
    console.timeEnd("[tobago-faces] faces-ajax");
    console.time("[tobago-faces] faces-ajax");
    console.debug("[tobago-faces] Faces event status: '%s'", event.status);
    if (event.status === "success" && event.responseXML) {
      event.responseXML.querySelectorAll("update").forEach(this.facesResponseSuccess.bind(this));
    } else if (event.status === "complete" && event.responseXML) {
      event.responseXML.querySelectorAll("update").forEach(this.facesResponseComplete.bind(this));
    }
  }

  facesResponseSuccess(update: Element): void {
    const id = update.id;
    let rootNode = this.getRootNode() as ShadowRoot | Document;
    // XXX in case of "this" is tobago-page (e.g. ajax exception handling) rootNode is not set correctly???
    if (!rootNode.getElementById) {
      rootNode = document;
    }
    console.debug("[tobago-faces] Update after faces.ajax success: %s", id);
  }

  facesResponseComplete(update: Element): void {
    const id = update.id;
    if (!FacesParameter.isInternalFacesId(id)) {
      console.debug("[tobago-faces] Update after faces.ajax complete: #", id);
      const overlay = this.querySelector(`tobago-overlay[for='${id}']`);
      if (overlay) {
        overlay.remove();
      } else {
        console.warn("Didn't found overlay for id", id);
      }
    }
  }

  get form(): HTMLFormElement {
    return this.querySelector("form");
  }

  get body(): HTMLBodyElement {
    return this.closest("body");
  }

  get locale(): string {
    let locale = this.getAttribute("locale");
    if (!locale) {
      locale = document.documentElement.lang;
    }
    return locale;
  }

  get focusOnError(): boolean {
    return this.getAttribute("focus-on-error") === "true";
  }

  set focusOnError(focusOnError: boolean) {
    this.setAttribute("focus-on-error", String(focusOnError));
  }

  get waitOverlayDelayFull(): number {
    return parseInt(this.getAttribute("wait-overlay-delay-full")) || 1000;
  }

  set waitOverlayDelayFull(waitOverlayDelayFull: number) {
    this.setAttribute("wait-overlay-delay-full", String(waitOverlayDelayFull));
  }

  get waitOverlayDelayAjax(): number {
    return parseInt(this.getAttribute("wait-overlay-delay-ajax")) || 1000;
  }

  set waitOverlayDelayAjax(waitOverlayDelayAjax: number) {
    this.setAttribute("wait-overlay-delay-ajax", waitOverlayDelayAjax.toString());
  }

}

document.addEventListener("tobago.init", (event: Event): void => {
  if (window.customElements.get("tobago-page") == null) {
    window.customElements.define("tobago-page", Page);
  }
});

class FacesParameter {

  static VIEW_STATE = "jakarta.faces.ViewState";
  static CLIENT_WINDOW = "jakarta.faces.ClientWindow";
  static VIEW_ROOT = "jakarta.faces.ViewRoot";
  static VIEW_HEAD = "jakarta.faces.ViewHead";
  static VIEW_BODY = "jakarta.faces.ViewBody";
  static RESOURCE = "jakarta.faces.Resource";

  static isInternalFacesId(id: string): boolean {
    return id.indexOf("jakarta.faces.") !== -1;
  }

  static isFacesBody(id): boolean {
    switch (id) {
      case FacesParameter.VIEW_ROOT:
      case FacesParameter.VIEW_BODY:
        return true;
      default:
        return false;
    }
  }
}
