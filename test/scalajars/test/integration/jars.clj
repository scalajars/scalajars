(ns scalajars.test.integration.jars
  (:require [clojure.test :refer :all]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [scalajars.test.integration.steps :refer :all]
            [scalajars.web :as web]
            [scalajars.test.test-helper :as help]
            [net.cgrand.enlive-html :as html]))

(use-fixtures :each help/default-fixture)

(deftest jars-can-be-viewed
  (-> (session web/scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (scp valid-ssh-key "test.jar" "test-0.0.1/test.pom")
  (scp valid-ssh-key "test.jar" "test-0.0.2/test.pom")
  (scp valid-ssh-key "test.jar" "test-0.0.3-SNAPSHOT/test.pom")
  (scp valid-ssh-key "test.jar" "test-0.0.3-SNAPSHOT/test.pom")
  (-> (session web/scalajars-app)
      (visit "/fake/test")
      (within [:div#jar-title :h1 :a]
              (has (text? "fake/test")))
      (within [[:.package-config-example html/first-of-type] :pre]
              (has (text? "[fake/test \"0.0.2\"]")))
      (within [:ul#versions]
              (has (text? "0.0.3-SNAPSHOT0.0.20.0.1")))))

(deftest jars-with-only-snapshots-can-be-viewed
  (-> (session web/scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (scp valid-ssh-key "test.jar" "test-0.0.3-SNAPSHOT/test.pom")
  (-> (session web/scalajars-app)
      (visit "/fake/test")
      (within [:div#jar-title :h1 :a]
              (has (text? "fake/test")))
      (within [[:.package-config-example html/first-of-type] :pre]
              (has (text? "[fake/test \"0.0.3-SNAPSHOT\"]")))
      (within [:span.commit-url]
              (has (text? " with this commit")))
      (within [:ul#versions]
              (has (text? "0.0.3-SNAPSHOT")))))

(deftest canonical-jars-can-be-viewed
  (-> (session web/scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (scp valid-ssh-key "fake.jar" "fake-0.0.1/fake.pom")
  (scp valid-ssh-key "fake.jar" "fake-0.0.2/fake.pom")
  (scp valid-ssh-key "fake.jar" "fake-0.0.3-SNAPSHOT/fake.pom")
  (-> (session web/scalajars-app)
      (visit "/fake")
      (within [:div#jar-title :h1 :a]
              (has (text? "fake")))
      (within [[:.package-config-example html/first-of-type] :pre]
              (has (text? "[fake \"0.0.2\"]")))
      (within [:ul#versions]
              (has (text? "0.0.3-SNAPSHOT0.0.20.0.1")))))

(deftest specific-versions-can-be-viewed
  (-> (session web/scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (scp valid-ssh-key "test.jar" "test-0.0.1/test.pom")
  (scp valid-ssh-key "test.jar" "test-0.0.2/test.pom")
  (scp valid-ssh-key "test.jar" "test-0.0.3-SNAPSHOT/test.pom")
  (-> (session web/scalajars-app)
      (visit "/fake/test")
      (follow "0.0.3-SNAPSHOT")
      (within [:div#jar-title :h1 :a]
              (has (text? "fake/test")))
      (within [[:.package-config-example html/first-of-type] :pre]
              (has (text? "[fake/test \"0.0.3-SNAPSHOT\"]")))
      (within [:ul#versions]
              (has (text? "0.0.3-SNAPSHOT0.0.20.0.1")))))

(deftest specific-canonical-versions-can-be-viewed
  (-> (session web/scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (scp valid-ssh-key "fake.jar" "fake-0.0.1/fake.pom")
  (scp valid-ssh-key "fake.jar" "fake-0.0.2/fake.pom")
  (scp valid-ssh-key "fake.jar" "fake-0.0.3-SNAPSHOT/fake.pom")
  (-> (session web/scalajars-app)
      (visit "/fake")
      (follow "0.0.1")
      (within [:div#jar-title :h1 :a]
              (has (text? "fake")))
      (within [[:.package-config-example html/first-of-type] :pre]
              (has (text? "[fake \"0.0.1\"]")))
      (within [:ul#versions]
              (has (text? "0.0.3-SNAPSHOT0.0.20.0.1")))))


(deftest canonical-jars-can-view-dependencies
  (-> (session web/scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (scp valid-ssh-key "fake.jar" "fake-0.0.1/fake.pom")
  (scp valid-ssh-key "fake.jar" "fake-0.0.2/fake.pom")
  (-> (session web/scalajars-app)
      (visit "/fake")
      (within [:ul#dependencies]
               (has (text? "org.clojure/clojure 1.3.0-beta1")))))
