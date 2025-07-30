// This is a generated file. Not intended for manual editing.
package com.jetbrains.rider.plugins.atomic.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.jetbrains.rider.plugins.atomic.psi.AtomicTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.jetbrains.rider.plugins.atomic.psi.*;

public class AtomicTagsSectionImpl extends ASTWrapperPsiElement implements AtomicTagsSection {

  public AtomicTagsSectionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AtomicVisitor visitor) {
    visitor.visitTagsSection(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AtomicVisitor) accept((AtomicVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<AtomicTagItem> getTagItemList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicTagItem.class);
  }

}
