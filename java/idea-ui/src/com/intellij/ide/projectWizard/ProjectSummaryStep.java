/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.projectWizard;

import com.intellij.ide.util.projectWizard.NamePathComponent;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ide.wizard.StepAdapter;

import javax.swing.*;
import java.awt.*;

/**
 * @author Dmitry Avdeev
 *         Date: 20.09.13
 */
public class ProjectSummaryStep extends StepAdapter {

  private final WizardContext myWizardContext;
  private JPanel myRootPanel;
  private JPanel myNameAndLocationPanel;
  private final NamePathComponent myNamePathComponent;

  public ProjectSummaryStep(WizardContext context) {
    myWizardContext = context;
    myNamePathComponent = NamePathComponent.initNamePathComponent(myWizardContext);
    myNameAndLocationPanel.add(myNamePathComponent, BorderLayout.CENTER);
  }

  @Override
  public void _commit(boolean finishChosen) throws CommitStepException {
    myWizardContext.setProjectName(myNamePathComponent.getNameValue());
    myWizardContext.setProjectFileDirectory(myNamePathComponent.getPath());
  }

  @Override
  public JComponent getComponent() {
    return myRootPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNamePathComponent.getNameComponent();
  }
}
