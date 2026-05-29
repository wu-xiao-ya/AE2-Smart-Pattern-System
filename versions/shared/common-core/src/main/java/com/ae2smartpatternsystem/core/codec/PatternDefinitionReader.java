package com.ae2smartpatternsystem.core.codec;

import com.ae2smartpatternsystem.core.codec.tag.TagObject;
import com.ae2smartpatternsystem.core.model.PatternDefinition;

public interface PatternDefinitionReader {
    PatternDefinition read(TagObject root);
}

