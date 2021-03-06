/*
 * Copyright (c) 2018-2021 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.redhat.codeready.selenium.rolling;

import static org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces.Locators.WORKSPACE_ITEM_STOP_START_WORKSPACE_BUTTON;
import static org.testng.Assert.assertTrue;

import com.google.inject.Inject;
import com.redhat.codeready.selenium.core.executor.hotupdate.CodeReadyHotUpdateUtil;
import com.redhat.codeready.selenium.pageobject.dashboard.CodereadyCreateWorkspaceHelper;
import com.redhat.codeready.selenium.pageobject.dashboard.CodereadyDashboard;
import org.eclipse.che.selenium.core.client.TestWorkspaceServiceClient;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.pageobject.dashboard.NewWorkspace.Devfile;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Ihor Okhrimenko */
public class RollingUpdateStrategyWithWorkspacesStartStopTest {
  private static final String PROJECT_NAME = "vertx-health-checks";

  @Inject private CodereadyDashboard dashboard;
  @Inject private Workspaces workspaces;
  @Inject private CodeReadyHotUpdateUtil codeReadyHotUpdateUtil;
  @Inject private TestWorkspaceServiceClient workspaceServiceClient;
  @Inject private CodereadyCreateWorkspaceHelper codereadyCreateWorkspaceHelper;
  @Inject private DefaultTestUser defaultTestUser;

  private String startedWorkspaceName;
  private String stoppedWorkspaceName;

  @BeforeClass
  public void setUp() throws Exception {
    dashboard.open();
    startedWorkspaceName =
        codereadyCreateWorkspaceHelper.createAndStartWorkspace(Devfile.JAVA_MAVEN, PROJECT_NAME);

    dashboard.open();
    stoppedWorkspaceName =
        codereadyCreateWorkspaceHelper.createAndStartWorkspace(Devfile.JAVA_MAVEN, PROJECT_NAME);
  }

  @AfterClass
  public void tearDown() throws Exception {
    workspaceServiceClient.delete(startedWorkspaceName, defaultTestUser.getName());
    workspaceServiceClient.delete(stoppedWorkspaceName, defaultTestUser.getName());
  }

  @Test
  public void startStopWorkspaceFunctionsShouldBeAvailableDuringRollingUpdate() throws Exception {
    dashboard.open();
    dashboard.waitDashboardToolbarTitle();
    dashboard.selectWorkspacesItemOnDashboard();

    // check existing of expected workspaces and their statuses
    workspaces.waitPageLoading();
    workspaces.waitWorkspaceIsPresent(stoppedWorkspaceName);
    workspaces.waitWorkspaceIsPresent(startedWorkspaceName);

    workspaces.clickOnWorkspaceActionsButton(
        startedWorkspaceName, WORKSPACE_ITEM_STOP_START_WORKSPACE_BUTTON);
    workspaces.waitWorkspaceStatus(stoppedWorkspaceName, Workspaces.Status.RUNNING);
    workspaces.waitWorkspaceStatus(startedWorkspaceName, Workspaces.Status.STOPPED);

    codeReadyHotUpdateUtil.executeMasterPodUpdateCommand();
    // check that che is updated
    assertTrue(
        codeReadyHotUpdateUtil
            .getRolloutStatus()
            .contains("deployment \"codeready\" successfully rolled out"));
    WaitUtils.sleepQuietly(60);

    // execute stop-start commands for existing workspaces
    workspaces.clickOnWorkspaceStopStartButton(stoppedWorkspaceName);
    workspaces.waitWorkspaceStatus(stoppedWorkspaceName, Workspaces.Status.STOPPED);
    workspaces.clickOnWorkspaceStopStartButton(startedWorkspaceName);
    workspaces.waitWorkspaceStatus(startedWorkspaceName, Workspaces.Status.RUNNING);
  }
}
