(ns scalajars.test.integration.uploads
  (:require [clojure.test :refer :all]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [scalajars.config :refer [config]]
            [scalajars.web :refer [scalajars-app]]
            [scalajars.test.integration.steps :refer :all]
            [scalajars.test.test-helper :as help]
            [clojure.java.io :as io]
            [cemerick.pomegranate.aether :as aether]
            [ring.adapter.jetty :as jetty]
            [net.cgrand.enlive-html :as enlive]
            [clojure.data.codec.base64 :as base64]))

(declare test-port)

(defn- run-test-app
  [f]
  (let [server (jetty/run-jetty
                #(binding [*out* (java.io.StringWriter.)]
                   (#'scalajars-app %))
                {:port 0 :join? false})
        port (-> server .getConnectors first .getLocalPort)]
    (with-redefs [test-port port]
      (try
        (f)
        (finally
          (.stop server))))))

(use-fixtures :once run-test-app)
(use-fixtures :each help/default-fixture help/index-fixture)

(deftest user-can-register-and-deploy
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" ""))
  (help/delete-file-recursively help/local-repo)
  (help/delete-file-recursively help/local-repo2)
  (aether/deploy
   :coordinates '[org.scalajars.dantheman/test "1.0.0"]
   :jar-file (io/file (io/resource "test.jar"))
   :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
   :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                        :username "dantheman"
                        :password "password"}}
   :local-repo help/local-repo)
  (is (= 6
         (count (.list (clojure.java.io/file (:repo config)
                                             "org"
                                             "scalajars"
                                             "dantheman"
                                             "test"
                                             "1.0.0")))))
  (is (= '{[org.scalajars.dantheman/test "1.0.0"] nil}
         (aether/resolve-dependencies
          :coordinates '[[org.scalajars.dantheman/test "1.0.0"]]
          :repositories {"test" {:url
                                 (str "http://localhost:" test-port "/repo")}}
          :local-repo help/local-repo2)))
  (-> (session scalajars-app)
      (visit "/groups/org.scalajars.dantheman")
      (has (status? 200))
      (within [:#content-wrapper
               [:ul enlive/last-of-type]
               [:li enlive/only-child]
               :a]
              (has (text? "dantheman")))
      (follow "org.scalajars.dantheman/test")
      (has (status? 200))
      (within [:#jar-sidebar :li.homepage :a]
              (has (text? "https://example.org")))))

(deftest user-can-deploy-to-new-group
   (-> (session scalajars-app)
       (register-as "dantheman" "test@example.org" "password" ""))
   (help/delete-file-recursively help/local-repo)
   (help/delete-file-recursively help/local-repo2)
   (aether/deploy
    :coordinates '[fake/test "0.0.1"]
    :jar-file (io/file (io/resource "test.jar"))
    :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
    :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                         :username "dantheman"
                         :password "password"}}
    :local-repo help/local-repo)
   (is (= '{[fake/test "0.0.1"] nil}
          (aether/resolve-dependencies
           :coordinates '[[fake/test "0.0.1"]]
           :repositories {"test" {:url
                                  (str "http://localhost:" test-port "/repo")}}
           :local-repo help/local-repo2)))
   (-> (session scalajars-app)
       (visit "/groups/fake")
       (has (status? 200))
       (within [:#content-wrapper
                [:ul enlive/last-of-type]
                [:li enlive/only-child]
                :a]
               (has (text? "dantheman")))
       (follow "fake/test")
       (has (status? 200))
       (within [:#jar-sidebar :li.homepage :a]
               (has (text? "https://example.org")))))

(deftest user-cannot-deploy-to-groups-without-permission
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (-> (session scalajars-app)
      (register-as "fixture" "fixture@example.org" "password" valid-ssh-key))
  (is (thrown-with-msg? org.sonatype.aether.deployment.DeploymentException
        #"Forbidden"
        (aether/deploy
         :coordinates '[org.scalajars.fixture/test "1.0.0"]
         :jar-file (io/file (io/resource "test.jar"))
         :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
         :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                              :username "dantheman"
                              :password "password"}}
         :local-repo help/local-repo))))

(deftest user-cannot-redeploy
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (aether/deploy
   :coordinates '[org.scalajars.dantheman/test "0.0.1"]
   :jar-file (io/file (io/resource "test.jar"))
   :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
   :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                        :username "dantheman"
                        :password "password"}}
   :local-repo help/local-repo)
  (is (thrown-with-msg?
       org.sonatype.aether.deployment.DeploymentException
       #"Unauthorized"
       (aether/deploy
        :coordinates '[org.scalajars.fixture/test "0.0.1"]
        :jar-file (io/file (io/resource "test.jar"))
        :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
        :repository {"test" {:url (str "http://localhost:" test-port "/repo")}}
        :local-repo help/local-repo))))

