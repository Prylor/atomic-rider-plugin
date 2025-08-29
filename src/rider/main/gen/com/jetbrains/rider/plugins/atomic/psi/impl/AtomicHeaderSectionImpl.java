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

public class AtomicHeaderSectionImpl extends ASTWrapperPsiElement implements AtomicHeaderSection {

  public AtomicHeaderSectionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AtomicVisitor visitor) {
    visitor.visitHeaderSection(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AtomicVisitor) accept((AtomicVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<AtomicAggressiveInliningProp> getAggressiveInliningPropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicAggressiveInliningProp.class);
  }

  @Override
  @NotNull
  public List<AtomicClassNameProp> getClassNamePropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicClassNameProp.class);
  }

  @Override
  @NotNull
  public List<AtomicDirectoryProp> getDirectoryPropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicDirectoryProp.class);
  }

  @Override
  @NotNull
  public List<AtomicEntityTypeProp> getEntityTypePropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicEntityTypeProp.class);
  }

  @Override
  @NotNull
  public List<AtomicNamespaceProp> getNamespacePropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicNamespaceProp.class);
  }

  @Override
  @NotNull
  public List<AtomicSolutionProp> getSolutionPropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicSolutionProp.class);
  }

  @Override
  @NotNull
  public List<AtomicUnsafeProp> getUnsafePropList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AtomicUnsafeProp.class);
  }

}
