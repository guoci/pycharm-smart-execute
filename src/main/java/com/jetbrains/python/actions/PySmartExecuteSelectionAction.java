package com.jetbrains.python.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.impl.PyIfPartElifImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PySmartExecuteSelectionAction extends AnAction {

  private static String getNLinesAfterCaret(final Editor editor, final int N) {
    LogicalPosition caretPos = editor.getCaretModel().getLogicalPosition();

//    Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, caretPos, caretPos);

//    LogicalPosition lineStart = lines.first;
    LogicalPosition lineStart = new LogicalPosition(caretPos.line, 0);
    int start = editor.logicalPositionToOffset(lineStart);
//    int end = DocumentUtil.getLineTextRange(editor.getDocument(), caretPos.getLine() + N).getEndOffset();
    LogicalPosition cp = caretPos;
    for (int i = 0; i <= N; ++i) {
      cp = new LogicalPosition(cp.line + 1, 0);
    }
    final int end = editor.logicalPositionToOffset(cp);
//    final int end = editor.logicalPositionToOffset(EditorUtil.calcSurroundingRange(editor, cp, cp).second);
    return editor.getDocument().getCharsSequence().subSequence(start, end).toString();
  }

  /*
   returns true if PsiElement not an evaluable Python statement
   */
  private static boolean isPartialStatement(PsiElement psiElement) {
    return psiElement instanceof PyElsePart ||
        psiElement instanceof PyIfPartElifImpl ||
        psiElement instanceof PyIfPart ||
        psiElement instanceof PyWhilePart ||
        psiElement instanceof PyExceptPart ||
        psiElement instanceof PyFinallyPart ||
        psiElement instanceof PyStatementPart ||
        psiElement instanceof PyStatementList;
  }

  /*
  closest parent that is evaluable
   */
  private static PsiElement getEvaluableParent(PsiElement psiElement) {
    if (psiElement.getNode().getElementType() == PyTokenTypes.ELSE_KEYWORD ||
        psiElement.getNode().getElementType() == PyTokenTypes.ELIF_KEYWORD ||
        psiElement.getNode().getElementType() == PyTokenTypes.EXCEPT_KEYWORD ||
        psiElement.getNode().getElementType() == PyTokenTypes.FINALLY_KEYWORD) {
      psiElement = psiElement.getParent();
    }
    return isPartialStatement(psiElement) ? psiElement.getParent() : psiElement;
  }

  private static void syntaxErrorAction(final AnActionEvent e) {
    PyExecuteInConsole.executeCodeInConsole(e.getProject(), "# syntax error", null, true, true, false, null);
  }
  static void moveCaretDown(final Editor editor, final int numLinesToSubmit) {
    final LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
    editor.getCaretModel().moveToOffset(
            editor.logicalPositionToOffset(
                    new LogicalPosition(pos.line + numLinesToSubmit, pos.column)));
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

//    VisualPosition pos = editor.getCaretModel().getVisualPosition();
//    Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, pos, pos);
//    int offset = editor.getCaretModel().getOffset();
//
//    LogicalPosition lineStart = lines.first;
//    LogicalPosition nextLineStart = lines.second;
//
//    int start = editor.logicalPositionToOffset(lineStart);
//    int end = editor.logicalPositionToOffset(nextLineStart);
//
//    Document document = editor.getDocument();
//
//    if (nextLineStart.line < document.getLineCount()) {
//
//      int newOffset = end + offset - start;
//
//      int nextLineEndOffset = document.getLineEndOffset(nextLineStart.line);
//      if (newOffset >= nextLineEndOffset) {
//        newOffset = nextLineEndOffset;
//      }
//
//      editor.getCaretModel().moveToOffset(newOffset);
//      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
//    }
  }

  enum CursorMoveAfterExecute {
    NO_MOVE, TO_THE_END_OF_CODE_BLOCK, TO_NEXT_CODE_REGION;
  }
  static void smartExecuteCode(final AnActionEvent e, final Editor editor, final CursorMoveAfterExecute cursorMoveAfterExecute) {
    final Document document = editor.getDocument();
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(e.getProject());
    psiDocumentManager.commitDocument(document);
    final PsiFile psiFile = psiDocumentManager.getPsiFile(document);

    final LogicalPosition logicalPos = editor.getCaretModel().getLogicalPosition();
    final int line = logicalPos.line;

    final int offset = DocumentUtil.getFirstNonSpaceCharOffset(document, line);
    final PsiElement psiElement = psiFile.findElementAt(offset);
    int numLinesToSubmit = document.getLineCount() - line;
    PsiElement lastCommonParent = null;
    for (int i = 0; line + i < document.getLineCount(); ++i) {
      final int lineStartOffset = DocumentUtil.getFirstNonSpaceCharOffset(document, line + i);
      final PsiElement pe = psiFile.findElementAt(lineStartOffset);
      if (psiElement == null)  // at last line
        return;
      final PsiElement commonParentRaw = pe == null ? psiElement.getContainingFile() : PsiTreeUtil.findCommonParent(psiElement, pe);
      if (commonParentRaw instanceof PsiDirectory) {// last line of notebook cell
        numLinesToSubmit = i;
        break;
      }
      final PsiElement commonParent = getEvaluableParent(commonParentRaw);
      if (commonParent.getTextOffset() < offset ||
          commonParent instanceof PyFile) { // at new statement
        numLinesToSubmit = i;
        break;
      }
      lastCommonParent = commonParent;
    }
    if (lastCommonParent == null) {
      if (psiElement instanceof PsiWhiteSpace) { // if we are at a blank line
        moveCaretDown(editor, 1);
        return;
      }
      syntaxErrorAction(e);
      return;
    }
    { // if at folded code, submit folded code
      final VisualPosition caretPos = editor.getCaretModel().getVisualPosition();
      final int l1 = editor.visualToLogicalPosition(new VisualPosition(caretPos.getLine(), 0)).line;
      final int l2 = editor.visualToLogicalPosition(new VisualPosition(caretPos.getLine() + 1, 0)).line;
      if (l1 + 1 < l2)
        numLinesToSubmit = l2 - l1;
    }
    String codeToSend =
        numLinesToSubmit == 0 ? "" :
            getNLinesAfterCaret(editor, numLinesToSubmit - 1);
    if (PsiTreeUtil.hasErrorElements(lastCommonParent) ||
        psiElement.getTextOffset() < offset) {
      codeToSend = null;
    }
    codeToSend = codeToSend == null ? null : codeToSend.trim();

    if (codeToSend != null && !codeToSend.isEmpty()) {
      PyExecuteInConsole.executeCodeInConsole(e.getProject(), codeToSend, null, true, true, false, null);
    }
    if (codeToSend != null) {
      if (cursorMoveAfterExecute == CursorMoveAfterExecute.TO_NEXT_CODE_REGION
          || cursorMoveAfterExecute == CursorMoveAfterExecute.TO_THE_END_OF_CODE_BLOCK) {
        moveCaretDown(editor, numLinesToSubmit);
      }
      if (cursorMoveAfterExecute == CursorMoveAfterExecute.TO_NEXT_CODE_REGION) {
        for (; ; ) { // skip comments and whitespace
          final int currentOffset = DocumentUtil.getFirstNonSpaceCharOffset(document,
              editor.getCaretModel().getLogicalPosition().line);
          final PsiElement pe = psiFile.findElementAt(currentOffset);
          if (pe != null && (pe.getNode().getElementType() == PyTokenTypes.END_OF_LINE_COMMENT
              || pe.getNode() instanceof PsiWhiteSpace)) {
            moveCaretDown(editor, 1);
          } else {
            break;
          }
        }
      }
    } else {
      syntaxErrorAction(e);
      return;
    }
  }

  @Nullable
  static String getSelectionText(@NotNull Editor editor) {
    if (editor.getSelectionModel().hasSelection()) {
      SelectionModel model = editor.getSelectionModel();

      return model.getSelectedText();
    }
    else {
      return null;
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
    if (editor != null) {
      final String selectionText = getSelectionText(editor);
      if (selectionText != null) {
        PyExecuteInConsole.executeCodeInConsole(e.getProject(), selectionText, null, true, true, false, null);
      }
      else {
        smartExecuteCode(e, editor, CursorMoveAfterExecute.TO_NEXT_CODE_REGION);
      }
    }
  }
}
