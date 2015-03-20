(ns scalajars.web.error-page
  (:require [scalajars.web.common :refer [html-doc]]
            [ring.util.response :refer [response status content-type]]
            [hiccup.element :refer [link-to]]
            [clj-stacktrace.repl :refer [pst]]))

(defn error-page-response [throwable]
  (-> (response (html-doc nil
                 "Oops, we encountered an error"
                 [:div.small-section
                  [:h1 "Oops!"]
                  [:p
                   "It seems as if an internal system error has occurred. Please give it another try. If it still doesn't work please "
                   (link-to "https://github.com/scalajars/scalajars/issues" "open an issue.")]
                  [:p "Including the following stack trace would also be helpful."]
                  [:pre.stacktrace (with-out-str (pst throwable))]]))
      (status 500)
      (content-type "text/html")))

(defn wrap-exceptions [app]
  (fn [req]
    (try
      (app req)
      (catch Throwable t
        (println (str "A server error has occured: " (.getMessage t)))
        (pst t)
        (error-page-response t)))))
