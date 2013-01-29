(ns weekly-hn.site
  (:use [weekly-hn.util :only [ms->date]]
        [weekly-hn.scrape :only [log spectatoritis-is-a-cancer]]))

(def site-dir "resources/public")

;; hmm, how to do this?
(defn save-page
  "Writes down a static page so we won't have to generate it again."
  [path html]
  (log "saving" path)
  (spit (str site-dir "/" path) html))

(def style [:style "
body { background-color: #fafafa; }
h1 { display: inline; font-size: inherit; }
h1 a { text-decoration: none; }
h2 { display: inline; font-size: inherit; }
#top {
  padding: 0.2em;
  border: dotted thin;
}
#top > #extra { font-size: smaller; }
#bot {
  border: dotted thin;
  padding: 0.2em;
  overflow: auto;
}
#foot {
  margin-top:0.2em; /*boo*/
  float: right;
  font-size: x-small;
}
/*#rationale { width: 40em; }*/
#stuff { margin: 1em 0 0 0.5em; }
#issue .flood { font-size: xx-small; float: right;}
#issue li { color: #999; padding: 0.1em; }
#issue .site { /* after links */
  font-size: small;
  color: #666;
  padding-left: 0.2em;
}
#issue .posttime { /* after site */
  font-size: x-small;
  color: #888;
  background-color: #eee;
  position: absolute; right: 1em; /* not great */
  border-radius: 0.3em;
  padding: 0.1em;
}
/* man, I sure don't understand how these pseudoclasses work. stuff on :visited doesn't work like I expect. */
a:link, a:visited { color: #333; }
a:hover { color: #111; background-color: #f84; }
a:active { color: #f84; background-color: #fafafa; }

#issue a:link { text-decoration: none }
#issue a:visited { color: #999; text-decoration: none }

a.points:link, a.points:visited { color: #f62; }
a.points:hover { color: #111; background-color: #f84; }
a.points:active { color: #111; background-color: #fafafa; }

a[href=\"iip\"] { font-size: small; }"])

(defn hn-link [id] (str "https://news.ycombinator.com/item?id=" id))

(defn maybe-hn [link]
  (if (.startsWith link "item?id=")
    (str "https://news.ycombinator.com/" link)
    link))

(defn site-base [url]
  (nth (re-find #"^https?://(?:www\.)?([^/]+).*$" url) 1))

(defn ms->cal [ms]
  (doto (java.util.Calendar/getInstance)
    (.setTime (java.util.Date. ms))))

;; god
(defn post-time [ms]
  (when ms
    (let [pad #(format "%02d" %)
          dt (ms->cal ms)
          mon (inc (.get dt java.util.Calendar/MONTH))
          dayom (.get dt java.util.Calendar/DAY_OF_MONTH)
          hr    (.get dt java.util.Calendar/HOUR_OF_DAY)
          min   (.get dt java.util.Calendar/MINUTE)
          hrs  (when-not (zero? hr) (str " " (pad hr)))
          mins (when-not (zero? min) (str ":" (pad min)))]
      (seq [" " (pad mon) "/" (pad dayom) hrs mins]))))

(defn gen-issue [stories title link nstories full?]
  (let [story->li (fn [{:keys [id link title points user comments time]}]
                    [:li
                     [:span
                      [:a.points {:href (hn-link id)} (str points)]
                      " " [:a {:href (maybe-hn link)} title]
                      " " [:span.site (site-base link)]
                      [:span.posttime (post-time time)]]])
        [flood-link link-text] (if full?
                                 [link "(restore sanity)"]
                                 [(str link ".full")
                                  (str "TERRIBLE FLOOD (" nstories ")")])]
    [:div#issue
     [:div.head
      [:h2 title]
      [:a.flood {:href flood-link} link-text]]
     [:ol#storylist
      (map story->li stories)]]))

(defn gen-issue-index [issue-index]
  (let [date->link (fn [s] [:span [:a {:href s} s]])
        in-prog [:span [:a {:href "iip"} "issue in-progress"]]]
    (interpose " " (cons in-prog (map date->link issue-index)))))

(defn skel [title issue-index stuff]
  [:html
   [:head
    [:title "weekly hn - " title]
    style]
   [:body
    [:div#top
     [:h1 [:a {:href "/"} "weekly hn"]]
     " "
     [:span#extra [:a {:href "/rationale"} "rationale"]]]
    [:div#stuff stuff]
    [:div#bot
     [:span#index (gen-issue-index issue-index)]
     [:span#foot
      [:a {:href "https://github.com/jli/weekly-hn"} "src."]
      " &copy; 2011 "
      [:a {:href "http://circularly.org/"} "jli"]
      " "
      [:a {:href "http://www.wtfpl.net/txt/copying/"} "WTFPL"]]]
    ]])

(defn issue-page [title link stories issue-index full?]
  (let [nstories (count stories)
        stories (if full? stories (take spectatoritis-is-a-cancer stories))]
    (skel title issue-index (gen-issue stories title link nstories full?))))

(def rationale
"<p><a href=\"https://news.ycombinator.com/\">Cool things</a> are
cool, but potentially <a
href=\"http://www.paulgraham.com/addiction.html\">addictive
timesinks</a>. <strong>weekly hn</strong> regularly visits HN and
publishes the top 50 stories each week on Sunday. This is better,
because:</p>

<ul>
<li>You can ignore the frivolous. (>500 stories hit the front page each
week. Most are forgettable.)</li>
<li>You're not pressured to poll, for fear of missing something.</li>
<li>You can batch up your nerd reading. Spend the rest of the week
hacking!</li>
</ul>

<p>This isn't perfect because:</p>

<ul>
<li>Frivolities may score higher than less-accessible,
more-interesting stuff.</li>
<li>You miss the chance to participate in the discussion.</li>
</ul>

<p>Why Sunday? Because <em>Saturdays are for hacking</em>.</p>

<p>For more eloquent thoughts, see:
<a href=\"https://en.wikipedia.org/wiki/Amusing_Ourselves_to_Death\">Amusing Ourselves to Death</a>
and <a href=\"https://en.wikipedia.org/wiki/Flow_%28psychology%29\">flow</a>.
<br>
For a different approach (with a cooler interface), see
<a href=\"http://hckrnews.com/\">hckrnews</a>.</p>

<p>thx. <a href=\"http://circularly.org/\">jli</a>.</p>")

(defn rationale-page [issue-index]
  (skel "\"rationale\"" issue-index
        (seq [[:h2.head "\"rationale\""] [:div#rationale rationale]])))