(deftest user-can-redeploy-snapshots
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" valid-ssh-key))
  (aether/deploy
   :coordinates '[org.scalajars.dantheman/test "0.0.3-SNAPSHOT"]
   :jar-file (io/file (io/resource "test.jar"))
   :pom-file (io/file (io/resource "test-0.0.3-SNAPSHOT/test.pom"))
   :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                        :username "dantheman"
                        :password "password"}}
   :local-repo help/local-repo)
  (aether/deploy
   :coordinates '[org.scalajars.dantheman/test "0.0.3-SNAPSHOT"]
   :jar-file (io/file (io/resource "test.jar"))
   :pom-file (io/file (io/resource "test-0.0.3-SNAPSHOT/test.pom"))
   :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                        :username "dantheman"
                        :password "password"}}
   :local-repo help/local-repo))

(deftest anonymous-cannot-deploy
  (is (thrown-with-msg? org.sonatype.aether.deployment.DeploymentException
        #"Unauthorized"
        (aether/deploy
         :coordinates '[fake/test "1.0.0"]
         :jar-file (io/file (io/resource "test.jar"))
         :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
         :repository {"test" {:url (str "http://localhost:" test-port "/repo")}}
         :local-repo help/local-repo))))

(deftest bad-login-cannot-deploy
  (is (thrown? org.sonatype.aether.deployment.DeploymentException
        (aether/deploy
         :coordinates '[fake/test "1.0.0"]
         :jar-file (io/file (io/resource "test.jar"))
         :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
         :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                              :username "guest"
                              :password "password"}}
         :local-repo help/local-repo))))

(deftest deploy-requires-lowercase
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" ""))
  (is (thrown-with-msg? org.sonatype.aether.deployment.DeploymentException
        #"Forbidden"
        (aether/deploy
         :coordinates '[faKE/test "1.0.0"]
         :jar-file (io/file (io/resource "test.jar"))
         :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
         :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                              :username "dantheman"
                              :password "password"}}
         :local-repo help/local-repo))))

(deftest deploy-requires-ascii-version
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" ""))
  (is (thrown-with-msg? org.sonatype.aether.deployment.DeploymentException
        #"Forbidden"
        (aether/deploy
         :coordinates '[fake/test "1.α.0"]
         :jar-file (io/file (io/resource "test.jar"))
         :pom-file (io/file (io/resource "test-0.0.1/test.pom"))
         :repository {"test" {:url (str "http://localhost:" test-port "/repo")
                              :username "dantheman"
                              :password "password"}}
         :local-repo help/local-repo))))

(deftest put-on-html-fails
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" "")
      (visit "/repo/group/artifact/1.0.0/injection.html"
             :request-method :put
             :headers {"authorization"
                       (str "Basic "
                            (String. (base64/encode
                                      (.getBytes "dantheman:password"
                                                 "UTF-8"))
                                     "UTF-8"))}
             :body "XSS here")
      (has (status? 400))))

(deftest put-using-dotdot-fails
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" "")
      (visit "/repo/../artifact/1.0.0/test.jar" :request-method :put
             :headers {"authorization"
                       (str "Basic "
                            (String. (base64/encode
                                      (.getBytes "dantheman:password"
                                                 "UTF-8"))
                                     "UTF-8"))}
             :body "Bad jar")
      (has (status? 400))
      (visit "/repo/group/..%2F..%2Fasdf/1.0.0/test.jar" :request-method :put
             :headers {"authorization"
                       (str "Basic "
                            (String. (base64/encode
                                      (.getBytes "dantheman:password"
                                                 "UTF-8"))
                                     "UTF-8"))}
             :body "Bad jar")
      (has (status? 400))
      (visit "/repo/group/artifact/..%2F..%2F..%2F1.0.0/test.jar"
             :request-method :put
             :headers {"authorization"
                       (str "Basic "
                            (String. (base64/encode
                                      (.getBytes "dantheman:password"
                                                 "UTF-8"))
                                     "UTF-8"))}
             :body "Bad jar")
      (has (status? 400))
      (visit "/repo/group/artifact/1.0.0/..%2F..%2F..%2F..%2F/test.jar"
             :request-method :put
             :headers {"authorization"
                       (str "Basic "
                            (String. (base64/encode
                                      (.getBytes "dantheman:password"
                                                 "UTF-8"))
                                     "UTF-8"))}
             :body "Bad jar")
      (has (status? 400))))

(deftest does-not-write-incomplete-file
  (-> (session scalajars-app)
      (register-as "dantheman" "test@example.org" "password" ""))
  (with-out-str
    (-> (session scalajars-app)
        (visit "/repo/group3/artifact3/1.0.0/test.jar"
               :body (proxy [java.io.InputStream] []
                       (read
                         ([_] (throw (java.io.IOException.)))))
               :request-method :put
               :content-length 1000
               :content-type "txt/plain"
               :headers {:content-length 1000
                         :content-type "txt/plain"
                         :authorization (str "Basic "
                                             (String. (base64/encode
                                                       (.getBytes "dantheman:password"
                                                                  "UTF-8"))
                                                      "UTF-8"))})
        (has (status? 403))))
  (is (not (.exists (clojure.java.io/file (:repo config)
                                          "group3"
                                          "artifact3"
                                          "1.0.0"
                                          "test.jar")))))
