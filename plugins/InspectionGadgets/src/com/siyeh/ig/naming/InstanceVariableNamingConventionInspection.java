/*
 * Copyright 2003-2007 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig.naming;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.fixes.RenameFix;
import org.jetbrains.annotations.NotNull;

public class InstanceVariableNamingConventionInspection
  extends ConventionInspection {

  private static final int DEFAULT_MIN_LENGTH = 5;
  private static final int DEFAULT_MAX_LENGTH = 32;

  @NotNull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "instance.variable.naming.convention.display.name");
  }

  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new RenameFix();
  }

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return true;
  }

  @NotNull
  public String buildErrorString(Object... infos) {
    final String fieldName = (String)infos[0];
    if (fieldName.length() < getMinLength()) {
      return InspectionGadgetsBundle.message(
        "instance.variable.name.convention.problem.descriptor.short");
    }
    else if (fieldName.length() > getMaxLength()) {
      return InspectionGadgetsBundle.message(
        "instance.variable.name.convention.problem.descriptor.long");
    }
    return InspectionGadgetsBundle.message(
      "instance.variable.name.convention.problem.descriptor.regex.mismatch",
      getRegex());
  }

  protected String getDefaultRegex() {
    return "m_[a-z][A-Za-z\\d]*";
  }

  protected int getDefaultMinLength() {
    return DEFAULT_MIN_LENGTH;
  }

  protected int getDefaultMaxLength() {
    return DEFAULT_MAX_LENGTH;
  }

  public BaseInspectionVisitor buildVisitor() {
    return new NamingConventionsVisitor();
  }

  private class NamingConventionsVisitor extends BaseInspectionVisitor {

    @Override
    public void visitField(@NotNull PsiField field) {
      super.visitField(field);
      if (field.hasModifierProperty(PsiModifier.STATIC)) {
        return;
      }
      final String name = field.getName();
      if (name == null) {
        return;
      }
      if (isValid(name)) {
        return;
      }
      registerFieldError(field, name);
    }
  }
}