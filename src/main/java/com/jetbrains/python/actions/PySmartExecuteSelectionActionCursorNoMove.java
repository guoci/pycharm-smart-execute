package com.jetbrains.python.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.jetbrains.python.actions.PySmartExecuteSelectionAction.CursorMoveAfterExecute;
import org.jetbrains.annotations.NotNull;

public class PySmartExecuteSelectionActionCursorNoMove extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
    if (editor != null) {
      final String selectionText = PySmartExecuteSelectionAction.getSelectionText(editor);
      if (selectionText != null) {
        PyExecuteInConsole.executeCodeInConsole(e.getProject(), selectionText, null, true, true, false, null);
      }
      else {
        PySmartExecuteSelectionAction.smartExecuteCode(e, editor, CursorMoveAfterExecute.NO_MOVE);
      }
    }
  }
}
