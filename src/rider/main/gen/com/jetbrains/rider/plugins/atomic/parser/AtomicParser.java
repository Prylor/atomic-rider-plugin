// This is a generated file. Not intended for manual editing.
package com.jetbrains.rider.plugins.atomic.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.jetbrains.rider.plugins.atomic.psi.AtomicTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class AtomicParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return atomicFile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // AGGRESSIVE_INLINING_KEYWORD COLON (TRUE | FALSE)
  public static boolean aggressive_inlining_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "aggressive_inlining_prop")) return false;
    if (!nextTokenIs(builder_, AGGRESSIVE_INLINING_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, AGGRESSIVE_INLINING_PROP, null);
    result_ = consumeTokens(builder_, 1, AGGRESSIVE_INLINING_KEYWORD, COLON);
    pinned_ = result_; // pin = 1
    result_ = result_ && aggressive_inlining_prop_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // TRUE | FALSE
  private static boolean aggressive_inlining_prop_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "aggressive_inlining_prop_2")) return false;
    boolean result_;
    result_ = consumeToken(builder_, TRUE);
    if (!result_) result_ = consumeToken(builder_, FALSE);
    return result_;
  }

  /* ********************************************************** */
  // file_content
  static boolean atomicFile(PsiBuilder builder_, int level_) {
    return file_content(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // CLASS_NAME_KEYWORD COLON IDENTIFIER
  public static boolean class_name_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_name_prop")) return false;
    if (!nextTokenIs(builder_, CLASS_NAME_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CLASS_NAME_PROP, null);
    result_ = consumeTokens(builder_, 1, CLASS_NAME_KEYWORD, COLON, IDENTIFIER);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // DIRECTORY_KEYWORD COLON (STRING | IDENTIFIER | TYPE_REFERENCE)
  public static boolean directory_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directory_prop")) return false;
    if (!nextTokenIs(builder_, DIRECTORY_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DIRECTORY_PROP, null);
    result_ = consumeTokens(builder_, 1, DIRECTORY_KEYWORD, COLON);
    pinned_ = result_; // pin = 1
    result_ = result_ && directory_prop_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // STRING | IDENTIFIER | TYPE_REFERENCE
  private static boolean directory_prop_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directory_prop_2")) return false;
    boolean result_;
    result_ = consumeToken(builder_, STRING);
    if (!result_) result_ = consumeToken(builder_, IDENTIFIER);
    if (!result_) result_ = consumeToken(builder_, TYPE_REFERENCE);
    return result_;
  }

  /* ********************************************************** */
  // ENTITY_TYPE_KEYWORD COLON IDENTIFIER
  public static boolean entity_type_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "entity_type_prop")) return false;
    if (!nextTokenIs(builder_, ENTITY_TYPE_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ENTITY_TYPE_PROP, null);
    result_ = consumeTokens(builder_, 1, ENTITY_TYPE_KEYWORD, COLON, IDENTIFIER);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // (header_section | section | COMMENT | CRLF)*
  static boolean file_content(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "file_content")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!file_content_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "file_content", pos_)) break;
    }
    return true;
  }

  // header_section | section | COMMENT | CRLF
  private static boolean file_content_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "file_content_0")) return false;
    boolean result_;
    result_ = header_section(builder_, level_ + 1);
    if (!result_) result_ = section(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, COMMENT);
    if (!result_) result_ = consumeToken(builder_, CRLF);
    return result_;
  }

  /* ********************************************************** */
  // entity_type_prop | aggressive_inlining_prop | unsafe_prop | namespace_prop | class_name_prop | directory_prop | solution_prop
  static boolean header_property(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "header_property")) return false;
    boolean result_;
    result_ = entity_type_prop(builder_, level_ + 1);
    if (!result_) result_ = aggressive_inlining_prop(builder_, level_ + 1);
    if (!result_) result_ = unsafe_prop(builder_, level_ + 1);
    if (!result_) result_ = namespace_prop(builder_, level_ + 1);
    if (!result_) result_ = class_name_prop(builder_, level_ + 1);
    if (!result_) result_ = directory_prop(builder_, level_ + 1);
    if (!result_) result_ = solution_prop(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // header_property+
  public static boolean header_section(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "header_section")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, HEADER_SECTION, "<header section>");
    result_ = header_property(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!header_property(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "header_section", pos_)) break;
    }
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // HYPHEN IMPORT_PATH
  public static boolean import_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "import_item")) return false;
    if (!nextTokenIs(builder_, HYPHEN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, IMPORT_ITEM, null);
    result_ = consumeTokens(builder_, 1, HYPHEN, IMPORT_PATH);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // import_item*
  static boolean import_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "import_list")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!import_item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "import_list", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // IMPORTS_KEYWORD COLON? import_list
  public static boolean imports_section(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "imports_section")) return false;
    if (!nextTokenIs(builder_, IMPORTS_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, IMPORTS_SECTION, null);
    result_ = consumeToken(builder_, IMPORTS_KEYWORD);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, imports_section_1(builder_, level_ + 1));
    result_ = pinned_ && import_list(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // COLON?
  private static boolean imports_section_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "imports_section_1")) return false;
    consumeToken(builder_, COLON);
    return true;
  }

  /* ********************************************************** */
  // NAMESPACE_KEYWORD COLON NAMESPACE_VALUE
  public static boolean namespace_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namespace_prop")) return false;
    if (!nextTokenIs(builder_, NAMESPACE_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, NAMESPACE_PROP, null);
    result_ = consumeTokens(builder_, 1, NAMESPACE_KEYWORD, COLON, NAMESPACE_VALUE);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // imports_section | tags_section | values_section
  static boolean section(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "section")) return false;
    boolean result_;
    result_ = imports_section(builder_, level_ + 1);
    if (!result_) result_ = tags_section(builder_, level_ + 1);
    if (!result_) result_ = values_section(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // SOLUTION_KEYWORD COLON (STRING | IDENTIFIER)
  public static boolean solution_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "solution_prop")) return false;
    if (!nextTokenIs(builder_, SOLUTION_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SOLUTION_PROP, null);
    result_ = consumeTokens(builder_, 1, SOLUTION_KEYWORD, COLON);
    pinned_ = result_; // pin = 1
    result_ = result_ && solution_prop_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // STRING | IDENTIFIER
  private static boolean solution_prop_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "solution_prop_2")) return false;
    boolean result_;
    result_ = consumeToken(builder_, STRING);
    if (!result_) result_ = consumeToken(builder_, IDENTIFIER);
    return result_;
  }

  /* ********************************************************** */
  // HYPHEN TAG_NAME
  public static boolean tag_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tag_item")) return false;
    if (!nextTokenIs(builder_, HYPHEN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TAG_ITEM, null);
    result_ = consumeTokens(builder_, 1, HYPHEN, TAG_NAME);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // tag_item*
  static boolean tag_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tag_list")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "tag_list", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAGS_KEYWORD COLON? tag_list
  public static boolean tags_section(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tags_section")) return false;
    if (!nextTokenIs(builder_, TAGS_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TAGS_SECTION, null);
    result_ = consumeToken(builder_, TAGS_KEYWORD);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, tags_section_1(builder_, level_ + 1));
    result_ = pinned_ && tag_list(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // COLON?
  private static boolean tags_section_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tags_section_1")) return false;
    consumeToken(builder_, COLON);
    return true;
  }

  /* ********************************************************** */
  // TYPE_REFERENCE+
  static boolean type_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_expression")) return false;
    if (!nextTokenIs(builder_, TYPE_REFERENCE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, TYPE_REFERENCE);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, TYPE_REFERENCE)) break;
      if (!empty_element_parsed_guard_(builder_, "type_expression", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // UNSAFE_KEYWORD COLON (TRUE | FALSE)
  public static boolean unsafe_prop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unsafe_prop")) return false;
    if (!nextTokenIs(builder_, UNSAFE_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, UNSAFE_PROP, null);
    result_ = consumeTokens(builder_, 1, UNSAFE_KEYWORD, COLON);
    pinned_ = result_; // pin = 1
    result_ = result_ && unsafe_prop_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // TRUE | FALSE
  private static boolean unsafe_prop_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unsafe_prop_2")) return false;
    boolean result_;
    result_ = consumeToken(builder_, TRUE);
    if (!result_) result_ = consumeToken(builder_, FALSE);
    return result_;
  }

  /* ********************************************************** */
  // COMMENT
  static boolean value_comment(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, COMMENT);
  }

  /* ********************************************************** */
  // HYPHEN VALUE_NAME COLON type_expression
  public static boolean value_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_item")) return false;
    if (!nextTokenIs(builder_, HYPHEN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, VALUE_ITEM, null);
    result_ = consumeTokens(builder_, 1, HYPHEN, VALUE_NAME, COLON);
    pinned_ = result_; // pin = 1
    result_ = result_ && type_expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // (value_item | value_comment)*
  static boolean value_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_list")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!value_list_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "value_list", pos_)) break;
    }
    return true;
  }

  // value_item | value_comment
  private static boolean value_list_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_list_0")) return false;
    boolean result_;
    result_ = value_item(builder_, level_ + 1);
    if (!result_) result_ = value_comment(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // VALUES_KEYWORD COLON? value_list
  public static boolean values_section(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "values_section")) return false;
    if (!nextTokenIs(builder_, VALUES_KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, VALUES_SECTION, null);
    result_ = consumeToken(builder_, VALUES_KEYWORD);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, values_section_1(builder_, level_ + 1));
    result_ = pinned_ && value_list(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // COLON?
  private static boolean values_section_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "values_section_1")) return false;
    consumeToken(builder_, COLON);
    return true;
  }

}
