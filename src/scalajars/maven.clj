(ns scalajars.maven
  (:require [clojure.java.io :as io]
            [scalajars.config :refer [config]]
            [clojure.string :refer [split]]
            [clj-stacktrace.repl :refer [pst]])
  (:import org.apache.maven.model.io.xpp3.MavenXpp3Reader
           org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
           java.io.IOException))

(defn model-to-map [model]
  {:name (or (.getArtifactId model)
             (-> model .getParent .getArtifactId))
   :group (or (.getGroupId model)
              (-> model .getParent .getGroupId))
   :version (or (.getVersion model)
                (-> model .getParent .getVersion))
   :description (.getDescription model)
   :homepage (.getUrl model)
   :url (.getUrl model)
   :licenses (.getLicenses model)
   :scm (.getScm model)
   :authors (vec (map #(.getName %) (.getContributors model)))
   :dependencies (vec (map
                       (fn [d] {:group_name (.getGroupId d)
                                :jar_name (.getArtifactId d)
                                :version (.getVersion d)
                                :scope (or (.getScope d) "compile")})
                       (.getDependencies model)))})

(defn read-pom
  "Reads a pom file returning a maven Model object."
  [file]
  (with-open [reader (io/reader file)]
    (.read (MavenXpp3Reader.) reader)))

(def pom-to-map (comp model-to-map read-pom))

(defn read-metadata
  "Reads a maven-metadata file returning a maven Metadata object."
  [file]
  (with-open [reader (io/reader file)]
    (.read (MetadataXpp3Reader.) reader)))

(defn snapshot-version
  "Get snapshot version from maven-metadata.xml used in pom filename"
  [file]
  (let [versioning (-> (read-metadata file) .getVersioning .getSnapshot)]
    (str (.getTimestamp versioning) "-" (.getBuildNumber versioning))))

(defn directory-for
  "Directory for a jar under repo"
  [{:keys [group_name jar_name version]}]
  (apply io/file (concat [(config :repo)] (split group_name #"\.") [jar_name version])))

(defn snapshot-pom-file [{:keys [jar_name version] :as jar}]
  (let [metadata-file (io/file (directory-for jar) "maven-metadata.xml")
        snapshot (snapshot-version metadata-file)
        filename (format "%s-%s-%s.pom" jar_name (re-find #"\S+(?=-SNAPSHOT$)" version) snapshot)]
    (io/file (directory-for jar) filename)))

(defn jar-to-pom-map [{:keys [jar_name version] :as jar}]
  (try
    (let [pom-file (if (re-find #"SNAPSHOT$" version)
                     (snapshot-pom-file jar)
                     (io/file (directory-for jar) (format "%s-%s.%s" jar_name version "pom")))]
      (pom-to-map (str pom-file)))
    (catch IOException e (pst e) nil)))

(defn github-info [pom-map]
  (let [scm (:scm pom-map)
        url (and scm (.getUrl scm))
        github-re #"^https?://github.com/([^/]+/[^/]+)"
        user-repo (->> (str url) (re-find github-re) second)]
    user-repo))

(defn commit-url [pom-map]
  (let [scm (:scm pom-map)
        url (and scm (.getUrl scm))
        base-url (re-find #"https?://github.com/[^/]+/[^/]+" (str url))]
    (if (and base-url (.getTag scm)) (str base-url "/commit/" (.getTag scm)))))
