package me.tinyoverflow.griefprevention.utils;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseUtilTest
{
    @Test
    void fromMacro()
    {
        CaseUtil caseUtil = CaseUtil.fromMacro("THESE_ARE_WORDS");
        validateWords(caseUtil.getWords());
    }

    @Test
    void fromKebab()
    {
        CaseUtil caseUtil = CaseUtil.fromKebab("these-are-words");
        validateWords(caseUtil.getWords());
    }

    @Test
    void toMacro()
    {
        String output = new CaseUtil(List.of("these", "are", "words")).toMacro();
        assertEquals("THESE_ARE_WORDS", output);
    }

    @Test
    void toKebab()
    {
        String output = new CaseUtil(List.of("these", "are", "words")).toKebab();
        assertEquals("these-are-words", output);
    }

    @Test
    void getWords()
    {
        CaseUtil caseUtil = new CaseUtil(List.of("these", "are", "words"));
        validateWords(caseUtil.getWords());
    }

    void validateWords(@NotNull List<String> words)
    {
        assertEquals(3, words.size());
        assertEquals("these", words.get(0));
        assertEquals("are", words.get(1));
        assertEquals("words", words.get(2));
    }
}