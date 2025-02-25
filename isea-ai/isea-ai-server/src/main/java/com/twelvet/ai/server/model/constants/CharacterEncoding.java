package com.twelvet.ai.server.model.constants;

import lombok.Getter;

@Getter
public enum CharacterEncoding {
    UTF_8("UTF-8"),
    GBK("GBK"),
    GB2312("GB2312"),
    ISO_8859_1("ISO-8859-1");

    private final String value;

    CharacterEncoding(String value) {
        this.value = value;
    }

}
