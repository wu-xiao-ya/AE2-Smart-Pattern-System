package com.wuxiaoya.techstart.core.codec;

import com.wuxiaoya.techstart.core.codec.tag.TagObject;
import com.wuxiaoya.techstart.core.model.PatternDefinition;

public interface PatternDefinitionReader {
    PatternDefinition read(TagObject root);
}

