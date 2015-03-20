(ns scalajars.stats.process "generate usage statistics from web log"
    (:require [clojure.java.io :as io]
              [clojure.string :as str]
              [net.cgrand.regex :as re]
              [clj-time.format :as timef]))

(def time-clf (timef/formatter "dd/MMM/YYYY:HH:mm:ss Z"))

;; net.cgrand/regex currently doesn't allow Patterns
;; but they're too handy so let's enable them anyway
(extend-type java.util.regex.Pattern
  re/RegexValue
  (pattern [re] (.pattern re))
  (groupnames [re] [])
  re/RegexFragment
  (static? [this _] true))

(def re-clf ; common log format (apache, nginx etc)
  (let [field #"\S+"
        nonbracket #"[^\]]+"
        nonquote #"[^\" ]+"
        reqline (list [nonquote :as :method] \space
                      [nonquote :as :path] \space
                      [nonquote :as :protocol])]
    (re/regex [field :as :host] \space
              [field :as :ident] \space
              [field :as :authuser] \space
              \[ [nonbracket :as :time] \] \space
              \" reqline \" \space
              [field :as :status] \space
              [field :as :size]
              #".*")))

(def re-path
  (let [segment #"[^/]+"
        sep #"/+"]
    (re/regex sep "repo" sep
              [(re/* segment sep) segment :as :group] sep
              [segment :as :name] sep
              [segment :as :version] sep
              segment \.
              [#"\w+" :as :ext])))

(defn parse-path [s]
  (when s
    (when-let [m (re/exec re-path s)]
      {:name (:name m)
       :group (str/replace (:group m) "/" ".")
       :version (:version m)
       :ext (:ext m)})))

(defn parse-long [s]
  (when-not (#{nil "" "-"} s)
    (try (Long/parseLong s)
         (catch NumberFormatException e))))

(defn parse-clf [line]
  (let [m (re/exec re-clf line)]
    (merge
     (parse-path (:path m))
     {:status (parse-long (:status m))
      :method (:method m)
      :size (parse-long (:size m))
      :time (when (:time m) (try (timef/parse time-clf (:time m))
                                 (catch IllegalArgumentException e)))})))

(defn valid-download? [m]
  (and m
       (= (:status m) 200)
       (= (:method m) "GET")
       (= (:ext m) "jar")))

(def as-year-month (partial timef/unparse (timef/formatters :year-month)))

(defn compute-stats [lines]
  (->> lines
       (map parse-clf)
       (filter valid-download?)
       (map (juxt :group :name :version))
       (frequencies)
       (reduce-kv (fn [acc [a g v] n] (assoc-in acc [[a g] v] n)) {})))

(defn process-log [logfile]
  (with-open [rdr (io/reader logfile)]
    (compute-stats (line-seq rdr))))

(defn -main [& args]
  (-> *in*
        java.io.BufferedReader.
        line-seq
        compute-stats
        prn))
