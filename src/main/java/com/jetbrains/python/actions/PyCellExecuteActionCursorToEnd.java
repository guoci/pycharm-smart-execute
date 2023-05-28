package com.jetbrains.python.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.jetbrains.python.actions.PySmartExecuteSelectionAction.CursorMoveAfterExecute;
import org.jetbrains.annotations.NotNull;

public class PyCellExecuteActionCursorToEnd extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
    if (editor != null) {
      final String selectionText = PySmartExecuteSelectionAction.getSelectionText(editor);
      if (selectionText != null) {
        PyExecuteInConsole.executeCodeInConsole(e.getProject(), selectionText, null, true, true, false, null);
      }
      else {
        if (PyCellExecuteAction.inJupyterNotebookMode(e, editor))
          PyCellExecuteAction.cellExecute(e, editor);
        else
          PyCellExecuteAction.submitTopLevelCodeBlock(e, editor, CursorMoveAfterExecute.TO_THE_END_OF_CODE_BLOCK);
      }
    }
  }
}
