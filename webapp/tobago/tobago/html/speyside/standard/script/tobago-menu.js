// menu.js



function getMenubarBorderWidth() {
  return 1;  
}

function getSubitemContainerBorderWidthSum() {
  return 2; // border * 2
}

function getItemHeight(menu) {
  if (menu && menu.level == 1) {
    if (menu.parent.popup) {
      return 18;
    }
    else if (menu.parent.menubar.className.match(/tobago-menubar-page-facet/)) {
      return 20;
    }
    else {
      return 15;
    }
  }
  else {
    return 15;
  }
}



function getPopupMenuWidth() {
  return 15;
}

function getMenuArrowWidth() {
  return 15;
}

function getToolbarButtonMenuTopOffset() {
  return -1;
}
