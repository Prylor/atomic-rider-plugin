// This is a generated file. Not intended for manual editing.
package com.jetbrains.rider.plugins.atomic.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface AtomicHeaderSection extends PsiElement {

  @NotNull
  List<AtomicAggressiveInliningProp> getAggressiveInliningPropList();

  @NotNull
  List<AtomicClassNameProp> getClassNamePropList();

  @NotNull
  List<AtomicDirectoryProp> getDirectoryPropList();

  @NotNull
  List<AtomicEntityTypeProp> getEntityTypePropList();

  @NotNull
  List<AtomicHeaderProp> getHeaderPropList();

  @NotNull
  List<AtomicNamespaceProp> getNamespacePropList();

  @NotNull
  List<AtomicSolutionProp> getSolutionPropList();

  @NotNull
  List<AtomicUnsafeProp> getUnsafePropList();

}
