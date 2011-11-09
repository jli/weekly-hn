(ns weekly-hn.util)

(defn ms->date [ms]
  (let [formatter (java.text.SimpleDateFormat. "yyyy-MM-dd")]
    (.format formatter (java.util.Date. ms))))
