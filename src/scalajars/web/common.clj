(ns scalajars.web.common
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css include-js]]
            [hiccup.element :refer [link-to unordered-list image]]
            [scalajars.web.safe-hiccup :refer [html5 raw form-to]]))

(defn when-ie [& contents]
  (str
   "<!--[if lt IE 9]>"
   (html contents)
   "<![endif]-->"))

(def footer
  [:footer.row
   (link-to "/projects" "projects")
   (link-to "https://github.com/scalajars/scalajars" "code")
   (link-to "/security" "security")
   (link-to "https://github.com/scalajars/scalajars/wiki" "help")
   [:div.row.bendyworks
    "remixed by"
    (link-to {:target "_blank"}
             "http://www.bendyworks.com/"
             (image "/images/bendyworks_logo_white.png" "Bendyworks Inc."))]])

(defn google-analytics-js []
  [])
  ; [:script "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  ; (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  ; m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  ; })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ; ga('create', 'UA-51806851-1', 'scalajars');
  ; ga('send', 'pageview');"])

(defn typekit-js []
  [:script "try{Typekit.load();}catch(e){}"])

(defn html-doc [account title & body]
  (html5
   [:head
    [:link {:type "application/opensearchdescription+xml"
            :href "/opensearch.xml"
            :rel "search"}]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
    [:title
     (when title
       (str title " - "))
     "Scalajars"]
    (map #(include-css (str "/stylesheets/" %))
         ;; Bootstrap was customized to only include the 'grid' styles
         ;; (then the default colors were removed)
         ;; more info: http://getbootstrap.com/css/#grid
         ["reset.css" "vendor/bootstrap/bootstrap.css" "screen.css"])
    (include-js "//use.typekit.net/zhw0tse.js")
    (typekit-js)
    (raw (when-ie (include-js "/js/html5.js")))]
   [:body.container-fluid
    [:div#content-wrapper
     [:header.small-header.row
      [:div.home.col-md-3.col-sm-3.col-xs-6.col-lg-3
       (link-to "/" (image "/images/scalajars-logo-tiny.png" "Scalajars"))
       [:h1 (link-to "/" "Scalajars")]]
      [:div.col-md-3.col-sm-3.col-xs-6.col-lg-3
       [:form {:action "/search"}
        [:input {:type "search"
                 :name "q"
                 :id "search"
                 :class "search"
                 :placeholder "Search projects..."
                 :required true}]]]
      [:nav.main-navigation.col-md-6.col-sm-6.col-xs-12.col-lg-6
       (if account
         (unordered-list
          [(link-to "/" "dashboard")
           (link-to "/profile" "profile")
           (link-to "/logout" "logout")])
         (unordered-list
          [(link-to "/login" "login")
           (link-to "/register" "register")]))]]
     body
     footer]]))

(defn html-doc-with-large-header [account title & body]
  (html5
   [:head
    [:link {:type "application/opensearchdescription+xml"
            :href "/opensearch.xml"
            :rel "search"}]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
    [:title
     (when title
       (str title " - "))
     "Scalajars"]
    (map #(include-css (str "/stylesheets/" %))
         ;; Bootstrap was customized to only include the 'grid' styles
         ;; (then the default colors were removed)
         ;; more info: http://getbootstrap.com/css/#grid
         ["reset.css" "vendor/bootstrap/bootstrap.css" "screen.css"])
    (include-js "//use.typekit.net/zhw0tse.js")
    [:script "try{Typekit.load();}catch(e){}"]
    (raw (when-ie (include-js "/js/html5.js")))]
   [:body.container-fluid
    [:div.hero.row
     [:header
      [:div.home.col-md-6.col-sm-6.col-xs-12.col-lg-6
       (link-to "/" (image "/images/scalajars-logo-small.png" "Scalajars"))
       [:h1
        (link-to "/" "Scalajars")]]
      [:nav.main-navigation.col-md-6.col-sm-6.col-xs-12.col-lg-6
       (if account
         (unordered-list
          [(link-to "/" "dashboard")
           (link-to "/profile" "profile")
           (link-to "/logout" "logout")])
         (unordered-list
          [(link-to "/login" "login")
           (link-to "/register" "register")]))]
      [:h2.hero-text.row
       [:div.col-md-12
        [:span.heavy "Scalajars"]
        " is a "
        [:span.heavy "dead easy"]
        " community repository for "]
       [:div.col-md-12
        " open source Clojure libraries."]]]
     [:div.search-form-container.col-md-12.col-xs-12.col-lg-12.col-sm-12
      [:form {:action "/search"}
       [:input {:type "search"
                :name "q"
                :id "search"
                :placeholder "Search projects..."
                :required true}]
       [:input {:id "search-button"
                :value "Search"
                :type "submit"}]]]
     [:h2.getting-started.row
      [:div.col-md-12
       "To get started pushing your own project "
       (link-to "/register" "register")
       " and then"]
      [:div.col-md-12
       " check out the "
       (link-to "http://wiki.github.com/ato/scalajars-web/tutorial" "tutorial")
       ". Alternatively, "
       (link-to "/projects" "browse the repository")
       "."]]]
    body
    footer]))

(defn flash [msg]
  (if msg
    [:div#flash msg]))

(defn error-list [errors]
  (when errors
    [:div.error
     [:strong "Blistering barnacles!"]
     "  Something's not shipshape:"
     (unordered-list errors)]))

(defn tag [s]
  (raw (html [:span.tag s])))

(defn jar-url [jar]
  (if (= (:group_name jar) (:jar_name jar))
    (str "/" (:jar_name jar))
    (str "/" (:group_name jar) "/" (:jar_name jar))))

(defn jar-name [jar]
  (if (= (:group_name jar) (:jar_name jar))
    (:jar_name jar)
    (str (:group_name jar) "/" (:jar_name jar))))

(defn jar-fork? [jar]
  (re-find #"^org.scalajars." (or
                             (:group_name jar)
                             (:group-id jar)
                             "")))

(def single-fork-notice
  [:p.fork-notice.hint
   "Note: this artifact is a non-canonical fork. See "
   (link-to "https://github.com/ato/clojars-web/wiki/Groups" "clojars wiki")
   " for more details."])

(def collection-fork-notice
  [:p.fork-notice.hint
   "Note: artifacts in italics are non-canonical forks. See "
   (link-to "https://github.com/ato/clojars-web/wiki/Groups" "clojars wiki")
   " for more details."])

(defn jar-link [jar]
  (link-to {:class (when (jar-fork? jar) "fork")}
           (jar-url jar)
           (jar-name jar)))

(defn user-link [username]
  (link-to (str "/users/" username) username))

(defn group-link [groupname]
  (link-to (str "/groups/" groupname) groupname))

(defn format-date [s]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") s))

(defn simple-date [s]
  (.format (java.text.SimpleDateFormat. "MMM d, yyyy") s))

(defn page-nav [current-page total-pages]
  (let [previous-text (raw "&#8592; Previous")
        next-text (raw "Next &#8594")
        page-range 3
        page-url "/projects?page="
        current-page (-> current-page (max 1) (min total-pages))

        main-div [:div.page-nav]
        previous-page (if (= current-page 1)
                        [[:span.previous-page.disabled previous-text]]
                        [[:a.previous-page
                          {:href (str page-url (- current-page 1))}
                          previous-text]])
        before-current (map
                        #(link-to (str page-url %) %)
                        (drop-while
                         #(< % 1)
                         (range (- current-page page-range) current-page)))
        current [[:em.current (str current-page)]]
        after-current (map
                       #(link-to (str page-url %) %)
                       (take-while
                        #(<= % total-pages)
                        (range (inc current-page) (+ current-page 1 page-range))))
        next-page (if (= current-page total-pages)
                    [[:span.next-page.disabled next-text]]
                    [[:a.next-page
                      {:href (str page-url (+ current-page 1))}
                      next-text]])]
    (vec
      (concat main-div previous-page before-current current after-current next-page))))

(defn page-description [current-page per-page total]
  (let [total-pages (-> (/ total per-page) Math/ceil .intValue)
        current-page (-> current-page (max 1) (min total-pages))
        upper (* per-page current-page)]
   [:div {:class "page-description"}
     "Displaying projects "
     [:b (str (-> upper (- per-page) inc) " - " (min upper total))]
     " of "
     [:b total]]))
