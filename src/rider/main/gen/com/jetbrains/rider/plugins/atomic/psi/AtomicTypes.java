// This is a generated file. Not intended for manual editing.
package com.jetbrains.rider.plugins.atomic.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.jetbrains.rider.plugins.atomic.psi.impl.*;

public interface AtomicTypes {

  IElementType AGGRESSIVE_INLINING_PROP = new AtomicElementType("AGGRESSIVE_INLINING_PROP");
  IElementType CLASS_NAME_PROP = new AtomicElementType("CLASS_NAME_PROP");
  IElementType DIRECTORY_PROP = new AtomicElementType("DIRECTORY_PROP");
  IElementType ENTITY_TYPE_PROP = new AtomicElementType("ENTITY_TYPE_PROP");
  IElementType HEADER_PROP = new AtomicElementType("HEADER_PROP");
  IElementType HEADER_SECTION = new AtomicElementType("HEADER_SECTION");
  IElementType IMPORTS_SECTION = new AtomicElementType("IMPORTS_SECTION");
  IElementType IMPORT_ITEM = new AtomicElementType("IMPORT_ITEM");
  IElementType NAMESPACE_PROP = new AtomicElementType("NAMESPACE_PROP");
  IElementType SOLUTION_PROP = new AtomicElementType("SOLUTION_PROP");
  IElementType TAGS_SECTION = new AtomicElementType("TAGS_SECTION");
  IElementType TAG_ITEM = new AtomicElementType("TAG_ITEM");
  IElementType UNSAFE_PROP = new AtomicElementType("UNSAFE_PROP");
  IElementType VALUES_SECTION = new AtomicElementType("VALUES_SECTION");
  IElementType VALUE_ITEM = new AtomicElementType("VALUE_ITEM");

  IElementType AGGRESSIVE_INLINING_KEYWORD = new AtomicTokenType("AGGRESSIVE_INLINING_KEYWORD");
  IElementType CLASS_NAME_KEYWORD = new AtomicTokenType("CLASS_NAME_KEYWORD");
  IElementType COLON = new AtomicTokenType("COLON");
  IElementType COMMENT = new AtomicTokenType("COMMENT");
  IElementType CRLF = new AtomicTokenType("CRLF");
  IElementType DIRECTORY_KEYWORD = new AtomicTokenType("DIRECTORY_KEYWORD");
  IElementType ENTITY_TYPE_KEYWORD = new AtomicTokenType("ENTITY_TYPE_KEYWORD");
  IElementType FALSE = new AtomicTokenType("FALSE");
  IElementType HEADER_KEYWORD = new AtomicTokenType("HEADER_KEYWORD");
  IElementType HYPHEN = new AtomicTokenType("HYPHEN");
  IElementType IDENTIFIER = new AtomicTokenType("IDENTIFIER");
  IElementType IMPORTS_KEYWORD = new AtomicTokenType("IMPORTS_KEYWORD");
  IElementType IMPORT_PATH = new AtomicTokenType("IMPORT_PATH");
  IElementType NAMESPACE_KEYWORD = new AtomicTokenType("NAMESPACE_KEYWORD");
  IElementType NAMESPACE_VALUE = new AtomicTokenType("NAMESPACE_VALUE");
  IElementType SOLUTION_KEYWORD = new AtomicTokenType("SOLUTION_KEYWORD");
  IElementType STRING = new AtomicTokenType("STRING");
  IElementType TAGS_KEYWORD = new AtomicTokenType("TAGS_KEYWORD");
  IElementType TAG_NAME = new AtomicTokenType("TAG_NAME");
  IElementType TRUE = new AtomicTokenType("TRUE");
  IElementType TYPE_REFERENCE = new AtomicTokenType("TYPE_REFERENCE");
  IElementType UNSAFE_KEYWORD = new AtomicTokenType("UNSAFE_KEYWORD");
  IElementType VALUES_KEYWORD = new AtomicTokenType("VALUES_KEYWORD");
  IElementType VALUE_NAME = new AtomicTokenType("VALUE_NAME");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == AGGRESSIVE_INLINING_PROP) {
        return new AtomicAggressiveInliningPropImpl(node);
      }
      else if (type == CLASS_NAME_PROP) {
        return new AtomicClassNamePropImpl(node);
      }
      else if (type == DIRECTORY_PROP) {
        return new AtomicDirectoryPropImpl(node);
      }
      else if (type == ENTITY_TYPE_PROP) {
        return new AtomicEntityTypePropImpl(node);
      }
      else if (type == HEADER_PROP) {
        return new AtomicHeaderPropImpl(node);
      }
      else if (type == HEADER_SECTION) {
        return new AtomicHeaderSectionImpl(node);
      }
      else if (type == IMPORTS_SECTION) {
        return new AtomicImportsSectionImpl(node);
      }
      else if (type == IMPORT_ITEM) {
        return new AtomicImportItemImpl(node);
      }
      else if (type == NAMESPACE_PROP) {
        return new AtomicNamespacePropImpl(node);
      }
      else if (type == SOLUTION_PROP) {
        return new AtomicSolutionPropImpl(node);
      }
      else if (type == TAGS_SECTION) {
        return new AtomicTagsSectionImpl(node);
      }
      else if (type == TAG_ITEM) {
        return new AtomicTagItemImpl(node);
      }
      else if (type == UNSAFE_PROP) {
        return new AtomicUnsafePropImpl(node);
      }
      else if (type == VALUES_SECTION) {
        return new AtomicValuesSectionImpl(node);
      }
      else if (type == VALUE_ITEM) {
        return new AtomicValueItemImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
