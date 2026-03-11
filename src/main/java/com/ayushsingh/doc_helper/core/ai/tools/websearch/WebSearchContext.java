package com.ayushsingh.doc_helper.core.ai.tools.websearch;

import com.ayushsingh.doc_helper.core.ai.tools.websearch.dto.WebSearchItem;

import java.util.ArrayList;
import java.util.List;

public class WebSearchContext {

    private static final ThreadLocal<List<WebSearchItem>> holder =
            ThreadLocal.withInitial(ArrayList::new);

    public static void set(List<WebSearchItem> items) {
        holder.set(items);
    }

    public static List<WebSearchItem> get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}