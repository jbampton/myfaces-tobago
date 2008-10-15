package org.apache.myfaces.tobago.layout.grid;

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

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.tobago.layout.LayoutComponent;
import org.apache.myfaces.tobago.layout.LayoutComponentImpl;
import org.apache.myfaces.tobago.layout.LayoutContainer;
import org.apache.myfaces.tobago.layout.LayoutContainerImpl;
import org.apache.myfaces.tobago.layout.LayoutContext;
import org.apache.myfaces.tobago.layout.math.AssertUtils;
import org.apache.myfaces.tobago.layout.math.EquationManager;

import java.util.Arrays;

public class GridLayoutManagerUnitTest extends TestCase {

  private static final Log LOG = LogFactory.getLog(GridLayoutManagerUnitTest.class);

  /**
   * <pre>
   * |                  800px               |
   * |    *    |    2*    |       500px     |
   * |         |          |    7*   |   3*  |
   * <p/>
   * +---------+----------+-----------------+  ----- ----- -----
   * |         |                            |
   * |         |                            |          *
   * |         |                            |
   * +---------+----------+--------+--------+  800px ----- -----
   * |         |          |        |        |                *
   * |         |          +--------+--------+        600px -----
   * |         |          |        |        |                *
   * +---------+----------+--------+--------+  ----- ----- -----
   * </pre>
   */
  public void test() {
    LayoutContainer container = new LayoutContainerImpl();
    LayoutComponent span = new LayoutComponentImpl();
    GridComponentConstraints bConstraint = GridComponentConstraints.getConstraints(span);
    bConstraint.setRowSpan(2);

    container.getComponents().add(new LayoutComponentImpl());
    container.getComponents().add(span);
    container.getComponents().add(new LayoutComponentImpl());
    container.getComponents().add(new LayoutComponentImpl());

    LayoutContainer subContainer = new LayoutContainerImpl();

    container.getComponents().add(subContainer);
    GridLayoutManager manager = new GridLayoutManager(container, "*;2*;500px", "*;600px");
    container.setLayoutManager(manager);

    GridLayoutManager subManager = new GridLayoutManager(subContainer, "7*;3*", "*;*");
    subContainer.setLayoutManager(subManager);
    subContainer.getComponents().add(new LayoutComponentImpl());
    subContainer.getComponents().add(new LayoutComponentImpl());
    subContainer.getComponents().add(new LayoutComponentImpl());
    subContainer.getComponents().add(new LayoutComponentImpl());

    LayoutContext layoutContext = new LayoutContext();

    EquationManager horizontal = layoutContext.getHorizontal();
    horizontal.setFixedLength(0, 800);
    horizontal.descend(0, 1);

    EquationManager vertial = layoutContext.getVertical();
    vertial.setFixedLength(0, 800);
    vertial.descend(0, 1);

    manager.layout(layoutContext);

    horizontal.ascend();
    vertial.ascend();

    double[] result = layoutContext.getHorizontal().solve();
    LOG.info("result: " + Arrays.toString(result));
    AssertUtils.assertEquals(new double[]{800, 100, 200, 500, 350, 150}, result, 0.000001);


    result = layoutContext.getVertical().solve();
    LOG.info("result: " + Arrays.toString(result));
    AssertUtils.assertEquals(new double[]{800, 200, 600, 300, 300}, result, 0.000001);
  }

  /**
   * <pre>
   * |               1000px              |
   * |     *     |     *     |     *     |
   * |           |   *   |   *   |   *   |
   * |   *   |   *   |   *   |           |
   * </pre>
   */
  public void testSpanOverlapsSpan() {
    LayoutContainer container = new LayoutContainerImpl();

    LayoutContainer span1 = new LayoutContainerImpl();
    GridComponentConstraints constraint1 = GridComponentConstraints.getConstraints(span1);
    constraint1.setRowSpan(2);

    LayoutContainer span2 = new LayoutContainerImpl();
    GridComponentConstraints constraint2 = GridComponentConstraints.getConstraints(span2);
    constraint2.setRowSpan(2);

    container.getComponents().add(span1);
    container.getComponents().add(new LayoutComponentImpl());
    container.getComponents().add(new LayoutComponentImpl());
    container.getComponents().add(span2);

    container.setLayoutManager(new GridLayoutManager(container, "*;*;*", "*;*"));
    span1.setLayoutManager(new GridLayoutManager(span1, "*;*;*", "*"));
    span2.setLayoutManager(new GridLayoutManager(span2, "*;*;*", "*"));

    LayoutContext layoutContext = new LayoutContext();

    EquationManager horizontal = layoutContext.getHorizontal();
    horizontal.setFixedLength(0, 900);
    horizontal.descend(0, 1);

    EquationManager vertial = layoutContext.getVertical();
    vertial.setFixedLength(0, 900);
    vertial.descend(0, 1);

    LOG.info(((GridLayoutManager)container.getLayoutManager()).getGrid());
    container.getLayoutManager().layout(layoutContext);

    horizontal.ascend();
    vertial.ascend();

    double[] result = layoutContext.getHorizontal().solve();
    LOG.info("result: " + Arrays.toString(result));
//fixme    AssertUtils.assertEquals(new double[]{900, 300, 300, 300, 200, 200, 200, 200, 200, 200}, result, 0.000001);


    result = layoutContext.getVertical().solve();
    LOG.info("result: " + Arrays.toString(result));
//fixme    AssertUtils.assertEquals(new double[]{900, 450, 450}, result, 0.000001);
  }
}
