/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.lang.completion.smartEnter.processors;

import com.intellij.codeInsight.editorActions.smartEnter.EnterProcessor;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

/**
 * User: Dmitry.Krasilschikov
 * Date: 05.08.2008
 */
public class GroovyPlainEnterProcessor implements EnterProcessor {
  public boolean doEnter(Editor editor, PsiElement psiElement, boolean isModified) {
    GrCodeBlock block = getControlStatementBlock(editor.getCaretModel().getOffset(), psiElement);

    if (block != null) {
      PsiElement firstElement = block.getFirstChild().getNextSibling();

      final int offset = firstElement != null ? firstElement.getTextRange().getStartOffset() - 1 : block.getTextRange().getEndOffset();
      editor.getCaretModel().moveToOffset(offset);
    }

    final EditorActionHandler endterHandler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_START_NEW_LINE);
    endterHandler.execute(editor, ((EditorEx)editor).getDataContext());
    return true;
  }

  @Nullable
  private static GrCodeBlock getControlStatementBlock(int caret, PsiElement element) {
    GrStatement body = null;

    if (element instanceof GrMethod) return ((GrMethod)element).getBlock();

    if (element instanceof GrMethodCall) {
      final GrClosableBlock[] arguments = ((GrMethodCall)element).getClosureArguments();
      if (arguments.length > 0) return arguments[0]; else return null;
    }

    if (element instanceof GrIfStatement) {
      body = ((GrIfStatement)element).getThenBranch();
      if (body != null && caret > body.getTextRange().getEndOffset()) {
        body = ((GrIfStatement)element).getElseBranch();
      }
    }
    else if (element instanceof GrWhileStatement) {
      body = ((GrWhileStatement)element).getBody();
    }
    else if (element instanceof GrForStatement) {
      body = ((GrForStatement)element).getBody();
    }

    if (body instanceof GrBlockStatement) {
      return ((GrBlockStatement)body).getBlock();
    }


    return null;
  }
}
