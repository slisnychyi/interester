package org.interester.report;

import org.interester.analyze.Text;
import org.interester.model.AnalyzeResponse.Query;
import org.interester.model.TopicResult;

record Labels(boolean manyTopics, boolean manyLangs) {

    static Labels of(Query query) {
        return new Labels(query.topics().size() > 1, query.langs().size() > 1);
    }

    String of(TopicResult r) {
        String lang = Text.languageName(r.lang()) + " (" + r.lang() + ")";
        if (!manyTopics) {
            return lang;
        }
        return manyLangs ? r.topic() + " · " + lang : r.topic();
    }
}
