package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lexer.FlexAdapter

class AtomicLexerAdapter : FlexAdapter(AtomicLexer(null))