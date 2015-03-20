(ns scalajars.test.integration.sessions
  (:require [clojure.test :refer :all]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [scalajars.test.integration.steps :refer :all]
            [scalajars.db :as db]
            [scalajars.web :as web]
            [scalajars.test.test-helper :as help]
            [net.cgrand.enlive-html :as enlive]
            [korma.core :as korma]))

(use-fixtures :each help/default-fixture)

(deftest user-cant-login-with-bad-user-pass-combo
  (-> (session web/scalajars-app)
      (login-as "fixture@example.org" "password")
      (follow-redirect)
      (has (status? 200))
      (within [:div :p.error]
              (has (text? "Incorrect username and/or password.")))))

(deftest user-can-login-and-logout
  (-> (session web/scalajars-app)
      (register-as "fixture" "fixture@example.org" "password" ""))
  (doseq [login ["fixture@example.org" "fixture"]]
    (-> (session web/scalajars-app)
        (login-as login "password")
        (follow-redirect)
        (has (status? 200))
        (within [:.light-article :> :h1]
                (has (text? "Dashboard (fixture)")))
        (follow "logout")
        (follow-redirect)
        (has (status? 200))
        (within [:nav [:li enlive/first-child] :a]
                (has (text? "login"))))))

(deftest user-with-password-wipe-gets-message
  (-> (session web/scalajars-app)
      (register-as "fixture" "fixture@example.org" "password" ""))
  (korma/update db/users
                (korma/set-fields {:password ""})
                (korma/where {:user "fixture"}))
  (-> (session web/scalajars-app)
      (login-as "fixture" "password")
      (follow-redirect)
      (has (status? 200))
      (within [:div :p.error]
              (has (text? "Incorrect username and/or password.")))))
