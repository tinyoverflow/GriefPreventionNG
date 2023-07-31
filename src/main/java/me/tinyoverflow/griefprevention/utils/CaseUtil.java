package me.tinyoverflow.griefprevention.utils;

import java.util.Arrays;
import java.util.List;

public class CaseUtil
{
    private final List<String> words;

    public CaseUtil(List<String> words)
    {
        this.words = words;
    }

    public static CaseUtil fromMacro(String input)
    {
        List<String> words = Arrays.stream(input.split("_"))
                .map(String::toLowerCase)
                .toList();

        return new CaseUtil(words);
    }

    public static CaseUtil fromKebab(String input)
    {
        List<String> words = Arrays.stream(input.split("-"))
                .map(String::toLowerCase)
                .toList();

        return new CaseUtil(words);
    }

    public String toMacro()
    {
        return String.join("_", words.stream().map(String::toUpperCase).toList());
    }

    public String toKebab()
    {
        return String.join("-", words);
    }

    public List<String> getWords()
    {
        return words;
    }
}
