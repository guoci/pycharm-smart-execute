package com.jetbrains.python.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.actions.PySmartExecuteSelectionAction.CursorMoveAfterExecute;
import com.jetbrains.python.psi.PyElementType;
import com.jetbrains.python.psi.PyElsePart;
import com.jetbrains.python.psi.PyExceptPart;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFinallyPart;
import com.jetbrains.python.psi.PyIfPart;
import com.jetbrains.python.psi.PyStatementList;
import com.jetbrains.python.psi.PyStatementPart;
import com.jetbrains.python.psi.PyWhilePart;
import com.jetbrains.python.psi.impl.PyIfPartElifImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PyCellExecuteAction extends AnAction {

  static boolean inJupyterNotebookMode(final AnActionEvent e, final Editor editor) {
    // by checking for `JupyterTokenType.CODE_MARKER`
    // but Pycharm's Jupyter mode is a closed sourced component, to workaround, check if it is not (`PyElementType` or `PsiWhiteSpace`)
    final Document document = editor.getDocument();
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(e.getProject());
    psiDocumentManager.commitDocument(document);
    final PsiFile psiFile = psiDocumentManager.getPsiFile(document);

    final LogicalPosition logicalPos = editor.getCaretModel().getLogicalPosition();
    final int line = logicalPos.line;
    int start = 6666, end = 6666;
    for (int i = 0; ; --i) {
      final int cline = line + i;
      int offset;
      try {
        offset = DocumentUtil.getFirstNonSpaceCharOffset(document, cline);
        start = offset;
      } catch (IndexOutOfBoundsException ex) {
        break;
      }
      final PsiElement psiElement = psiFile.findElementAt(offset);
      // System.out.println("i = " + i);
      // System.out.println("cline = " + cline);
      // System.out.println("psiElement = " + psiElement);
      // System.out.println("psiElement.getNode() = " + psiElement.getNode());
      if (psiElement == null)
        break;
      final var elementType = psiElement.getNode().getElementType();
      // System.out.println("elementType = " + elementType);
      if (!((elementType instanceof PyElementType) || (psiElement.getNode() instanceof PsiWhiteSpace)))
        return true;
    }
    return false;
  }
  static void moveCaretVertical(final Editor editor, final int numLines) {
    final LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
    editor.getCaretModel().moveToOffset(
        editor.logicalPositionToOffset(
            new LogicalPosition(pos.line + numLines, pos.column)));
  }

  static void submitTopLevelCodeBlock(final AnActionEvent e, final Editor editor, CursorMoveAfterExecute cursorMoveAfterExecute) {
    final Document document = editor.getDocument();
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(e.getProject());
    psiDocumentManager.commitDocument(document);
    final PsiFile psiFile = psiDocumentManager.getPsiFile(document);
    final int currentLogicalPos = editor.getCaretModel().getOffset();
    for (; ; ) {
      final LogicalPosition logicalPos = editor.getCaretModel().getLogicalPosition();
      final int firstNonSpaceCharOffset = DocumentUtil.getFirstNonSpaceCharOffset(document, logicalPos.line);
      // System.out.println("firstNonSpaceCharOffset = " + firstNonSpaceCharOffset);
      final int lineStartOffset = DocumentUtil.getLineStartOffset(firstNonSpaceCharOffset,
          document);
//       System.out.println("lineStartOffset = " + lineStartOffset);
      if (lineStartOffset == firstNonSpaceCharOffset) {
        final PsiElement pe = psiFile.findElementAt(lineStartOffset);
//        System.out.println("pe.getTextOffset() = " + pe.getTextOffset());
        if (pe.getTextOffset() == lineStartOffset) {
          PySmartExecuteSelectionAction.smartExecuteCode(e, editor, cursorMoveAfterExecute);
          if(cursorMoveAfterExecute==CursorMoveAfterExecute.NO_MOVE)
            editor.getCaretModel().moveToOffset(currentLogicalPos);
          return;
        }
      }
      try {
        moveCaretVertical(editor, -1);
      }catch(Exception ex){
//        ex.printStackTrace();
        throw ex;
      }
    }
  }
  static void cellExecute(final AnActionEvent e, final Editor editor) {
    final Document document = editor.getDocument();
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(e.getProject());
    psiDocumentManager.commitDocument(document);
    final PsiFile psiFile = psiDocumentManager.getPsiFile(document);

    final LogicalPosition logicalPos = editor.getCaretModel().getLogicalPosition();
    final int line = logicalPos.line;

    int start = 6666, end = 6666;
    for (int i = 0; ; --i) {
      final int cline = line + i;
      int offset;
      try {
        offset = DocumentUtil.getFirstNonSpaceCharOffset(document, cline);
        start = offset;
      } catch (IndexOutOfBoundsException ex) {
        break;
      }
      final PsiElement psiElement = psiFile.findElementAt(offset);
      // System.out.println("i = " + i);
      // System.out.println("cline = " + cline);
      // System.out.println("psiElement = " + psiElement);
      // System.out.println("psiElement.getNode() = " + psiElement.getNode());
      if (psiElement == null)
        break;
      final var elementType = psiElement.getNode().getElementType();
      // System.out.println("elementType = " + elementType);
      if (!((elementType instanceof PyElementType) || (psiElement.getNode() instanceof PsiWhiteSpace)))
        break;
    }
    // System.out.println("forward==================");
    for (int i = 0; ; ++i) {
      final int cline = line + i;
      int offset;
      try {
        offset = DocumentUtil.getFirstNonSpaceCharOffset(document, cline);
        end = offset;
      } catch (IndexOutOfBoundsException ex) {
        break;
      }
      final PsiElement psiElement = psiFile.findElementAt(offset);
      // System.out.println("i = " + i);
      // System.out.println("cline = " + cline);
      // System.out.println("psiElement = " + psiElement);
      if (psiElement == null)
        break;
      // System.out.println("psiElement.getNode() = " + psiElement.getNode());
      final var elementType = psiElement.getNode().getElementType();
      // System.out.println("elementType = " + elementType);
      if (!((elementType instanceof PyElementType) || (psiElement.getNode() instanceof PsiWhiteSpace)))
        break;
    }
    {
      start = DocumentUtil.getLineEndOffset(start, document);
      end = DocumentUtil.getLineEndOffset(end, document);
      String codeToSend = editor.getDocument().getCharsSequence().subSequence(start, end).toString();
      // System.out.println("codeToSend = " + codeToSend);
      PyExecuteInConsole.executeCodeInConsole(e.getProject(), codeToSend, null, true, true, false,
          null);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
    if (editor != null) {
      final String selectionText = PySmartExecuteSelectionAction.getSelectionText(editor);
      if (selectionText != null) {
        PyExecuteInConsole.executeCodeInConsole(e.getProject(), selectionText, null, true, true, false, null);
      }
      else {
        if (inJupyterNotebookMode(e, editor))
          cellExecute(e, editor);
        else
          submitTopLevelCodeBlock(e, editor, CursorMoveAfterExecute.TO_NEXT_CODE_REGION);
      }
    }
  }
}
