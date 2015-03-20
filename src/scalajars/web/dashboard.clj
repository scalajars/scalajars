(ns scalajars.web.dashboard
  (:require [scalajars.web.common :refer [html-doc html-doc-with-large-header jar-link group-link tag days-from-now]]
            [scalajars.db :refer [jars-by-username find-groupnames recent-jars]]
            [scalajars.stats :as stats]
            [hiccup.element :refer [unordered-list link-to]]))

(defn recent-jar [jar-map]
  (let [stats (stats/all)
        description (:description jar-map)
        truncate-length 120
        days-ago (days-from-now (:created jar-map))]
    [:li.recent-jar
      [:div.date days-ago  " days ago"]
      [:div.timeline]
      [:div.infos
       (jar-link jar-map)
       [:p.descriptions
         (if (> (count description) truncate-length)
           (str (subs description 0 truncate-length) "...")
           description)]]
      [:div.meta
        [:span.name "vX.XX by XXX"]
        [:span.download (stats/download-count stats
                                              (:group_name jar-map)
                                              (:jar_name jar-map))]]]))

(defn index-page [account]
  (html-doc-with-large-header account nil
    [:div.or-repo
      [:div.or "or"]
      (link-to "/projects" "Browse the repository")]
    [:article.push-informations
     [:div.push-information
      [:h3.push-header "Push with Leiningen"]
      [:div.push-example
       [:pre.push-example-leiningen
        (tag "$") " lein deploy scalajars\n"]
       [:div.buttons
        (link-to "https://github.com/scalajars/scalajars/wiki/Tutorial" "View a tutorial")]]]
     [:div.push-information
      [:h3.push-header "Maven Repository"]
      [:div.push-example
       [:pre
        (tag "<repository>\n")
        (tag "  <id>") "scalajars.org" (tag "</id>\n")
        (tag "  <url>") "http://scalajars.org/repo" (tag "</url>\n")
        (tag "</repository>")]]]]
    [:div.recent-jars-header-container.row
     [:h2.recent-jars-header.col-md-12.col-lg-12.col-sm-12.col-xs-12
      "Recently pushed projects"]]
    [:ul.recent-jars-list.row (map recent-jar (recent-jars))]))

(defn dashboard [account]
  (html-doc account "Dashboard"
    [:div.light-article.col-md-12.col-lg-12.col-xs-12.col-sm-12
     [:h1 (str "Dashboard (" account ")")]
     [:div.col-md-4.col-lg-4.col-sm-4.col-xs-12
      [:div.dash-palette
       [:h2 "Your Projects"]
       (if (seq (jars-by-username account))
         (unordered-list (map jar-link (jars-by-username account)))
         [:p "You don't have any projects, would you like to "
          (link-to "http://wiki.github.com/ato/clojars-web/pushing" "add one")
          "?"])]]
     [:div.col-md-4.col-lg-4.col-sm-4.col-xs-12
      [:div.dash-palette
       [:h2 "Your Groups"]
       (unordered-list (map group-link (find-groupnames account)))]]
     [:div.col-md-4.col-lg-4.col-sm-4.col-xs-12
      [:div.dash-palette
       [:h2 "FAQ"]
       [:ul
        [:li (link-to "https://github.com/ato/clojars-web/wiki/Tutorial" "How I create a new project?")]
        [:li (link-to "http://wiki.github.com/ato/clojars-web/pushing" "How do I deploy to clojars?")]
        [:li (link-to "https://github.com/ato/clojars-web/wiki/Data" "How can I access scalajars data programatically?")]
        [:li (link-to "https://github.com/ato/clojars-web/wiki/Groups" "What are groups?")]
        [:li (link-to "https://github.com/ato/clojars-web/wiki/POM" "What does my POM need to look like?")]]]]]))
