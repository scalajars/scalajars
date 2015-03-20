(ns scalajars.test.unit.promote
  (:require [clojure.test :refer :all]
            [scalajars.promote :refer :all]
            [clojure.java.io :as io]
            [scalajars.maven :as maven]
            [scalajars.db :as db]
            [scalajars.config :refer [config]]
            [scalajars.test.test-helper :as help]))

(use-fixtures :each help/default-fixture)

(defn copy-resource [version & [extension]]
  (let [extension (or extension "pom")]
    (.mkdirs (.getParentFile (file-for "robert" "hooke" version "")))
    (io/copy (io/reader (io/resource (str "hooke-" version "." extension)))
             (file-for "robert" "hooke" version extension))))

(deftest test-snapshot-blockers
  (is (= ["Snapshot versions cannot be promoted"
          "Missing file hooke-1.2.0-SNAPSHOT.jar"
          "Missing file hooke-1.2.0-SNAPSHOT.pom"]
         (take 3 (blockers {:group "robert" :name "hooke"
                            :version "1.2.0-SNAPSHOT"})))))

(deftest test-metadata-blockers
  (copy-resource "1.1.1")
  (is (clojure.set/subset? #{"Missing url" "Missing description"}
                           (set (blockers {:group "robert" :name "hooke"
                                           :version "1.1.1"})))))

(deftest test-unsigned
  (copy-resource "1.1.2")
  (let [b (blockers {:group "robert" :name "hooke" :version "1.1.2"})]
    (is (some #(.endsWith % "hooke-1.1.2.pom is not signed.") b))
    (is (some #(.endsWith % "hooke-1.1.2.jar is not signed.") b))
    (is (some #(= % "Missing file hooke-1.1.2.jar") b))))

(deftest test-success
  (copy-resource "1.1.2")
  (io/copy "dummy hooke jar file"
           (file-for "robert" "hooke" "1.1.2" "jar"))
  (copy-resource "1.1.2" "jar.asc")
  (copy-resource "1.1.2" "pom.asc")
  (db/add-user "test@ex.com" "testuser" "password" "asdf"
               (slurp (io/resource "pubring.gpg")))
  (db/add-member "robert" "testuser" nil)
  (is (empty? (blockers {:group "robert" :name "hooke" :version "1.1.2"}))))

(deftest test-failed-signature
  (copy-resource "1.1.2")
  (io/copy "dummy hooke jar file corrupted"
           (file-for "robert" "hooke" "1.1.2" "jar"))
  (copy-resource "1.1.2" "jar.asc")
  (copy-resource "1.1.2" "pom.asc")
  (db/add-user "test@ex.com" "testuser" "password" "asdf"
               (slurp (io/resource "pubring.gpg")))
  (db/add-member "robert" "testuser" nil)
  (is (= [(str "Could not verify signature of "
               (config :repo) "/robert/hooke/1.1.2/hooke-1.1.2.jar. "
               "Ensure your public key is in your profile.")]
         (blockers {:group "robert" :name "hooke" :version "1.1.2"}))))

(deftest test-no-key
  (copy-resource "1.1.2")
  (io/copy "dummy hooke jar file corrupted"
           (file-for "robert" "hooke" "1.1.2" "jar"))
  (copy-resource "1.1.2" "jar.asc")
  (copy-resource "1.1.2" "pom.asc")
  (db/add-user "test@ex.com" "testuser" "password" "asdf"
               "")
  (db/add-member "robert" "testuser" nil)
  (is (= [(str "Could not verify signature of "
               (config :repo) "/robert/hooke/1.1.2/hooke-1.1.2.jar. "
               "Ensure your public key is in your profile.")
          (str "Could not verify signature of "
               (config :repo) "/robert/hooke/1.1.2/hooke-1.1.2.pom. "
               "Ensure your public key is in your profile.")]
         (blockers {:group "robert" :name "hooke" :version "1.1.2"}))))
