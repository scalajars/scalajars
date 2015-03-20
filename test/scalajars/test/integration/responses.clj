(ns scalajars.test.integration.responses
  (:require [clojure.test :refer :all]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [scalajars.test.integration.steps :refer :all]
            [scalajars.web :as web]
            [scalajars.test.test-helper :as help]
            [net.cgrand.enlive-html :as enlive]))

(use-fixtures :each help/default-fixture)

(deftest respond-404
  (-> (session web/scalajars-app)
      (visit "/nonexistant-route")
      (has (status? 404))
      (within [:title]
              (has (text? "Page not found - Scalajars")))))

(deftest respond-405-for-puts
  (-> (session web/scalajars-app)
      (visit "/nonexistant-route" :request-method :put)
      (has (status? 405))))
